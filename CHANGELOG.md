# Changelog

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
