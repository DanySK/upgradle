#!/bin/bash
set -e
./gradlew shadow
./gradlew githubRelease
./gradlew publish || ./gradlew publish || ./gradlew publish || ./gradlew publish || ./gradlew publish 
