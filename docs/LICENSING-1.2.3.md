# Apache 2.0 adoption

On October 30, 2026, Jawad Mohammed explicitly authorized licensing Hydri under Apache License 2.0. The root LICENSE is the official unmodified Apache text, retrieved from apache.org; copyright attribution is in NOTICE and SPDX headers on Hydri's Kotlin sources/build scripts. The bundled Apache text and About page describe the same license. NOTICE is included as an attribution aid and does not add restrictions.

Apache 2.0 permits copying, modification, commercial use and redistribution subject to its conditions, including license/notice preservation, notices of modified files, and its patent provisions. It does not grant general trademark rights. No separate restrictive branding policy has been introduced.

Repository history lists one author, Jawad Mohammed. No separately contributed material with conflicting terms was identified. Existing Hydri assets were retained. This review does not establish exclusive copyright in generic or AI-assisted elements, or conduct trademark clearance.

The cached POM metadata for the 91 unchanged release-runtime modules was inspected; see [inventory](license-inventory-1.2.3.json). It reports Apache 2.0 and BSD-3-Clause. Guava's ListenableFuture inherits Apache 2.0 from guava-parent 26.0-android. The repackaged Glance protobuf runtime is BSD-3-Clause: Google's original copyright, conditions and disclaimer are preserved in LICENSES/Protobuf-BSD-3-Clause.txt and bundled in the app. These permissive dependencies can be distributed alongside Apache-licensed Hydri without replacing their licenses. JUnit remains EPL 1.0 and test-only. Existing dependency META-INF notices and Apache/EPL texts remain intact.

This inventory is a focused attribution review, not a claim that dependencies become licensed by Hydri or a guarantee about every upstream provenance question. Third-party code remains under its respective terms. [Third-party credits](../THIRD_PARTY_NOTICES.md) provide upstream links.
