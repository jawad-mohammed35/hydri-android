# Hydri: security, licensing and branding review

Reviewed October 29, 2026 (New York; October 30 UTC). This is a repository review and decision guide, not a legal opinion or a penetration-test certification.

## Existing rights and licensing

There is **no LICENSE for Hydri's own code** in the current tree or reachable history. `THIRD_PARTY_NOTICES.md` explicitly says that no owner license has been selected. The Apache 2.0 and EPL files in `LICENSES/` and app assets cover dependencies; they do not automatically license Hydri. GitHub's license metadata is also empty.

[GitHub explains](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository) that default copyright applies without a license. Public GitHub hosting nevertheless permits viewing and forking under GitHub's terms. Public visibility is not an open-source license or a technical barrier to copying; existing public copies cannot be recalled simply by changing future terms.

### Options and tradeoffs

- **MIT:** simple, attribution-preserving permission for modification, commercial use and redistribution. It does not fit a goal of prohibiting commercial clones. License/copyright notices must be retained.
- **Apache 2.0:** similarly permits commercial reuse and redistribution with notice obligations, plus an explicit patent grant. It does not grant general rights to the project's trademarks. See [the license](https://www.apache.org/licenses/LICENSE-2.0.html).
- **GPLv3:** permits commercial use and redistribution, while covered distributed modifications must satisfy its corresponding-source and license requirements. It discourages closed-source redistribution but does not ban competing apps. See [GNU's FAQ](https://www.gnu.org/licenses/gpl-faq.html).
- **Owner-approved source-available terms:** can permit inspection and specified private/personal use while requiring permission for commercial use or redistribution. Restrictions must be drafted clearly, including what users may do with APKs, modifications and contributions. This reduces community reuse and contribution opportunities and adds enforcement/legal maintenance work. A lawyer-reviewed agreement is preferable to improvised restrictive clauses.

**Recommendation for the stated goal:** consider source-available terms with an explicit personal-use APK grant, a commercial/redistribution permission process and separate branding guidance. This requires the owner's approval and rights review. If genuine open-source collaboration is more important, consider GPLv3 plus a branding policy, accepting commercial forks. [The OSI definition](https://opensource.org/osd) permits commercial use; “noncommercial open source” is not OSI open source.

No license, binary-use agreement, contribution-license agreement or branding restriction was adopted in this update. Discuss code contributions before accepting them while licensing remains unresolved; contributors retain rights to their work unless properly licensed or assigned. Earlier valid grants, if any are later discovered, cannot simply be withdrawn retroactively from existing recipients.

## Copyright and branding

Copyright can protect original, sufficiently authored code, documentation and artwork; it does not monopolize app ideas, ordinary UI patterns or a name. Trademark law concerns source-identifying names/logos and confusing branding. [USPTO's comparison](https://www.uspto.gov/trademarks/basics/trademark-patent-copyright) explains the distinction. No trademark clearance search or registration was performed, and this review does not establish that “Hydri” is available for registration.

Consider a professional name/logo clearance search before registration in relevant markets, and a separate policy requiring forks to avoid implying official endorsement or using reserved branding. A policy cannot cancel rights already granted by the actual code license. Simple water-drop graphics and generic interface ideas may have limited exclusive protection. Preserve editable original assets and evidence of human design/authorship.

Proposed notice, **for approval after ownership review**: “Copyright © 2026 Jawad Mohammed. All rights reserved.” It would need to distinguish third-party material and remain consistent with the selected license. A notice does not override open-source permissions. AI assistance also matters: the [U.S. Copyright Office's 2025 guidance](https://www.copyright.gov/newsnet/2025/1060.html) requires sufficient human-authored expression; do not assume every generated element is independently copyrightable. Obtain jurisdiction-specific advice for registration/enforcement.

## Security findings and actions

- A local pattern scan covered **128 unique reachable text blobs** across **288 reachable Git objects**, plus sensitive filename checks. It found no matching credential patterns or tracked signing/sensitive-config paths. The scan excludes binary text detection and text blobs over 2 MB and is not proof that no secret ever existed in unreachable objects or elsewhere.
- The current tree has no application tokens, API keys, personal-data logging, hardcoded custom signing passwords or tracked keystores identified by review. The local debug keystore is outside Git. GitHub Actions secret metadata shows **zero repository secrets**; no release workflows are tracked. See [signing details and backup instructions](SIGNING.md).
- **Implemented:** stronger ignore rules for key/keystore formats, signing properties, service-account files, bundles, local logs and credentials. `.gitignore` is preventive; it does not remove secrets already committed.
- **Implemented and verified on GitHub:** secret scanning, secret-scanning push protection and private vulnerability reporting enabled. No license or visibility change was made. Main has no branch-protection configuration returned by the API; protections/required CI reviews and Dependabot update automation remain recommendations, not changes made here.
- OSV's batch API returned **no advisories for 91 resolved release-runtime Maven coordinates** on October 30 UTC. Coverage is database/package-specific, not a guarantee of safety. Platform Android vulnerabilities and all build-tool issues are outside that result.
- Separate review found [GHSA-mvvg-497x-hmj8](https://github.com/gradle/gradle/security/advisories/GHSA-mvvg-497x-hmj8), affecting Gradle 8.11.1. **The wrapper is updated to patched 8.14.6**, with the official distribution SHA-256 pinned. The advisory concerns a local attacker racing a build worker connection; it is not a remotely exploitable Hydri APK issue. Avoid shared/untrusted build hosts.
- The release APK has no Internet, broad-storage, location or contacts permission. Notifications, boot, vibration and WorkManager wake locks support existing features; AndroidX's signature-protected internal receiver permission is expected. Public activity/widget entry points are required; shortcuts open editors rather than silently performing destructive actions. Reminder PendingIntents are immutable.
- Data remains app-private and automatic cloud backup is disabled. Manual backups/CSV files are plaintext and can contain personal information; store/share them deliberately. Import validation, size limits and transactional restore are retained. No schema or data reset is introduced.
- The local publishing helper retrieves Git Credential Manager credentials into memory and uploads APK/checksums through GitHub's API. It is ignored rather than tracked. Do not print its headers or persist credentials. Protect the maintainer account with passkeys/2FA and recovery backups. An account compromise can replace both an APK and its checksum; checksums alone are not an independent trust anchor.

No credential exposure was identified that establishes a need to rotate an account token or key. If an exposure is discovered later, rotate/revoke the affected secret; history cleanup alone is not sufficient. R8 obfuscation is not a substitute for key protection and cannot stop copying public code, so no risky obfuscation change was added solely as “protection.”

## Decisions reserved for the owner

Choose the source/binary license, approve any copyright/branding policy, decide on trademark clearance/registration, and approve any future signing-identity migration. Those decisions are intentionally unresolved. Safe repository safeguards, documentation and UI work proceed independently.
