# SkriptDocsGenerator

SkriptDocsGenerator exports Skript and addon syntax metadata into JSON files that can be published on documentation platforms such as SkriptHub or skUnity.

This repository is a maintained fork of the original project with updated build and test infrastructure for newer Skript and API versions.

## Quick start

1. Download the [latest release](https://github.com/yhdev7935/SkriptDocsGenerator/releases/latest) and place it in your `plugins/` folder.
2. Start your server with Skript installed.
3. Run `/skriptdocsgenerator`.
4. Generated JSON files are written to `plugins/SkriptDocsGenerator/`.

## What's updated in this fork

- Build baseline modernized to Gradle 8.10.2 and Java 21 toolchain.
- CI updated to run on JDK 21.
- Skript and API versions are no longer hard-pinned in source; they are configurable through Gradle properties.
- MockBukkit + JUnit 5 tests were added, including command execution tests for `/skriptdocsgenerator`.

## Build and test

```bash
./gradlew clean build
./gradlew test
```

## Version configuration

Default values are set in `build.gradle` and can be overridden at build time:

- `skriptVersion`
- `spigotApiVersion`
- `paperApiVersion`
- `mockBukkitVersion`
- `pluginApiVersion`

Example:

```bash
./gradlew clean build \
  -PskriptVersion=2.14.1 \
  -PspigotApiVersion=1.21.11-R0.2-SNAPSHOT \
  -PpaperApiVersion=1.21.11-R0.1-SNAPSHOT \
  -PpluginApiVersion=1.21
```

Note: `skriptVersion` and `spigotApiVersion` must be changed together. The build fails fast if only one is overridden.

## Compatibility note

Current upstream Skript marks several legacy static APIs as deprecated for removal. The plugin still uses those APIs for backward compatibility, so deprecation warnings during compilation are currently expected.

## Support

- Open an issue: https://github.com/yhdev7935/SkriptDocsGenerator/issues
