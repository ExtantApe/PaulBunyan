# Changelog

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
