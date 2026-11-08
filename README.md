# PaulBunyan

A Paper plugin that makes trees fall. Chop a natural tree with an axe and it tips over, the logs land on the ground, and a sapling goes back in where it stood.

**Status:** pre-release. STATUS_LINE

- Requires Paper 26.1 or newer, on Java 25. Tested on 26.1.2, 26.2 and 26.3. Java Edition only.
- Replaces tree-felling plugins such as TreeAssist and LumberJack.

## What it does

- Chop any log of a natural tree with an axe, and the whole tree tips over, away from you, in about a second. It's drawn with block displays, so players need nothing installed. It cracks as it goes and thuds when it lands, with a puff of wood dust on each log.
- The logs land on the ground as logs, ready to chop again. Set `land-as: items` to drop them as items instead.
- The leaves fall with the tree, and drop what they would have dropped if they'd decayed: saplings, sticks and apples.
- A sapling is planted where the tree stood, free, if one can grow there. Dark and pale oak get four saplings in a square, or none if any of the four spots won't take one, because a single sapling never grows. Spruce and jungle get four where the trunk was 2x2, otherwise one. Mangroves get a propagule on the nearest mud. Huge fungi get a fungus, only on their own nylium.
- Sneak to chop one log the normal way.
- When a real tree won't fall, the action bar says why: it touches a build, it's too big, part of it is in someone's claim, or your axe is too worn. Chopping a log cabin or a log fence says nothing, because those aren't trees.
- The axe wears one point per log, with Unbreaking applied as usual. If the tree has more logs than the axe has uses left, it doesn't fall, and you get one log and a message.
- Every overworld tree falls, including 2x2 spruce and jungle, branching acacia, dark and pale oak, cherry, azalea and mangrove, and so do huge crimson and warped fungi.
- Creative players chop one log, unless `creative-fells` is on.

The owner's decisions behind these rules are in [docs/decisions.md](docs/decisions.md).

## What counts as a natural tree

The plugin only fells a tree it's sure of. When it isn't, the log breaks like vanilla and nothing else moves.

- **Leaves.** A tree needs leaves that grew naturally. The game marks leaves a player places, so a log cabin, a log fence or a "tree" built from placed logs and leaves never falls.
- **Builds.** If any log of the tree touches anything that isn't ground, plants or another tree (planks, glass, a torch, a sign, a fence), nothing falls. That covers a house built around a real tree.
- **Placed logs.** The plugin notes every log a player places from the moment it's installed. A placed log stuck to a natural tree stays where it is, and the rest of the tree falls. More than two placed logs touching it count as a build.
- **Two trees.** Where two trees share leaves, each leaf goes with whichever trunk is nearer, the same way the game decides decay. The other tree keeps its logs and its leaves.
- **Fungi** have no natural-leaves marker, so a huge fungus has to stand on its own nylium.

Limits:

- Logs placed before the plugin was installed, or by commands or WorldEdit, look natural. On their own they still never fall, because they have no natural leaves. Stuck to a real tree, they fall with it.
- Leaves touching a build don't stop a fall. Only logs do. A platform resting on leaves stays up after the leaves are gone.
- Trees bigger than `max-logs` (250) or `max-leaves` (3000) break like vanilla.
- A bee nest on a felled tree stays where it was, in the air, bees and all.

## Where logs land

A log never replaces a block. It comes to rest on the first block below it. If that block has a full top, the log becomes a block. On a torch, slab, sign, flower, crop, chest or water it drops as an item instead, and the block underneath is left alone. Logs that would land in lava or fire, fall into the void, end up buried under a steep slope, or land in a GriefPrevention claim the player can't build in go to the player's inventory, and anything that doesn't fit is dropped at their feet.

The log you chop yourself drops as an item, the same as in vanilla, so lava right beside the trunk can catch it.

Logs never land inside a player, mob or boat, and nobody on or under a falling tree takes fall damage for ten seconds.

## If the server stops mid-fall

Each fall writes a small journal in `plugins/PaulBunyan/falls/` before it touches a block. If the server stops normally, every fall in the air lands at once before the plugin shuts down. After a crash or `kill -9`, the journal is replayed on the next start. Each log is checked against both its old spot and its landing spot, so nothing lands twice. A journal is kept for ten minutes after the fall, longer than the default five-minute autosave, then deleted.

What can still go wrong after a crash, rarely:

- In items mode, and for logs that went to the player, the plugin can't see whether the items survived the crash, so it doesn't redo them. Those can be lost if the crash came within a few minutes of the fall.
- Leaf drops aren't journaled. A crash mid-fall can lose them.

## Install

1. Put `PaulBunyan-<version>.jar` in `plugins/`.
2. Start the server. Settings are in `plugins/PaulBunyan/config.yml`.

GriefPrevention and CoreProtect are used if they're installed. Neither is needed.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `land-as` | `blocks` | `blocks` to land logs as blocks, `items` to drop them. |
| `max-logs` | `250` | Bigger trees break like vanilla. |
| `max-leaves` | `3000` | Same, for leaves. |
| `replant` | `true` | Plant a sapling where the tree stood. |
| `nether-fungi` | `true` | Fell huge crimson and warped fungi too. |
| `creative-fells` | `false` | Let creative players fell trees. |
| `fall-ticks` | `24` | How long the fall takes, in ticks. `0` lands at once with no animation. |
| `max-animated-blocks` | `600` | Above this many logs and leaves, the leaves vanish instead of falling with the tree. |
| `disabled-worlds` | `[]` | Worlds where trees never fall, by folder name. |

## Commands

| Command | What it does |
| --- | --- |
| `/paulbunyan check <x y z> [world]` | Says whether the log there would fall, and if not, why, naming the block that stopped it. Works from the console. |
| `/paulbunyan reload` | Reloads the settings. |

`/pb` is short for `/paulbunyan`.

## Permissions

| Permission | Default | Allows |
| --- | --- | --- |
| `paulbunyan.fell` | everyone | Felling trees. Without it, logs break like vanilla. |
| `paulbunyan.admin` | ops | `/paulbunyan check` and `reload`. |

## Building

```sh
./gradlew build
```

The jar is `build/libs/PaulBunyan-<version>.jar`. Pushing a `v*` tag builds it and attaches it to a GitHub Release.

## Testing

TESTING_SECTION

## License

MIT. See [LICENSE](LICENSE).
