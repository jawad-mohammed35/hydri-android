# Release history maintenance

On October 30, 2026, after v1.2.3 was published as stable and its public APK was downloaded, signature-checked, installed, launched and tested for data preservation, the following GitHub pre-release entries and their assets were removed at the owner's explicit request:

- v1.0.0-test.1 — source tag retained at `e9509f79a444d7bd3b1bbcf05c3eb84b0df866c8`.
- v1.1.0 — source tag retained at `fb3787f6d14bda4f39e0342b4e8ef0b5f566f2c5`.
- v1.2.0 — source tag retained at `f601319dd0fef9cda46f8128839d583396f183f4`.

GitHub classified all three as pre-releases. The repository had zero active GitHub Actions workflows; no active workflow references required these artifacts. All source commits and tags remain intact. Signing files and build configuration were not deleted.

Stable releases **v1.2.1**, **v1.2.2**, and latest **v1.2.3** remain published with their assets. No previous stable release was deleted.

Recommendation: retain the older stable releases for troubleshooting, reproducibility and historical reference. Three stable entries keep the release page manageable, and GitHub already highlights the latest. Removing stable binaries offers little benefit here; any future stable cleanup should be an explicit owner decision with recovery needs considered first.

Old verification reports record the results at their original publication date. Their retired pre-release APKs are no longer downloadable; source remains available through the retained tags. Current downloads are listed on the [Releases page](https://github.com/jawad-mohammed35/hydri-android/releases).
