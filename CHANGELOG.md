# Changelog


## Unreleased

- **Trees ask Waycave too.** PaulBunyan asked only GriefPrevention before felling, then removed logs and leaves directly, so a tree whose crown reached into a Waycave gate's protected zone (or one whose logs landed there) could take blocks out of it. Waycave has no claim API. A new setting, `ask-plugins` (default `[Waycave]`), names plugins to ask as well: each gets a test break of every log and leaf the fall would remove, and a test place where logs would land, sent only to that plugin's own listeners, so loggers and skill plugins never count them. A no, a listener that fails or no player to ask means the tree is chopped log by log, and logs that would land there go to the player, as with a claim. A named plugin that isn't installed is skipped. A server whose config.yml has no `ask-plugins` line gets the default. The wedge, saw and peavey ask the same way.
- The refusal now reads "Part of this tree is in a claim or a protected spot, so it won't fall." (wedge, saw and peavey lines say the same), since it may be a gate, not a claim. Player-facing wording, for review.
- Tested: unit tests (`./gradlew clean build`, 13 in ProtectionTest: a named guard's no stops a fall and a landing, its yes lets it fall, an unnamed plugin never sees the test break, a failing guard and a missing player are a no, a named plugin that isn't installed is skipped, GriefPrevention still decides first). A bot section (`only=gates` with `PB_TEST_WAYCAVE_JAR`) places a real Waycave gate and an oak whose leaves reach into its zone, with a control run where `ask-plugins: []` lets the same tree fall. Not tested on a server: the bot section was written but not run (test servers had to give way to other work), so a real Waycave gate, 26.1.2, 26.2 and 26.3 are untested; regrowth (stumps and saplings) still asks GriefPrevention only.

## 0.9.0

Real felling, last part (K597): the felling wedge, crosscut saw and peavey. This finishes the felling plan.

- Felling wedges, small for one-block trunks and large for 2x2: set one in the side of a trunk and the tree falls straight away from it. It comes back to whoever set it.
- Crosscut saw: hold right-click to saw through a fallen section, or twice to fell a standing tree. Two sawyers on opposite sides, taking turns in rhythm, saw a whole trunk or fell a tree in a few seconds.
- Peavey: roll a fallen section one block sideways across its length, away from you.
- The resource pack draws all four. `/paulbunyan give <player> tool <tool>` gives one.
- None of PaulBunyan's tools or special axes go into vanilla recipes. Before, two special axes in a crafting grid made a plain iron axe.

Tested: 124 unit tests, and bots on throwaway Paper servers: the new tools, special axes, fallen logs, terrain, one-piece, landing, species, felling and players on 26.1.2 (215 of 215), and the new tools, special axes and fallen logs on 26.3 (40 of 40; the wedge, peavey and crafting checks need a bot that can use a block, so they run on 26.1.2 and 26.2 only). Not tested: 26.2, and a real game client, so the pictures, the saw's motion and how two real players keep the rhythm are unseen.

## 0.8.0

Real felling, seventh part (K597): special axes.

- 121 special axes: eleven heads (copper, iron, gold, emerald, amethyst, quartz, lapis, redstone, glowstone, obsidian and prismarine) on eleven handle woods. Crafted like an axe, with stripped logs where the sticks go. Each head has one perk; see the README's table.
- Any special axe bucks 3 fallen logs with one swing, and on its own handle's wood swings half again as fast and bucks 5. Sneak to break one.
- PaulBunyan's own resource pack draws them, a handle and a head layered so every pairing works. It's written to `plugins/PaulBunyan/pack/` and attached to each release, for Packmule to serve. `tools.resource-pack: false` shows plain iron axes instead.
- New commands: `/paulbunyan give <player> <handle> <head>` and `/paulbunyan pack`. New settings: `tools.special-axes` and `tools.resource-pack`.
- Still to build in K597: the felling wedge, the crosscut saw and the peavey.

Tested: 117 unit tests, and bots on throwaway Paper servers: tools, fallen logs, terrain, one-piece, landing, species, felling and players on 26.1.2 (198 of 198), and tools, fallen logs and one-piece on 26.3 (45 of 46; the miss was opening a crafting table, which 26.3's bots can't do, so the crafting check now runs on 26.1.2 and 26.2 only, like placing blocks). Emerald's doubling is unit-tested only. Not tested: 26.2, and a real game client, so nobody has seen the pack's pictures in hand.

## 0.7.0

Real felling, sixth part (K597): fallen logs are joined.

- Logs a fall leaves lying break at half speed with any tool, one log at a time. The player's client is told the slower speed, so the crack keeps pace with the server. Logs placed by players and logs of standing trees break at normal speed.
- Chopping a fallen log never fells anything, even when what's left of the trunk looks like a tree.
- Fallen logs are marked in the chunk's own data, so they stay joined after the fall journal is gone and after a restart. A fallen log someone picks up and places again is an ordinary log.
- New setting `fallen-log-speed` (default `50`, a percent of normal). `100` breaks fallen logs like any other log.

Tested: 109 unit tests, and bots on throwaway Paper servers: fallen logs, terrain, one-piece, landing, species, felling and players on 26.1.2 (172 of 172, and the final fallen-log check 14 of 14), and fallen logs, terrain and one-piece on 26.3 (30 of 31; the one miss was the break-time comparison, which can't work with 26.3's hovering bots and now runs on 26.1.2 and 26.2 only). Not tested: 26.2, and a real game client, so whether the crack looks right at half speed is unseen.

## 0.6.0

Real felling, fifth part (K597): slopes.

- Uphill, the tree strikes the slope before it's flat and lands short, with its crown crushed into the hill. Before, it always tipped to flat, so more of it was buried.
- Over a drop, the animation tips past flat before the tree comes down. It still comes to rest flat on the block grid and falls in one piece.
- Grass, flowers and other walk-through blocks don't stop a fall. A tree leaning on a wall right beside it lands flat, as before.
- New setting `terrain` (default `true`). `false` tips every tree to flat, as in 0.5.0.

Tested: 108 unit tests, and bots on throwaway Paper servers: terrain, one-piece, landing, species and felling on 26.1.2 (152 of 152), and terrain and one-piece on 26.3 (20 of 20). Not tested: 26.2, and a real game client.

## 0.5.0

Real felling, fourth part (K597): the tree comes down in one piece.

- On flat ground the trunk lands whole, as before. Across a gap or a ravine it lies from one side to the other, like a bridge, and up to two logs can stick out past an edge. Before, every log over air dropped on its own.
- Whatever hangs further over a drop breaks off in sections of two to five logs, and each section falls on in one piece until it hits something. A section's fall counts toward `long-drop`.
- The breaks are seeded by the fall, so a fall finished after the server stops mid-fall breaks the same way.
- New setting `one-piece` (default `true`). `false` lands each log on its own, as in 0.4.0.

Tested: 100 unit tests, and bot runs on throwaway Paper servers: species, landing, felling and players on 26.1.2 (156 of 156), and the new gap and edge checks plus landing on 26.3. Not tested: 26.2, and a real game client.

## 0.4.0

Real felling, third part (K597): regrowth.

- A stump left by a high cut is remembered in `plugins/PaulBunyan/regrowth.txt`. About three in-game days later (`regrowth.stump-days`, with random spread) it grows into a wild, one-off tree of its own kind, made up by the plugin from a saved seed: a twisting trunk, odd branches, a ragged crown with mixed leaves. It only grows where every block is air or a small plant, outside claims, clear of builds, with nobody standing in it and its chunks loaded. Otherwise it waits and tries again. It never replaces anything else. Before, a stump got no sapling and stayed a stump.
- Once a fall's logs are cleared, up to three saplings of its kind sprout along where it lay, on grass or dirt outside claims, and are removed after 30 in-game days if they haven't grown. A cut at the ground still replants at the base.
- With Rewild installed, saplings PaulBunyan planted and trees grown from stumps or those saplings are reported as keeps. Rewild growing the original tree back is Rewild's side, not built here.
- Growth is logged in CoreProtect and Prism as the plugin, not a player.
- New `regrowth` settings: `stumps`, `stump-days`, `saplings`, `sapling-max`, `sapling-days`, `disabled-worlds` (a per-world off switch) and `rewild-link`.

Tested: 88 unit tests, and the full bot suite (315 of 315) on a throwaway Paper 26.1.2 server with an in-game day shortened to 5 seconds. Not tested live: GriefPrevention claims refusing growth (unit tests only), Rewild itself (the provider is tested against a stand-in), 26.2 and 26.3, and a real game client.

## 0.3.0

Real felling, second part (K597): how a fallen log lands.

- A log that would end up inside the ground, where the tree hit a slope, is crushed into a log item on top. Before, it rose out of the slope and landed as a block. A log that comes down on logs that landed first still stacks on them.
- A log that drops more than `long-drop.items-after` blocks (4) breaks into a log item, and more than `long-drop.sticks-after` (12) splinters into two sticks. Over an edge, the drop counts from the lip.
- Lava, fire, the void and land the player can't build on still send the log to the player, whatever the drop.

Tested: 44 unit tests, including each landing rule, and the full bot suite (286 of 286) on a throwaway Paper 26.1.2 server. The bots don't fell trees onto slopes or over edges, so crushing and long drops are covered by the unit tests only. Not yet seen in a real game client.

## 0.2.0 (2026-11-14)

- A tree now falls from the cut, toward its heavy side, with a creak first and fall damage for anyone under it.
- With GSit installed, players can sit only on logs lying on their side.
- Falls are recorded in Prism as the player who chopped, block by block. Prism 4.5 hides its builder class, so PaulBunyan reaches it through its public interface. If recording ever fails, the console says so once.
- Two small bugs found by the live bot checks are fixed.

Tested with bots on a copy of a real server on Paper 26.2 and 26.3 build 249: a chopped tree fell, its 6 logs and 55 leaves showed in Prism under the player, and a grave from LanternGraves near the tree was left alone. Not yet seen in a real game client.

## 0.1.0 (2026-11-08)

First version, for Paper 26.1 and up.

- Chop a natural tree with an axe and it tips over, lands as logs (or items), and a sapling is replanted.
  Sneak to chop one log.
- All overworld trees, including 2x2 spruce, jungle, dark oak and pale oak, mangroves and azaleas, plus
  huge crimson and warped fungi.
- Trees touching a build are left alone. Placed logs and other trees' leaves stay where they are.
- The axe wears one point per log, and a tree doesn't fall if that would break the axe.
- GriefPrevention claims are respected, and falls are recorded in CoreProtect.
- A journal finishes falls the server stopped or crashed during.
