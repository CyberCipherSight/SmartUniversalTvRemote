# Security Policy

## Secret-handling rule

This repository must never contain credentials or private signing material.

Do **not** commit:

- API keys or access tokens
- account passwords
- Android signing keystores (`.jks`, `.keystore`)
- private keys or certificates
- service-account credentials
- `.env` files
- `local.properties`
- Play Console credentials
- private Firebase/Google service configuration

The `.gitignore` contains defensive patterns, but contributors are still responsible for reviewing every commit before pushing.

If a credential is accidentally committed, revoke/rotate it immediately. Removing it from a later commit is not sufficient because Git history may still contain it.

## Reporting a vulnerability

Please avoid publishing exploitable security issues with sensitive details in a public issue. Contact the repository owner privately through their GitHub profile until a dedicated security contact is configured.
