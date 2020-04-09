package org.danilopianini.upgradle

import org.danilopianini.upgradle.api.Change
import org.danilopianini.upgradle.api.Credentials
import org.danilopianini.upgradle.api.Credentials.Companion.authenticated
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.Pattern
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.PullRequestMarker
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryBranch
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import java.io.File

fun Repository.clone(branch: RepositoryBranch, destination: File, credentials: Credentials): Git = Git
        .cloneRepository()
        .authenticated(credentials)
        .setURI(htmlUrl)
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

fun Git.commit(message: String, author: String = "UpGradle [Bot]", email: String = "danilo.pianini@gmail.com") =
    commit().setMessage(message).setAuthor(PersonIdent(author, email)).call()

fun Git.pushTo(branch: String, credentials: Credentials): List<RemoteRefUpdate> = push()
    .authenticated(credentials)
    .setForce(false)
    .setRemote("origin")
    .setRefSpecs(RefSpec(branch))
    .let { runCatching { it.call() }.getOrElse {
        UpGradle.logger.warn("Push failed: is the project archived?", it)
        emptyList()
    } }
    .flatMap { it.remoteUpdates }

fun Repository.createPullRequest(update: Operation, head: String, base: String, credentials: Credentials) =
    PullRequestService()
        .authenticated(credentials)
            .createPullRequest(this,
                PullRequest()
                    .setBase(PullRequestMarker().setRef(base).setLabel(base))
                    .setHead(PullRequestMarker().setRef(head).setLabel(head))
                    .setTitle(update.pullRequestTitle)
                    .setBody(update.pullRequestMessage)
                    .setBodyText(update.pullRequestMessage)
            )
