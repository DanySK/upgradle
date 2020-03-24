# UpGradle

![GitHub](https://img.shields.io/github/license/DanySK/upgradle)
![Travis (.com)](https://img.shields.io/travis/com/DanySK/upgradle)
![Codacy coverage](https://img.shields.io/codacy/coverage/75076bfcac4a4360851b2b55824280f0)
![Code Climate coverage](https://img.shields.io/codeclimate/coverage/DanySK/upgradle)
![Codecov](https://img.shields.io/codecov/c/github/DanySK/upgradle)
![CII Best Practices Summary](https://img.shields.io/cii/summary/3803)
![Codacy grade](https://img.shields.io/codacy/grade/75076bfcac4a4360851b2b55824280f0)
![Code Climate coverage](https://img.shields.io/codeclimate/coverage/DanySK/upgradle)
![Code Climate maintainability](https://img.shields.io/codeclimate/maintainability-percentage/DanySK/upgradle)
![Code Climate maintainability](https://img.shields.io/codeclimate/issues/DanySK/upgradle)
![Code Climate maintainability](https://img.shields.io/codeclimate/tech-debt/DanySK/upgradle)
[![CodeFactor](https://www.codefactor.io/repository/github/danysk/upgradle/badge)](https://www.codefactor.io/repository/github/danysk/upgradle)
![GitHub language count](https://img.shields.io/github/languages/count/DanySK/upgradle)
![GitHub top language](https://img.shields.io/github/languages/top/DanySK/upgradle)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=alert_status)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=bugs)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=code_smells)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=coverage)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=ncloc)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=security_rating)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=sqale_index)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=DanySK_upgradle&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=DanySK_upgradle)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/DanySK/upgradle)
![GitHub repo size](https://img.shields.io/github/repo-size/DanySK/upgradle)
![GitHub issues](https://img.shields.io/github/issues/DanySK/upgradle)
![GitHub closed issues](https://img.shields.io/github/issues-closed/DanySK/upgradle)
![GitHub pull requests](https://img.shields.io/github/issues-pr/DanySK/upgradle)
![GitHub closed pull requests](https://img.shields.io/github/issues-pr-closed/DanySK/upgradle)
![Maven Central](https://img.shields.io/maven-central/v/org.danilopianini/upgradle)
![GitHub commit activity](https://img.shields.io/github/commit-activity/y/DanySK/upgradle)
![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/DanySK/upgradle/latest/master)
![GitHub contributors](https://img.shields.io/github/contributors/DanySK/upgradle)
![GitHub last commit](https://img.shields.io/github/last-commit/DanySK/upgradle)

A bot for one-shot maintenance of multiple GitHub projects,
focused on Gradle.

## Use

This project is very much in beta, details will come soon.

```yaml
includes:
  - owners: DanySK
    repos: travis.*
    branches:
      - master
excludes:
  owners: Protelis
  repos: Protelis
  branches: master
modules:
  - GradleWrapper
```