# Real felling: the plan

An idea from the owner and a young tester, talked through with Helm on 2026-11-13 and recorded the same day. It's on hold: Minecraft plugins are in maintenance mode (ops D164), so nothing here is built yet. Decisions behind it are in [decisions.md](decisions.md), under 2026-11-13.

## How a tree falls

- **Cut height.** Wherever you chop, everything below the cut stays standing as a stump, and everything above falls.
- **Direction.** The tree falls toward its heaviest side, worked out from the shape of its logs and leaves. A felling wedge overrides this.
- **Warning.** It creaks and leans for about a second before it goes.
- **One piece.** The tree falls whole and only breaks where it strikes something: the ground, a boulder, another tree or a ledge. Breaks land haphazardly, in sections of two to five logs.
- **Crushed parts.** Whatever ends up inside the ground on impact turns into items. Leaves give sticks, apples, saplings and leaves, and logs give log items.
- **Terrain.** Uphill, the tree lands short and the crown crushes more. Downhill or over an edge, it tips past flat, then drops like sand. The animation can be at any angle, but the tree always comes to rest flat on the block grid.
- **Long drops.** A short drop lands as sections, a longer one breaks into log items, and a long way down splinters into sticks. Each cutoff is a setting.

## Getting hit

A server setting, `fall-danger`, works like difficulty:

| Level | Near the trunk | Hit by falling wood | Trunk hits your head | Trunk lands on your whole body |
| --- | --- | --- | --- | --- |
| `off` (default) | Knocked back | Flung, harder the bigger the hit | Flung hard | Flung clear |
| `max` | Knocked back | Damage | Big knockback and health loss | Death, with a LanternGraves grave in the nearest free block if it's installed |

There's room for levels in between. Worlds where players can't take damage always behave as `off`. The default stays `off`, so a server that installs Paul gets the old promise that a fall never hurts anyone. The owner's own server sets `max` on every world where players can take damage.

## Bucking and tools

All recipes use the normal crafting table. Nothing vanilla is changed or replaced.

- **Sections** lying on the ground are joined logs. A normal axe breaks them one log at a time, slower than a loose log.
- **Special axes** are built from a handle wood and a head material, drawn as separate layers so every pairing works without its own picture. Handles: the overworld and nether woods. Heads: real materials such as copper, iron, gold, emerald, amethyst, quartz, lapis, redstone, glowstone, obsidian and prismarine, with the final list chosen by the tester. Each head has one small perk. Any special axe bucks three logs at once. On a tree of its handle's wood, it swings faster and bucks five.
- **Felling wedge.** Set it in a trunk and the tree always falls away from it. A small wedge works on one-block trunks, and a large one, crafted from two small ones, on 2x2 trunks. You get it back.
- **Crosscut saw.** Alone, hold right-click to saw through a section in a few seconds. With two players on opposite sides, take turns clicking in rhythm to cut a whole trunk in seconds, or fell a big 2x2 tree. Holding right-click always works too, just slower.
- **Peavey.** Right-click a section to roll it one block, only sideways across its length, the way a real log rolls.
- **Sitting.** Fallen and placed logs are ordinary log blocks, so GSit can let players sit on them. Check first whether that's only a GSit setting, and whether GSit runs on 26.x. Paul doesn't depend on GSit.

## Regrowth

- **Stumps** slowly grow into wild, one-off organic trees, with twisting trunks, odd branches and mixed leaves. Paul does this itself and keeps a saved list of stumps. Stumps inside a claim or touching a build stay stumps.
- **Saplings** sprout at random along where a tree landed once the fall is cleared, and stay about a month. Cut right at the ground and a sapling goes at the base.
- **Rewild**, through the keep list it already offers other plugins:
  - It never touches a descendant. That means a sapling or tree that came from a fall, or a stump's regrowth.
  - If there's a stump, the stump regrows. Rewild leaves the original tree alone.
  - If there's no stump and no descendant, and the spot is clear, Rewild grows the original tree back.
  - If regrowing it would destroy anything, Rewild leaves it.
  - Both plugins still work without each other.

## Kept from today

Only natural trees fall. Trees touching builds and claims don't. The crash journal, CoreProtect logging (now including sections and rolled logs) and the size limits all stay.

## Not now

- Log stairs and slabs. They're possible with display models over real blocks, but they're a big job.
- Babe the Blue Ox.

## Testing and credit

- Bots test every rule on throwaway 26.1, 26.2 and 26.3 servers.
- Real-client checks: how the crushed fall looks, how the saw rhythm feels, whether the warning is enough, and how being flung reads.
- The tester is credited by his player name.
