<!-- modrinth_exclude.start -->

[![Version](https://img.shields.io/modrinth/v/7YpmyzZr)](https://modrinth.com/mod/raid-restore)
[![Build](https://img.shields.io/github/actions/workflow/status/litetex-oss/mcm-raid-restore/checkBuild.yml?branch=dev)](https://github.com/litetex-oss/mcm-raid-restore/actions/workflows/checkBuild.yml?query=branch%3Adev)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=litetex-oss_mcm-raid-restore&metric=alert_status)](https://sonarcloud.io/dashboard?id=litetex-oss_mcm-raid-restore)

<!-- modrinth_exclude.end -->

# Raid Restorer

Restores the raid mechanic like it was in 1.20, which makes raid farms work again.

<i>Note that this mod is still in a preview phase as 1.21 is not released yet</i>

## Motivation/Why does this mod exist?

Raids [in 1.21](https://minecraft.wiki/w/Java_Edition_24w13a) got significantly changed and it's no longer possible to create a stacking raid farm.

<!-- modrinth_exclude.start -->

<details><summary>More details, comparisons and arguments</summary>

### Resource gathering times
Imagine you have a big redstone project that requires a full Shulkerbox of Redstone blocks (27 x 64 x 9 = ~16k Redstone) or you need lots of Emeralds for trading.<br/>
How do you get those resources in 1.21+?

#### Options in 1.20
| Method | Items/h | Emeralds/h | Redstone/h | Time to fill Redstone block shulkerbox | Notes |
| --- |  --- | --- | --- | --- | --- |
| [Raid farm](https://www.youtube.com/watch?v=TDnppbTrdks)| ~8k | 3.5k | ~340 | 2d | † 1.21
| [Stacking Raid Farm](https://www.youtube.com/watch?v=n3mOlrMGjUg) | 128k | 56k | ~5.4k | ~3h | † 1.21
| [Max. Stacking Raid Farm](https://www.youtube.com/watch?v=yhW2Wub_4yw) | 896k | 471k | ~33.7k | 30min | † 1.21
| Witch farm (various designs) | 2-5k | - | 200-550 | 1.5-3d | At least 2x slower since 1.18 due to larger world height
| [Overworld mob farm](https://www.youtube.com/watch?v=Gg17wAr_IOI) (values adjusted for 1.18+) | ~9k | - | ~10 | >2 months | 
| [Mining/Caving at -54](https://www.youtube.com/watch?v=5cnLaNtxMek&t=480s) | 250 ore blocks | near 0 | ~1.5k | ~11h | <ul><li>No AFK</li><li>Non renewable</li><li>Requires appropriate equipment</li><li>Risk of death</li></ul>
| Trading (50 clerics + 50 smiths) | - | 0 when trading for Redstone <br/><br/>~6.5k<br/><i>1 per 4 Iron <br/>x 12 trades <br/>x 2 times/day <br/>x 5.5 (11 min/day) <br/>x 50 villagers</i> | ~13k<br/><i>2 per Emerald <br/>x 12 trades <br/>x 2 times/day <br/>x 5.5 (11 min/day) <br/>x 50 villagers</i> | 1.2h | <ul><li>No AFK</li><li>Requires >26k Iron/h (an average 4x Iron farm with 12 villagers produces ~1.5k Iron/h)</li><li>May cause lag as villagers need a lot of performance</li></ul>

#### Predicted Raid farm for 1.21
<i>Note that these values are predictions as there is no Raid farm for 1.21 yet.</i>
| Method | Items/h | Emeralds/h | Redstone/h | Time to fill Redstone block shulkerbox | Notes |
| --- |  --- | --- | --- | --- | --- |
| 1.21 Raid farm (predicted) | ~6k | ~2.7k | ~250 | 2.7 days | <ul><li>Stacking Raid farms are no longer possible</li><li>Requires collecting and drinking the Ominous Bottle (~2s)</li><li>Harder to AFK as bottle needs to be consumed + Killing mobs</li><li>Requires a beacon/regeneration to not starve to death</li><li>Additional count down of Raid omen (30s)</li><li>A raid cycle is roughly ~2mins in comparison to ~1m30s in 1.20 → 1.21 farm has an estimated 75% performance of 1.20</li></ul>

#### Conclusion for 1.21
As we can see without stacking Raid farms collecting these resources takes a lot longer or becomes way harder (no AFK).

### The nerf itself
Although no explicit reason have been stated why Stacked raid farms have been nerfed (they could have just kept the Bad Omen mechanic, maybe shorten the effect duration when killing a Raider to ~3 minutes - from 100 - and drop the bottle additionally), it's likely caused by the fact that parts of the community are complaining about the fact that these are to "overpowered/cheaty/illegal". (Quite the same happened for AFK fish farms in 1.16 - but you had Villager trading as an viable alternative)

IMHO this is the wrong way to go, as this change just causes negative effects:
1. It makes the game harder: "Very advanced players" (that need a lot of resources) now have no viable alternative. Do you like to mine/trade with villagers for hours or AFK for a lot longer? No? Well in 1.21 you have to. This can be very demotivating.
2. Stacked raid farms may be relatively easy to build (it still takes some hours to build them), but only so because (other) people did a lot of research and engineering on that mechanic.
3. Just because the mechanic exists doesn't mean that you HAVE to use it. I bet that most casual players don't even know/care about it. You can play the game in the way that you like.<br/>So I kind of don't understand why some people that complain about how other people play the game now get what they want despite having no positive effects for them and just bad ones for others.

<details><summary>The same opinion is also expressed by others</summary>

* > If you don't like them, don't use them. It's not like anyone is preventing you from build a witch farm instead. No need to ruin everyone else's fun just because you prefer to play differently.<sup>[Reddit](https://www.reddit.com/r/technicalminecraft/comments/r1yaee/comment/hm4seea)</sup>
* > Don’t like it, don’t build it is my opinion. If we plan on asking Mojang to nerf any farm/mechanic we don’t personally like or use ourselves things would be incredibly messy and nothing but nonstop arguments about what stays and what’s “cheating/too over powered.”<sup>[Reddit](https://www.reddit.com/r/technicalminecraft/comments/xab4ja/comment/insvopo)</sup>
* > I think the main problem is people telling others how to play. If you feel building a raid farm is "cheating", don't do it. Your friend seems to think otherwise... <sup>[Reddit](https://www.reddit.com/r/Minecraft/comments/fg148d/comment/fk1uv48)</sup>
* > I do think it is OP, but the fact that you can build a farm this powerful with so little effort should be considered an achievement of the technical community, because they did the research, the design, and they exploited the code in just the right way <sup>[Reddit](https://www.reddit.com/r/technicalminecraft/comments/xarqd9/comment/inwasb5)</sup>
* > ... because having tons of those materials doesn’t take away from the experience. However, it does not matter what I think or what Ilmango thinks, or what anyone else who comments here thinks. Play how u want to play bro c: <sup>[Reddit](https://www.reddit.com/r/technicalminecraft/comments/xarqd9/comment/invemiw)</sup>

</details>

</details>

## Installation
[Installation guide for the latest release](https://github.com/litetex-oss/mcm-raid-restore/releases/latest#Installation)

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

<!-- modrinth_exclude.end -->
