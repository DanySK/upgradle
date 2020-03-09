# UpGradle

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