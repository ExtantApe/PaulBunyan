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
