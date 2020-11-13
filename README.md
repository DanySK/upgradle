# UpGradle

A bot for one-shot maintenance of multiple GitHub projects, focused on Gradle.

## Badges

### Info
[![Build Status](https://github.com/DanySK/upgradle/workflows/CI/badge.svg)](https://github.com/DanySK/upgradle/actions?query=workflow%3ACI)
![GitHub](https://img.shields.io/github/license/DanySK/upgradle)
[![Javadocs](https://www.javadoc.io/badge/org.danilopianini/upgradle.svg)](https://www.javadoc.io/doc/org.danilopianini/upgradle)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/3803/badge)](https://bestpractices.coreinfrastructure.org/projects/3803)
![GitHub language count](https://img.shields.io/github/languages/count/DanySK/upgradle)
![GitHub top language](https://img.shields.io/github/languages/top/DanySK/upgradle)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=ncloc)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/DanySK/upgradle)
![GitHub repo size](https://img.shields.io/github/repo-size/DanySK/upgradle)
![Maven Central](https://img.shields.io/maven-central/v/org.danilopianini/upgradle)
![GitHub contributors](https://img.shields.io/github/contributors/DanySK/upgradle)

### Coverage

### Quality
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
![Codacy grade](https://img.shields.io/codacy/grade/75076bfcac4a4360851b2b55824280f0)
![Codecov](https://img.shields.io/codecov/c/github/DanySK/upgradle)
![Code Climate maintainability](https://img.shields.io/codeclimate/maintainability-percentage/DanySK/upgradle)
![Code Climate maintainability](https://img.shields.io/codeclimate/issues/DanySK/upgradle)
![Code Climate maintainability](https://img.shields.io/codeclimate/tech-debt/DanySK/upgradle)
[![CodeFactor](https://www.codefactor.io/repository/github/danysk/upgradle/badge)](https://www.codefactor.io/repository/github/danysk/upgradle)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=alert_status)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=bugs)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=code_smells)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=security_rating)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=sqale_index)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)

### Progress
![GitHub issues](https://img.shields.io/github/issues/DanySK/upgradle)
![GitHub closed issues](https://img.shields.io/github/issues-closed/DanySK/upgradle)
![GitHub pull requests](https://img.shields.io/github/issues-pr/DanySK/upgradle)
![GitHub closed pull requests](https://img.shields.io/github/issues-pr-closed/DanySK/upgradle)
![GitHub commit activity](https://img.shields.io/github/commit-activity/y/DanySK/upgradle)
![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/DanySK/upgradle/latest/master)
![GitHub last commit](https://img.shields.io/github/last-commit/DanySK/upgradle)



## Idea

UpGradle checks for updates to your project, applies the update, and opens a pull request for you.
If you got a CI in place, then the update is tested, and your maintenance time drops down, as you need only to approve
things when they work as expected, or fix an issue that would be going to hit.


### Concepts

UpGradle is structured around a very simple model.
You provide a way to identify yourself on GitHub,
it fetches which repositories you got access,
then filters those you want to upgrade,
clones them, checks out the appropriate branch in a temporary folder,
runs all the configured `Module`s (more words shortly),
then pushes a new branch with the change and prepares a pull request.

A `Module` is a small piece of software that, starting from a clean checked out branch,
can identify a list of possible update `Operation`s to apply, and how to apply them.

### Use

0. Download [the latest version of the tool](https://github.com/DanySK/upgradle/releases/latest), select the `upgradle-<version>-all.jar` artifact
0. You need Java 8 or later installed on your system
0. Generate a GitHub access token
0. Run `GITHUB_TOKEN=<token> java -jar upgradle-<version>-all.jar path_to_configuration_file`.
*Note*: if `path_to_configuration_file` is omitted, the default configuration will be used

## Configuration

This program expects a configuration in form of a YAML, JSON, or TOML file.
In the remainder, the former will be used.
The documentation refers to the latest stable version.

### Available options

The structure is quite simple, and expects the following keys:

* `includes`: which repositories should be considered
* (optional) `excludes`: matching repositories will not be considered. The selection is operated over those selected by `includes`
* `modules`: the list of modules to execute
* (optional) `labels`: the list of labels to apply to the pull requests
* (optional) `author`: Specifies the author to be used for commit messages

### Inclusion and exclusion of repositories

`includes` and `excludes` share the same syntax.
There can be a single or multiple descriptors,
each one  with the following information:

* `owners`: matching users' repositories will be considered
* `repos`: matching repository names will be considered
* `branches`: matching branch names will be considered
* `topics`: matching [topics](https://github.com/topics) will be considered

Every key can be a string or a list of strings,
that will get interpreted as regular expressions.

### Module selection

`modules` expects a list of modules entry.
Each entry must contain:
* `name`: the name of the module
(its implementing class, namely, a subclass of `org.danilopianini.upgradle.Module` available in the classpath).
* (optional) `options`: an object containing the options for the module

#### Available module: GradleWrapper

The `GradleWrapper` module fetches updates for the gradle wrapper.
To be considered, the project must be a valid gradle project with a wrapper.

##### Options
* `versionRegex`, *defaults to `.*`*: only versions matching the regex will be considered.
For instance, only version 6.x of Gradle should be considered, `versionRegex: '6(\.\d+)*'` could be specified.
* `strategy`, *defaults to `latest`*: filters versions for which an update pull request will be created.
Possible values are `latest` (only consider the latest version),
`next` (only consider the next incremental version),
and `all` (consider all versions).
Multiple strategies can be combined using a list, so e.g.,
if `[next, latest]` is specified and there are at least two different versions that are valid candidates for an update,
two pull requests will be generated.
Strategies are applied *after* `versionRegex`, so it is quite easy to build strategies such as
"filter out non-stable versions, then propose both an incremental and and an update to the latest version."

#### Available module: RefreshVersions

The `GradleWrapper` module fetches updates for Gradle projects using refreshVersions.
For the updates to be proposed, a valid `versions.properties` file should be present in the repository.

##### Options
* `versionRegex`, *defaults to `.*`*. Only versions matching the regex will be considered.
For instance, say that you expect only stable versions to be considered, filtering out release candidates,
alphas, betas, etc. A valid version in the example is a sequence of digits split by dots, which might begin with `v`.
In this case, the user can specify `versionRegex: 'v?\d+(\.\d+)*'`
* `strategy`, *defaults to `latest`*. Filters versions for which an update pull request will be created.
Possible values are `latest` (only consider the latest version),
`next` (only consider the next incremental version),
and `all` (consider all versions).
Multiple strategies can be combined using a list, so e.g.,
if `[next, latest]` is specified and there are at least two different versions that are valid candidates for an update,
two pull requests will be generated.
Strategies are applied *after* `versionRegex`, so it is quite easy to build strategies such as
"filter out non-stable versions, then propose both an incremental and and an update to the latest version."
* `timeoutInMinutes` *defaults to `5`.
The maximum time Upgradle will wait for its Gradle child process to complete the execution of the refreshVersion task
before inspecting the result.
In normal conditions, the process will terminate (successfully or not) well before five minutes,
so the timer is not very important.
However, it can be very useful to workaround [this Gradle bug](https://github.com/gradle/gradle/issues/14347),
causing the Gradle update to freeze.

#### Available module: TravisDist

The `TravisDist` module updates the `dist` entry on `.travis.yml`.
For it to work, the file must exist, be a valid YAML, and `dist` must be explicitly specified.

##### Options
* `versionRegex`, *defaults to `.*`*: only versions matching the regex will be considered.
* `strategy`, *defaults to `latest`*: filters versions for which an update pull request will be created.
Possible values are `latest` (only consider the latest version),
`next` (only consider the next incremental version),
and `all` (consider all versions).
Multiple strategies can be combined using a list, so e.g.,
if `[next, latest]` is specified and there are at least two different versions that are valid candidates for an update,
two pull requests will be generated.
Strategies are applied *after* `versionRegex`, so it is quite easy to build strategies such as
"filter out non-stable versions, then propose both an incremental and and an update to the latest version."

### Labeling the pull requests

`labels` allows for configuring labels with which the pull requests opened by UpGradle will be labeled.
This is especially useful for further automated workflows,
e.g. using [automerge](https://github.com/pascalgn/automerge-action).
The content of `labels` is a single or multiple descriptor expecting:

* `name`: a string with the name of the label
* *optional* `color` a color hexadecimal string, six characters long. If omitted, a random color will be generated.

In case the desired label is not available in the repository for which the PR is being prepared,
a new label with the desired name and color will be added.

### Specifying the commit author

`author` is used to determine which name and email address is to be associated for the generated commits and directly relate to git config. It's advised to at least use a real email address.

* `name` - Relates to `git config user.name`, defaults to `UpGradle [Bot]`
* `email` - Relates to `git config user.email`, defaults to `<>`

N.B. The author of the pull-request only depends on the owner of the `GITHUB_TOKEN`

### Working example

A working example is provided in [this repository](https://github.com/DanySK/upgradle-bot),
which relies on a Travis CI instance to perform the work.

A possible simple configuration is:
```yaml
includes:
  - owners: .*
    repos: .*
    branches:
      - master
modules:
  - name: GradleWrapper
    options:
      # Strategies "all", "next" "latest"
      strategy: next
      versionRegex: \d+(\.\d+)*
  - name: RefreshVersions
    options:
      # Strategies "all", "next" "latest"
      strategy:
        - next
        - latest
      versionRegex: v?\d+(\.\d+)*

```

### Default configuration

[The default configuration](https://github.com/DanySK/upgradle/blob/master/src/main/resources/upgradle.yml)
is aggressive and will run UpGradle on all the repositories you have write access to,
running all the shipped modules.

You probably don't want it

### Providing GitHub credentials

Credentials must be provided as environment variables.
UpGradle supports both user and password or token authentication.
The latter is to be preferred;
GitHub is planning to drop support for user/password authentication in future.

Variables must be provided prefixed by `GITHUB_`, `GH_`, or `RELEASES_`;
and followed by `TOKEN` for the token,
`USERNAME` or `USER` for the username,
or `PASSWORD` for the password.
In case both token-based and user/password are provided, token access is preferred.

A valid environment could be, for instance:

* `GITHUB_TOKEN=1a2b3c4d5e6f7g8h9i0j`
* `GITHUB_USERNAME=DanySK` and `GITHUB_PASSWORD=secret`
* `GH_TOKEN=1a2b3c4d5e6f7g8h9i0j`
* `RELEASES_USERNAME=DanySK` and `RELEASES_PASSWORD=secret`

Token should have `public_repo` access if you only plan to use UpGradle for open source,
or `repo` access if you intend to use it also on private repositories.

## Contributing to the project

### Reporting issues

Issues can be opened in the GitHub issue tracker of this project.

### Proposing changes

Pull requests are welcome.
Continuous integration is in place for testing and checking code quality.
Every new feature or module *must* come with adequate testing (even though I did not test my own modules as I should have had :( ).

### Developing a new module

Simply implement the `org.danilopianini.upgradle.Module` interface,
which is little more than a function `(File)->List<Operation>`
from the folder where the project is checked out
to the list of `Operation`s that should get performed.

A `SimpleOperation` class is provided that can get built by passing
a name for the destination branch, a commit message, a title and a body for the pull request,
and a function `() -> List<Change>` actually performing the job.

A `Change` simply defines what gets changed, can be either a single file (`OnFile`)
or a path to a file (`Pattern`).

