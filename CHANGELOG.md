# Changelog

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
