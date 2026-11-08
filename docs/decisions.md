# Decisions

The owner's decisions for Paul Bunyan, with the date each was made. Where one differs from the original
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
