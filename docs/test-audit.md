# Test audit

Kept by this repo's owning session (Minecraft: Plugins Lead), per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-14: regrowth (0.4.0)

What regrowth promises, and what proves it. "Unit" is `src/test`; "live" is the `regrowth` section of `test/suite.mjs` on Paper 26.1.2, with an in-game day shortened to 5 seconds.

### Covered

- **A stump grows into a wild tree, the same one for the same seed:** unit `GrowerTest` (deterministic, differs by seed, own wood and leaves, bounds, every log joined to the stump, twists, 2x2 trunks); live "the stump grows into a tree after its time", "a natural tree the plugin would fell".
- **It never replaces anything but air and small plants, and never grows into a claim, a build, a standing player or an unloaded chunk:** unit `GrowthTest` (each refusal, a positive control, a 300-world property test that nothing placed is on a taken block, a replay after a crash); live "a block in the trunk's way is not replaced", "a build touching it keeps it a stump".
- **The list is saved atomically and survives bad lines:** unit `StoreTest`.
- **Everything is switchable, per world too:** unit `SettingsTest`; live "regrowth off" for a disabled world and for `stumps: false`.
- **Saplings sprout along a cleared fall, a few at most, and go after their days; the base sapling stays:** unit `SproutsTest`; live "saplings sprout along where it lay", "removed after their days".
- **GriefPrevention that can't be asked counts as a claim:** unit `ProtectionTest`.
- **Rewild gets the saplings and grown trees, and a failure reaches Rewild so it resets nothing:** unit `RewildKeepsTest` against stand-ins shaped like Rewild's interface.

### Skipped, with reasons

- **A real GriefPrevention claim stopping growth, live.** The refusal is the same code path as the unit-tested one, and the claim lookup is reflection on GriefPrevention's `getClaimAt`; the live check would be the first to prove that reflection works. Left for the next live session.
- **Rewild itself.** Needs Rewild's jar and an admin step (`/rewild plugins`); the provider is tested against stand-ins.
- **A crash while a tree is half grown.** The replay is unit-tested (blocks already placed are accepted), but no live `kill -9` during growth.
- **26.2 and 26.3.** Regrowth ran on 26.1.2 only.
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

## 2026-11-15, 0.5.0 (one-piece fall)

### Covered
- **On flat ground the tree lands whole where it tipped:** unit `OnePieceTest` and the existing bot species and landing checks.
- **Across a gap it lies whole, like a bridge; past an edge two logs stick out and the rest breaks off:** unit (boulders, a single boulder, a ledge) and live (a birch across a two-block gap, and over a three-block drop), on 26.1.2 and 26.3.
- **A broken-off section's drop counts toward long-drop:** unit (`theDropCountsInLanding`, the ledge case).
- **A held log never goes inside the ground, a player or a claim the player can't build in:** unit (`aHeldLogStaysOverAirButStillFollowsTheRules`). The claim case is the safety rule, so it has its own assertion.
- **No two logs end up in one block, and the same fall breaks the same way:** unit, over 50 seeds on a stepped drop with a branchy crown.
- **Sections are two to five logs:** unit, trunks of 2 to 40.
- **Regression:** bot run of species, landing, felling and players on 26.1.2, 156 of 156; landing on 26.3, 46 of 47 (one birch grew too short for the old check, since loosened and rerun, 15 of 15).

### Gaps skipped, with reasons
- **26.2 bot run:** the change is pure placement math with no version-specific calls, and 26.1.2 and 26.3 both pass.
- **How a bridge or a broken section looks in a real client:** real-client list.

## 2026-11-15, 0.6.0 (terrain)

### Covered
- **A quarter turn is exactly the flat tip, in all four directions, and no turn leaves the tree standing:** unit `TerrainTest`.
- **Flat ground is struck at flat; uphill before flat; over a drop past flat:** unit.
- **Grass, flowers and the tree's own leaves don't stop a fall; a wall right beside it is struck at once and the tree lands flat:** unit.
- **Uphill it lands short of where it would on flat ground, with every log accounted for:** live, 26.1.2 and 26.3.
- **Regression:** bots on 26.1.2 (terrain, one-piece, landing, species, felling), 152 of 152; terrain and one-piece on 26.3, 20 of 20.

### Gaps skipped, with reasons
- **The past-flat animation over a drop:** visual only; the landing it leads to is covered by the one-piece edge check.
- **26.2:** pure placement math, and 26.1.2 and 26.3 pass.

## 2026-11-15, 0.7.0 (fallen logs are joined)

### Covered
- **A fallen log breaks at half speed; a loose log at normal speed:** live, by the bot's break speed while digging (26.1.2 and 26.3) and by time to break as the server sees it, about twice a loose log's with the same wooden axe (26.1.2).
- **The slowdown comes off once the player stops:** live, after the break.
- **Chopping a fallen log fells nothing else:** live, exactly one log goes.
- **`fallen-log-speed` is clamped to 5..100, default 50:** unit `SettingsTest`.
- **Regression:** bots on 26.1.2 (fallen, terrain, one-piece, landing, species, felling, players), 172 of 172; fallen, terrain and one-piece on 26.3, 30 of 31 (the break-time comparison, now limited to versions where bots stand on the ground).

### Gaps skipped, with reasons
- **The mark surviving a restart:** it's the same chunk-data storage placed logs have used since 0.1, already covered by the restart checks.
- **Leaving mid-dig, a plugin stop mid-dig:** the change is transient and never saved with the player, so a stale slowdown can't outlive the session.
- **How the crack looks at half speed in a real client:** real-client list.
- **26.2:** no version-specific calls beyond the 26.1 attribute API, and 26.1.2 and 26.3 pass.

## 2026-11-15, 0.8.0 (special axes)

### Covered
- **121 recipes, crafted the axe way with stripped logs:** live, a copper axe with an oak handle clicked together at a crafting table (26.1.2); unit, every handle and head pairing parses back.
- **The numbers in each perk (iron 333 uses, obsidian 500, gold 125 at speed 12, quartz double on stems, prismarine underwater speed):** unit for the table; live for obsidian, gold and prismarine on the item itself.
- **Bucking 3, and 5 on its own wood, along the trunk, stopping at a gap:** unit (`Bucking.line`) and live on 26.1.2 and 26.3.
- **Redstone sends bucked logs to the inventory with nothing left on the ground:** live.
- **Lapis gives experience; glowstone's outline appears and is gone after 30 seconds:** live.
- **Amethyst's dust shows only while the amethyst axe is held:** live, 26.1.2 (the bots can only look around where they move).
- **Fallen logs and own wood combine in the dig speed:** unit.
- **The pack builds the same bytes every time, has every layer, 16x16 pictures, only its own namespace, and no pixel drawn by both layers:** unit; live, the file is written with its SHA-1.
- **Regression:** 26.1.2, 198 of 198; 26.3, 45 of 46 (crafting, now gated like placing).

### Gaps skipped, with reasons
- **Emerald in a live run:** leaf drops are random, so a live check would be slow and flaky; the doubling is unit-tested.
- **Quartz's speed on stems live:** it's a vanilla tool rule on the item, the same mechanism the obsidian and gold checks read.
- **Recipe-book discovery and the anvil repair:** vanilla behaviour on the item's components.
- **How the axes look in hand, and the pack through Packmule:** real-client list. Packmule's own suite covers serving a pack.

## 2026-11-15, 0.9.0 (wedge, crosscut saw, peavey)

### Covered
- **A wedge sends the tree straight away from it, fits only its trunk size, and always comes back:** unit (`away`, `fits`) and live (set in the east side, the tree falls west though the bot faces north, and the wedge returns), 26.1.2.
- **The saw alone cuts a section of two to five logs, and fells a standing tree in two holds:** live, 26.1.2 and 26.3.
- **Two sawyers on opposite sides, in rhythm, cut the whole trunk:** unit (`inRhythm`, `opposite`, including too quick, too slow, the same sawyer twice and the same side) and live with two bots, 26.1.2 and 26.3.
- **The peavey rolls a section across its length, away from the pusher:** unit (`roll`) and live, 26.1.2.
- **No PaulBunyan item goes into a vanilla recipe:** live, two special axes give no repair result, with two plain iron axes repairing as the control. This is the safety rule against losing an axe's components.
- **A small wedge crafts from three iron ingots:** live.
- **The pack draws every tool:** unit.
- **Regression:** 26.1.2, 215 of 215; 26.3, 40 of 40.

### Gaps skipped, with reasons
- **The peavey's refusals (solid block, a living thing, a claim, a drop):** small branches of one method; the claim refusal uses the same protection check the fall uses, which the claims tests cover. Worth a live case when the peavey next changes.
- **A wedge coming back after ten minutes or at shutdown:** the same give-back path the fall uses, which is covered.
- **The large wedge on a 2x2 tree live:** the size rule is unit-tested, and the fall itself is the existing 2x2 felling.
- **Real players keeping the saw's rhythm:** real-client list.

