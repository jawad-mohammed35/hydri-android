# Licensing of previous Hydri builds

Reviewed October 30, 2026 against the retained Git tags and locally cached copies of the published APKs. This records the repository evidence and current scope; it is **not a new license grant for older versions**. No old tag, commit, release description or APK was modified by this review.

## What the history shows

The root Apache 2.0 LICENSE, NOTICE and Hydri copyright/SPDX notices were introduced in commit [`2ce0da3`](https://github.com/jawad-mohammed35/hydri-android/commit/2ce0da3), authored October 30, 2026 at 16:36 EDT. **v1.2.3** is the first published tag containing that commit. Its source explicitly licenses Hydri's original code and documentation under Apache 2.0, and its APK contains `assets/Hydri-NOTICE.txt` as well as the license texts.

The retained tags **v1.0.0-test.1, v1.1.0, v1.2.0, v1.2.1 and v1.2.2** contain no root LICENSE or NOTICE for Hydri itself. They contain `LICENSES/Apache-2.0.txt` and `LICENSES/EPL-1.0.html` for third-party components. Their THIRD_PARTY_NOTICES.md explicitly states that no license for Hydri's own source had been selected. The cached APKs likewise contain Apache/EPL dependency license assets, but no Hydri-specific NOTICE. Merely bundling a library's license does not clearly license the entire app under that license.

Git tags identify fixed source snapshots. Adding a license file on main does not insert it into earlier snapshots, rewrite already-published APKs or, by itself, clearly declare every historical Hydri version Apache-licensed. Public GitHub visibility also is not a software license, although GitHub's terms allow viewing and forking on the service. [GitHub's licensing guidance](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository) explains that distinction.

The current Apache grant nevertheless covers the original material included in the licensed current work, including unchanged code shared with earlier versions. Someone can reuse that covered code from the licensed version under its conditions. It would be misleading to claim that all code appearing in an older tag remains unavailable simply because the old snapshot lacked LICENSE. Conversely, permission for the current work should not be presented as an unqualified blanket grant for every older source snapshot or APK. [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) defines the covered work and its source/object forms and grants permissions subject to its conditions.

## Can the owner license older versions explicitly?

Yes, if Jawad Mohammed owns or is authorized to license the relevant original material. An explicit additional grant can identify the older tags/commit SHAs and, for APKs, exact versions and SHA-256 hashes. It can state that the owner's original material in those versions is available under Apache 2.0, while preserving third-party copyrights, license conditions and notices. Contributions or assets owned by someone else require sufficient existing permission or the relevant owner's consent; a new Hydri notice cannot replace those rights.

Such a grant can be documented in a new, clearly scoped notice on the current branch and linked from older release descriptions. It does **not** require moving tags, rewriting commits, changing signing identities or replacing the old binaries. The published APK bytes and signatures can remain intact. A linked license/attribution notice can explain the permission attached to the identified binary; any binary repackaging would be a separate operation requiring approval and fresh verification.

## Recommendation

Keep Apache 2.0 and the existing notices for v1.2.3 and the current work. Keep this historical clarification available to readers. If the owner wants a uniformly Apache-licensed historical catalog, approve a separate explicit grant after confirming ownership and scope, then add a linked note to the still-published older stable releases. Until that decision, do not describe pre-v1.2.3 releases as automatically Apache-licensed, and do not alter their artifacts or Git history. No retroactive grant or historical-release change has been enacted here.
