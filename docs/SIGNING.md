# Hydri signing and private backups

## Current configuration

Hydri's release variant is **non-debuggable**, but uses the original **Android debug/testing signing identity**, not a dedicated production release identity. `app/build.gradle.kts` deliberately reuses the SDK debug signing configuration and checks the certificate fingerprint before release signing. No replacement key was generated for this update.

The local signing file found on the maintainer's machine is `%USERPROFILE%\.android\debug.keystore`. It is outside the repository and is not tracked in any reachable Git history. GitHub Actions currently has zero repository secrets and zero environments, and this repository has no tracked release workflow. No custom signing password is hardcoded in Gradle; the configuration inherits Android's standard debug defaults. Those defaults are well-known and should not be treated as strong password protection. This does not mean the private key itself is public; other developers' debug keystores are different identities.

The public certificate SHA-256 is `ed0e989eaa47ed777d32e9dcc9902f4462d349f5ad132bb8568971383b3ec9f0`. Certificates and fingerprints are public identifiers; the private key inside the keystore must remain private.

## What to preserve privately

Keep the exact keystore file, its store type, keystore password, signing-key alias and key password. Also retain the package ID (`com.hydri.app`), certificate fingerprint, build configuration and release version-code history. Record the actual credentials privately in a password manager; do not paste them into issues, README files, chat logs or command-line arguments.

1. Create two encrypted backups of the existing keystore outside this checkout and outside public/shared folders. Use a trusted encrypted archive or encrypted backup product, with its encryption passphrase kept separately in a password manager.
2. Keep one backup offline and another in a separate trusted location. An encrypted backup may use private cloud storage if access and account recovery are secured; do not upload a raw keystore.
3. Document the store password, alias and key password in the password manager, together with the backup locations. Losing either the file or necessary credentials can make signing future updates impossible.
4. Test restoration into a separate private temporary directory. Use a keystore inspection tool with interactive password entry to compare the public certificate fingerprint, then sign a disposable release and verify its certificate. Never overwrite the active keystore during a restoration test.
5. Do not delete/regenerate the default Android debug keystore when resetting Android Studio or moving computers. Hydri currently depends on this particular file. It may also sign other local development apps.

No private backup was created inside this repository as part of this review, and no credentials were exported. Existing backup copies elsewhere could not be verified.

## Future production signing

[Android describes debug signing as insecure by design](https://developer.android.com/studio/publish/app-signing); it is not the appropriate long-term production configuration. The current private key still matters: possession of it can permit signing malicious compatible updates. A stable GitHub label does not change the nature of the certificate.

Preserve the current identity for existing users. One option for an approved follow-up is to import the **existing private key and unchanged certificate** into a separately managed private release keystore with strong credentials, then update the signing configuration and verify the same fingerprint. This can separate Hydri from the SDK's disposable debug-file location without changing update identity. It does not erase any prior compromise or turn the debug-origin certificate into a different production certificate.

Simply replacing the identity with a newly generated release key can break updates. Android signing-key rotation, older Android compatibility and possible future Play App Signing need a separately tested plan. Do not generate a new identity, repackage private keys, rotate a key or change signing credentials on the owner's behalf without approval.

If the private key is ever exposed, treat it as a security incident; removing a file from Git is insufficient. Protect users, revoke exposed account tokens where relevant and plan a signing migration. Merely changing the keystore password does not invalidate a stolen private key.
