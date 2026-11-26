# GKD-XA v1.5.5 fixes and official release

- auth.status: granted
- authorization: user explicitly authorized implementing the findings from the 2026-11-25 GitHub source audit, testing, committing and pushing main, and creating the v1.5.5 official-release tag.
- in_scope: [84593320z/GKD-XA](https://github.com/84593320z/GKD-XA), main baseline commit `8095c633e95b0b82adc8c41a9cf0df6de9937a8e`; fix snapshot-only AI side effects, clarify snapshot/generation modes, retire enhanced mode and its real-action execution path, reject public plaintext HTTP for AI requests, correct AI input/privacy guidance, and warn before exporting backups containing provider credentials. Include regression tests, version metadata, main push, and `v1.5.5` tag that triggers the repository official-release workflow.
- network_profile: repository-maintenance; push authorized branch, main and version tag only after validation; existing GitHub Actions uses repository signing secrets.
- out_of_scope: no device access or installation, no real AI provider requests, no secrets/key material read or published, no other repositories or unrelated history/branches, no manual release outside the repository’s existing tagged workflow.
- release identity: `li.songe.gkdx`, versionCode `156`, versionName `1.5.5`, tag `v1.5.5`.
- validation: repository regression gates, production release metadata check, and unsigned/local perf build where the environment permits; GitHub Actions owns official signing, APK identity verification and publishing.
