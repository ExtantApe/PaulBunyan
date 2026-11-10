# Test audit

Kept by this repo's owning session (Minecraft: Plugins Lead), per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-10: first full audit (at 0.1.0)

What PaulBunyan promises (README and `docs/decisions.md`), and what proves it. "Unit" is `src/test`; "live" is `test/suite.mjs` on throwaway Paper 26.1.2, 26.2 and 26.3 servers with bots and GriefPrevention.

### Covered

**Only real trees fall**
- **Placed leaves, log cabins, log fences and placed logs never fall:** unit `placedLeavesMeanABuild`, `logCabinWallNeverFalls`, `playerPlacedLogIsNotATree`, `manyPlacedLogsMeanABuild`; live "log cabin", "log fence", "placed logs with placed leaves".
- **A tree touching a build is left alone, and the build is untouched:** unit `treeTouchingABuildIsLeftAlone`, `unloadedNeighbourCountsAsABuild`; live "house built around a real tree", "tree with a torch on it".
- **Two trees sharing leaves: only the chopped one falls,** and the other keeps its logs and leaves: unit `sharedCanopySplitsByDistance`; live.
- **Every overworld tree, 2x2s, branches, mangroves and huge fungi:** unit `giantTwoByTwoAndBranches`, `mangroveRootsAndMudAreNatural`, `fungusNeedsNyliumAndTakesItsCap`; live per tree type.
- **The size caps:** unit `sizeCap`; live "an enormous tree hits the size cap, and breaks like vanilla".

**Nothing is lost, doubled or put where it shouldn't be**
- **Every log is accounted for, nothing is left standing, no display left behind:** live, per tree type, and for ten bots at once.
- **A log never replaces a block** (torches, flowers, water), **never lands in lava, the void or inside a player, mob or boat:** unit `neverReplacesTorchesFlowersOrWater`, `lavaVoidAndClaimsGoToThePlayer`, `neverInsideAPlayer`, `buriedTooDeepGoesToThePlayer`; live "no log was placed inside the bot / the boat".
- **A crash mid-fall:** live "the fall journal is replayed after a kill -9", "chunk unloaded mid-fall", "no log lost or doubled".
- **CoreProtect sees the logs removed and placed:** live.

**Claims**
- **A fall never puts blocks in a claim the player can't build in,** and logs that would land there go to the player: live, with a real GriefPrevention claim.
- **A player without rights can't fell a claimed tree; the outside tree still falls:** live.
- **New in this audit: GriefPrevention installed but impossible to ask means no tree falls anywhere, and the log says why.** Unit `ProtectionTest` (3 tests, with a positive control without GriefPrevention). The code already did this; nothing tested it.

**Fairness and safety**
- **Nobody on or under a falling tree takes fall damage:** live.
- **Sneaking, creative and a worn axe chop one log; the axe wears one point per log:** live.
- **Ten fellings at once keep the average tick under 50 ms:** live.

### Skipped, with reasons

- **How the fall looks, sounds and feels**, and older clients through ViaVersion: real-client list.
- **GriefPrevention's own `allowBreak` throwing at the moment of a fall.** The code catches it and says no, the same branch as the new unit test. A test would need a fake GriefPrevention plugin class, which costs more than it proves.
