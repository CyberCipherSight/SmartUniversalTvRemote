# Contributing

Contributions are welcome, especially verified IR profiles and hardware test results.

## Before opening a pull request

1. Do not commit secrets, API keys, tokens, passwords, signing keys, keystores or private configuration.
2. Keep generated build output (`build/`, APK/AAB files) out of source-control commits.
3. For new IR data, document its source and redistribution terms in `SOURCES.md`.
4. Prefer normalized protocol/address/command data instead of unexplained raw captures when possible.
5. Include the TV brand, exact model, country/region and phone model used for verification.
6. State which buttons were physically tested.

## Development

```bash
./gradlew assembleDebug
```

For IR changes, also review the verification utilities in `tools/` and test on real hardware before marking a profile verified.

## Pull requests

Keep pull requests focused. A useful IR-profile PR should explain:

- what TV/profile it adds or fixes;
- the protocol and carrier frequency;
- provenance/licensing of the command data;
- physical test results;
- whether any existing profile behavior changes.
