# SimCapture

DHIS2 data and tracker capture app for Android to support integration with SimprintsID.

Summary of changes comparing to upstream [dhis2/dhis2-android-capture-app](https://github.com/dhis2/dhis2-android-capture-app):

| Feature                                 | File / line of code                                                                                    | Type of change | Description                                                                                   |
|-----------------------------------------|--------------------------------------------------------------------------------------------------------|----------------|-----------------------------------------------------------------------------------------------|
| Infra: fork sync from upstream          | [simcapture-sync-fork-upstream-main.yml](.github/workflows/simcapture-sync-fork-upstream-main.yml)     | New file       | GitHub Action to sync upstream's `main` to this repo's `upstream-main` nightly on Mon-Fri     |
| Infra: automatic PR on upstream release | [simcapture-upstream-release-pr.yml](.github/workflows/simcapture-upstream-release-pr.yml)             | New file       | GitHub Action to open a PR when `upstream-main` has an upstream release newly merged into it  |
| Infra: signed APK releases              | [simcapture-github-release-signed-apk.yml](.github/workflows/simcapture-github-release-signed-apk.yml) | New file       | GitHub Action to create a GitHub Release on a merge to `main`                                 |
| Infra: signed APK releases              | [app/build.gradle.kts#L85](app/build.gradle.kts#L85)                                                   | Code change    | App's Package ID set to `com.simprints.simcapture`                                            |
| Docs: fork-specific README              | [README.md#L1-L20](README.md#L1-L20)                                                                    | Code change    | This section in README                                                                        |

SimCapture [releases](https://github.com/Simprints/SimCapture/releases) have the following naming scheme: `SimCapture-DHIS2-<upstream_DHIS2_version>-fork-<SimCapture_version>`. 
An example of a release APK name: `SimCapture-DHIS2-v3.3.1-fork-1-signed-release.apk`.

The upstream `README` is preserved below.


# README #

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=dhis2_dhis2-android-capture-app&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=dhis2_dhis2-android-capture-app)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=dhis2_dhis2-android-capture-app&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=dhis2_dhis2-android-capture-app)

Check the [Wiki](https://github.com/dhis2/dhis2-android-capture-app/wiki) for information about how to build the project and its architecture **(WIP)**

### What is this repository for? ###

DHIS2 Android application.
