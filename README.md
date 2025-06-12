<!-- modrinth_exclude.start -->

[![Version](https://img.shields.io/modrinth/v/7YpmyzZr)](https://modrinth.com/mod/raid-restore)
[![Build](https://img.shields.io/github/actions/workflow/status/litetex-oss/mcm-raid-restore/check-build.yml?branch=dev)](https://github.com/litetex-oss/mcm-raid-restore/actions/workflows/check-build.yml?query=branch%3Adev)

# Raid Restorer

<!-- modrinth_exclude.end -->

Restores the raid mechanic like it was in 1.20 - makes (stacked) raid farms work again

<details><summary>Showcase</summary>

[Showcase](https://github.com/litetex-oss/mcm-raid-restore/assets/40789489/eec2998c-00ad-4f8b-ac66-219fea0d5107)

</details>

## Motivation/Why does this mod exist?

Raids [in 1.21](https://minecraft.wiki/w/Java_Edition_24w13a) got significantly changed and it's no longer possible to create a stacking raid farm.

See also: [More details, comparisons and arguments](https://github.com/litetex-oss/mcm-raid-restore/blob/dev/MOTIVATION.md)

<!-- modrinth_exclude.start -->

## Installation
[Installation guide for the latest release](https://github.com/litetex-oss/mcm-raid-restore/releases/latest#Installation)

### Usage in other mods

Add the following to ``build.gradle``:
```groovy
dependencies {
    modImplementation 'net.litetex.mcm:raid-restore:<version>'
    // Further documentation: https://wiki.fabricmc.net/documentation:fabric_loom
}
```

> [!NOTE]
> The contents are hosted on [Maven Central](https://repo.maven.apache.org/maven2/net/litetex/mcm/). You shouldn't have to change anything as this is the default maven repo.<br/>
> If this somehow shouldn't work you can also try [Modrinth Maven](https://support.modrinth.com/en/articles/8801191-modrinth-maven).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

<!-- modrinth_exclude.end -->
