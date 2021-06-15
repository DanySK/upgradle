package org.danilopianini.upgradle

import org.danilopianini.upgradle.api.Change
import org.danilopianini.upgradle.api.Credentials
import org.danilopianini.upgradle.api.Credentials.Companion.authenticated
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.Pattern
import org.danilopianini.upgradle.config.CommitAuthor
import org.danilopianini.upgradle.remote.Branch
import org.danilopianini.upgradle.remote.Repository
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.Label
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.PullRequestMarker
import org.eclipse.egit.github.core.SearchRepository
import org.eclipse.egit.github.core.service.LabelService
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import java.io.File

fun Repository.clone(branch: Branch, destination: File, credentials: Credentials): Git =
    Git.cloneRepository()
        .authenticated(credentials)
        .setURI(cloneUri)
        .setBranch(branch.name)
        .setDirectory(destination)
        .call()

fun Git.add(location: File, changes: Iterable<Change>) = add().apply {
    changes.forEach {
        // Add changes to the tracker
        addFilepattern(
            when (it) {
                is OnFile -> it.file.relativeTo(location).path
                is Pattern -> it.pattern.replace(location.absolutePath, "")
            }
        )
    }
}.call()

fun Git.commit(message: String, author: CommitAuthor) =
    commit().setMessage(message).setAuthor(PersonIdent(author.name, author.email)).call()

fun Git.pushTo(branch: String, credentials: Credentials): List<RemoteRefUpdate> = push()
    .authenticated(credentials)
    .setForce(false)
    .setRemote("origin")
    .setRefSpecs(RefSpec(branch))
    .let {
        runCatching { it.call() }.getOrElse {
            UpGradle.logger.warn("Push failed: is the project archived?", it)
            emptyList()
        }
    }
    .flatMap { it.remoteUpdates }

fun Repository.createPullRequest(update: Operation, head: String, base: String, credentials: Credentials) =
    PullRequestService()
        .authenticated(credentials)
        .createPullRequest(
            asIdProvider(),
            PullRequest()
                .setBase(PullRequestMarker().setRef(base).setLabel(base))
                .setHead(PullRequestMarker().setRef(head).setLabel(head))
                .setTitle(update.pullRequestTitle)
                .setBody(update.pullRequestMessage)
                .setBodyText(update.pullRequestMessage)
        )

fun Repository.applyLabels(labels: Collection<Label>, pr: PullRequest, credentials: Credentials) {
    if (labels.isNotEmpty()) {
        val labelService = LabelService().authenticated(credentials)
        val availableLabels = labelService.getLabels(asIdProvider())
        val actualLabels = labels.map { desiredLabel ->
            availableLabels.find { desiredLabel.name == it.name }
                ?: labelService.createLabel(asIdProvider(), desiredLabel).also {
                    UpGradle.logger.info("Created new label $desiredLabel")
                }
        }
        labelService.client.post<List<Label>>(
            "/repos/$owner/$name/issues/${pr.number}/labels",
            mapOf("labels" to actualLabels.map { it.name }),
            List::class.java
        )
    }
}

private fun Repository.asIdProvider(): IRepositoryIdProvider =
    if (this is IRepositoryIdProvider) this else SearchRepository(owner, name)
