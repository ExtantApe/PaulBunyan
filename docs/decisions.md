# Decisions

The owner's decisions for PaulBunyan, with the date each was made. Where one differs from the original
brief's suggestion, it says so.

## 2026-11-07

**Already decided before work began**

- By default, logs land as blocks to chop again. A setting turns them into items instead (`land-as`).
- Natural trees only.
- A sapling is replanted.
- Mangroves are included.

**Asked at the start of the build**

- **Doubtful cases: fell the sure part.** The brief suggested skipping any doubtful tree. Instead, where
  two trees share leaves, or a placed log is stuck to a tree, the natural part falls and the rest stays.
  A tree that touches a build is the exception: it doesn't fall at all.
- **What triggers a fall:** an axe. Sneaking chops one log the normal way.
- **Axe wear:** one point per log, with Unbreaking applied. The tree doesn't fall if that could break
  the axe.
- **Replant:** free, and only where the sapling can grow.
- **Nether stems: included.** The brief suggested leaving them out of the first version. Huge crimson
  and warped fungi fall, and a fungus is replanted only on its own nylium.
- **Leaves: fall with the tree** and give out their drops. The brief suggested letting them decay in place.

**Standing rules that shaped the build**

- Target Paper 26.1 and up on Java 25, with no 1.21.x support. Test on 26.1, 26.2 and 26.3.
- Paper only: `paper-api` and `paper-plugin.yml`.
- Test servers are throwaway, local, one at a time, and deleted after each run. The owner accepts the
  Minecraft EULA for them.
- Where there's a choice, do it well for players: clear messages, satisfying feedback, sensible defaults.
- Kids come first on the servers. Here that means a fall never hurts anyone: falling logs do no damage,
  nobody on or under a tree takes fall damage, and logs never land inside a player.

## 2026-11-07, later

- **Go public:** the owner approved making the repo and its v0.1.0 GitHub Release public, after the
  pre-public audit and with the leak check in CI. Modrinth, Hangar and Spigot wait for the owner's own code
  review.

## 2026-11-13

- **Real felling plan:** recorded and on hold while Minecraft is in maintenance mode (ops D164). The design is in [felling-plan.md](felling-plan.md).
- **Fall damage becomes a setting.** It replaces the 2026-11-07 rule that a fall never hurts anyone. `fall-danger` works like difficulty. The default `off` keeps that old rule, and players are only knocked back or flung. At `max`, falling wood damages players, and a trunk landing on a whole body kills. The owner's own server runs `max` on every world where players can take damage. The owner's words: "It should be a server setting almost like Minecraft difficult. But in our case, ours is set to the max for best experience."
- **Recipes:** the normal crafting table. The workbench idea waits for log stairs and slabs.
- **Matching wood:** an axe cuts trees of its own handle wood faster.
- **Rewild and felled trees:** descendants are never touched, and stumps regrow. With nothing left and a clear spot, Rewild grows the original back. If regrowing it would destroy anything, Rewild leaves it.

## 2026-11-14

- **Real felling, first part (ops D170):** the owner asked for everything left to be built and staged for a test session: "Build everything we have left plugin wise and stage it", with the scope "Include the felling plan" and "Run as inconspicuous as possible on Minecraft stuff so it's great work but cheap." 0.2.0 builds the parts of [felling-plan.md](felling-plan.md) that could be made solid in time: the cut height and stump, falling toward the heaviest side, the one-second creak, and `fall-danger` (`off` default, `max`, per world). Logging also goes to Prism (ops D168). Not built yet, and left out of the test: falling in one piece with breaks, crushed parts, terrain and long drops, special axes, the wedge, crosscut saw and peavey, regrowth and the Rewild link. A stump left by a high cut gets no sapling until regrowth exists.
- **Real felling, second part (K597, 2026-11-14):** crushed parts and long drops from [felling-plan.md](felling-plan.md). A log that ends up inside the ground is crushed into a log item. Past `long-drop.items-after` (4) it breaks into a log item, and past `long-drop.sticks-after` (12) it splinters into two sticks. Leaves already drop their items. Still to build: falling in one piece with sections, terrain angles, the tools, regrowth and the Rewild link.
- **Credit (2026-11-14, owner, D193):** the credit reads "A big thanks goes to LegoShepherd for brainstorming this idea out with me." It names no specific feature, and gives no real name, age or relation.
- **Real felling, third part (K597, 2026-11-14):** regrowth is built (0.4.0). A stump left by a high cut grows into a wild one-off tree after `regrowth.stump-days` (3) in-game days, only where every block is air or a small plant, outside claims, clear of builds and loaded. Saplings sprout along a cleared fall (up to 3, removed after 30 days) and PaulBunyan reports its saplings and grown trees to Rewild as keeps. Each part has an off switch, and `regrowth.disabled-worlds` turns it off per world. The 0.2.0 rule that a stump gets no sapling until regrowth exists is replaced: the stump regrows. Still to build: falling in one piece with sections, terrain angles, the tools, and Rewild growing the original tree back (that last one is Rewild's side).
- **Real felling, fourth part (K597, 2026-11-15):** the tree comes down in one piece (0.5.0). Once it has tipped flat, it drops as a whole until part of it strikes something. Everything between the first and last thing it rests on stays, so a trunk across a gap lies there like a bridge, and up to two logs may stick out past the end. The rest breaks off in sections of two to five logs that fall on, each in one piece. A piece's drop counts toward `long-drop`. The breaks are seeded by the fall, so a fall finished after a crash breaks the same way. `one-piece: false` keeps the old log-by-log landing. Read as: the plan's "only breaks where it strikes something" means a trunk breaks at the edge of what holds it up. Still to build: terrain angles (tipping past flat on a slope), sections as joined logs that a normal axe bucks slowly, and the tools.
- **Real felling, sixth part (K597, 2026-11-15):** fallen logs are joined (0.7.0), from the plan's "Sections lying on the ground are joined logs. A normal axe breaks them one log at a time, slower than a loose log." Read as: every log a fall leaves lying is joined, whether its piece landed whole or broke off, and "slower" is half speed by default (`fallen-log-speed`). It applies to any tool, since the plan's special axes will be the faster way. A fallen log never fells anything when chopped. Which section a log belongs to isn't stored yet; the peavey and crosscut saw will need that, and it can be added when they're built. Still to build in K597: the tools.
- **Real felling, fifth part (K597, 2026-11-15):** terrain (0.6.0). The fall is worked out against the ground while the tree still stands: it tips until a log would enter something solid (grass, flowers and other walk-through blocks don't count). Struck between 30 and 81 degrees, it lands short, at that angle, and logs inside the hill are crushed. Struck sooner than 30 degrees, it's leaning on something right beside it, so it lands flat as before. Not struck by 90, it tips past flat (up to 135) in the animation and then lands flat and falls in one piece. Two logs that round to the same block at a slant stack. `terrain: false` keeps the flat tip. The owner's work order (Simon, 2026-11-15): PaulBunyan, then MobSquad, then MobShowdown. Still to build in K597: sections as joined logs that a normal axe bucks slowly, and the tools.

## 2026-11-15

**The tools (K597), relayed verbatim by Simon from the owner's chat**

- **Axe heads:** "All eleven at once": copper, iron, gold, emerald, amethyst, quartz, lapis, redstone, glowstone, obsidian and prismarine, each with its perk from the draft. Copper is plain. Iron wears a quarter less. Gold is fastest and wears fastest. Emerald gives more fruit and saplings. Amethyst shows which way a tree will fall. Quartz is fast on stems. Lapis gives XP. Redstone puts bucked logs straight into the inventory. Glowstone makes the falling logs glow. Obsidian wears half as fast. Prismarine works at full speed underwater.
- **Art:** "Its own small pack (Recommended)". PaulBunyan ships a small resource pack that Packmule serves, in the server's pixel style, with handle and head drawn as separate layers so every pairing works. The wedge, crosscut saw and peavey share it. Read as: 16x16 textures in the vanilla look, drawn for this plugin (no game art copied), in PaulBunyan's own namespace.
- **Special axes as built (0.8.0), read from the plan where it gave no number:** a special axe is an iron axe underneath, with iron's damage and its numbers as the plain baseline: 250 uses and mining speed 6. Iron's perk is 333 uses, obsidian's 500, and gold is speed 12 with 125 uses. The recipe is an axe's shape with stripped logs of the handle's wood for the sticks, so no vanilla recipe changes. The head materials are the plain items: copper ingot, iron ingot, gold ingot, emerald, amethyst shard, nether quartz, lapis lazuli, redstone dust, glowstone dust, obsidian and prismarine shard. Emerald doubles saplings, propagules and apples from the leaves. Lapis gives one experience point for every three logs, at least one. Glowstone outlines the landed logs for 30 seconds. Amethyst's dust runs from the trunk the way the tree would fall, as far as the tree is tall, and only the holder sees it. Bucking takes the next logs along the same trunk, either side, nearest first, and sneaking breaks one. The handle woods are the eleven tree woods PaulBunyan fells; bamboo isn't a tree.
- **Wedge, saw and peavey as built (0.9.0), read from the plan where it gave no number:** recipes are a small wedge from three iron ingots in an L, a large wedge from two small ones, a crosscut saw from two sticks over three iron ingots, and a peavey from two iron ingots over two sticks. A wedge needs a tree that would fall and comes back after the fall, when its log is broken, after ten minutes, or at shutdown, never lost. The saw's few seconds are three, a section is up to five logs, a standing trunk takes two holds, the rhythm is a stroke 6 to 30 ticks after the partner's from the opposite side, and eight strokes finish the cut, up to 32 logs. The peavey rolls up to five logs, refuses solid blocks, living things, claims and drops over three blocks. The three tools are trial keys underneath; a guard keeps every PaulBunyan item out of vanilla recipes, which also stops two special axes repairing into a plain iron axe.
- **Order:** PaulBunyan's tools come before MobSquad, because the owner picked "Finish PaulBunyan first".

