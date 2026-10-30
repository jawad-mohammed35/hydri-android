# Security

Please report vulnerabilities privately through [GitHub security reporting](https://github.com/jawad-mohammed35/hydri-android/security/advisories/new). Do not put credentials, signing files or hydration backups in public issues. Include the affected version, Android version and steps to reproduce using synthetic data.

Hydri stores hydration records in app-private Android storage. The APK has no Internet permission or account service. Manual CSV exports and JSON backups can contain names and hydration history; protect files you save or share. Automatic Android backup is disabled. App-private storage relies on Android's sandbox and device encryption; Hydri does not add separate database encryption.

Download official APKs from [this repository's releases](https://github.com/jawad-mohammed35/hydri-android/releases). A checksum checks file integrity; a matching signing certificate provides update continuity. Neither prevents someone from copying public source or distributing a separately signed imitation.

Maintainers should use passkeys or two-factor authentication, protect account recovery codes, review dependency updates and use least-privilege credentials. Never commit signing keys or build with untrusted code while signing credentials are available. See [the signing guide](docs/SIGNING.md) and [the repository review](docs/SECURITY-AND-LICENSING.md).
