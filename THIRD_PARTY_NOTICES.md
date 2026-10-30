# Third-party notices

Hydri uses the following open-source projects. Copyright remains with each project's contributors. Versions are pinned in `app/build.gradle.kts`; Gradle resolves their transitive dependencies. No analytics or network client is included by Hydri.

- AndroidX Activity 1.9.3, Core 1.15.0, Compose BOM 2025.01.00 (UI, foundation, animation, Material, Material 3), Lifecycle 2.8.7, Navigation 2.8.5, Room 2.7.2, DataStore 1.1.1, WorkManager 2.10.0, Glance 1.1.1 — The Android Open Source Project, Apache License 2.0. https://android.googlesource.com/platform/frameworks/support/
- Material Icons — Google, Apache License 2.0. https://github.com/google/material-design-icons
- Glance repackaged Protocol Buffers runtime — Google Inc., BSD-3-Clause. Copyright 2008 Google Inc. Original conditions and disclaimer: `LICENSES/Protobuf-BSD-3-Clause.txt`, also bundled in the app. https://github.com/protocolbuffers/protobuf
- Guava ListenableFuture 1.0 — Google, Apache License 2.0 (inherited from guava-parent 26.0-android). https://github.com/google/guava
- Kotlin 2.1.0 and kotlinx.coroutines — JetBrains and contributors, Apache License 2.0. https://github.com/JetBrains/kotlin and https://github.com/Kotlin/kotlinx.coroutines
- SQLite — public domain, provided by Android. https://sqlite.org/copyright.html
- JUnit 4.13.2 (test-only) — JUnit contributors, Eclipse Public License 1.0. https://github.com/junit-team/junit4
- AndroidX Test 1.6.2 and JUnit extension 1.2.1 (test-only) — Android Open Source Project, Apache License 2.0.
- Android Gradle Plugin 8.7.3 and Gradle 8.14.6 (build tooling) — Apache License 2.0.

Full Apache and EPL license texts are in `LICENSES/` and bundled in the application assets. The in-app acknowledgments can be extended in `MoreScreen.kt`.

The hydration reference is attributed to the Institute of Medicine, *Dietary Reference Intakes for Water, Potassium, Sodium, Chloride, and Sulfate* (2005), https://doi.org/10.17226/10925. It describes total water from food and all beverages, not drinking-water-only goals. No affiliation or endorsement is implied.

Hydri's original code and documentation are licensed under Apache License 2.0. Copyright 2026 Jawad Mohammed. Dependencies retain their own copyrights and licenses. See LICENSE and NOTICE.

Additional Hydration Guide references: Nordic Nutrition Recommendations (2023), adopting EFSA (2010) total-water values, https://pub.norden.org/nord2023-003/fluid-and-water-balance.html; CDC, Water and Healthier Drinks (webpage updated March 5, 2026), https://www.cdc.gov/healthy-weight-growth/water-healthy-drinks/. Dates and reference scope are distinguished in the guide.
