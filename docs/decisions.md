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
