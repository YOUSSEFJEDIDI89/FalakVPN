## What does this change?

<!-- One or two sentences. -->

## Why?

<!-- Link the issue if there is one: Closes #123 -->

## How did you verify it?

<!--
Tests that were run and their output. If you touched the `core` module,
please show the result of `./gradlew :core:test`.
-->

- [ ] `./gradlew :core:test` passes
- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] Tested on a device (Android version: ____)

## Checklist

- [ ] The `core` module still has **no Android dependencies** (the engine must stay testable on a JVM)
- [ ] Added or updated a test for the behaviour you changed
- [ ] No new native libraries or opaque binaries
- [ ] No secrets, keystores or personal server credentials are committed
