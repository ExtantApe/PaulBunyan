# PaulBunyan

A Paper plugin that makes trees fall. Chop a natural tree with an axe and it tips over, the logs land on the ground, and a sapling goes back in where it stood.

**Status:** pre-release. It has passed a bot test suite on throwaway Paper 26.1.2, 26.2 and 26.3 servers, and 25 unit tests. It hasn't been seen in a real game client yet, so how the fall looks, sounds and feels, and whether older clients connected through ViaVersion show it properly, are untested. See [Testing](#testing).

- Requires Paper 26.1 or newer, on Java 25. Tested on 26.1.2, 26.2 and 26.3. Java Edition only.
- Replaces tree-felling plugins such as TreeAssist and LumberJack.

## What it does

- Chop any log of a natural tree with an axe. Everything below your cut stays standing as a stump, and the rest creaks for a second, then tips over toward its heaviest side, or away from you if it stands straight. It's drawn with block displays, so players need nothing installed. It cracks as it goes and thuds when it lands, with a puff of wood dust on each log.
- The logs land on the ground as logs, ready to chop again. Set `land-as: items` to drop them as items instead.
- The leaves fall with the tree, and drop what they would have dropped if they'd decayed: saplings, sticks and apples.
- A sapling is planted where the tree stood, free, if one can grow there. Dark and pale oak get four saplings in a square, or none if any of the four spots won't take one, because a single sapling never grows. Spruce and jungle get four where the trunk was 2x2, otherwise one. Mangroves get a propagule on the nearest mud or dirt within ten blocks of the trunk, since their roots cover the ground right around it. Huge fungi get a fungus, only on their own nylium.
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

## Getting hit

`fall-danger` works like difficulty. At `off`, the default, nobody is ever hurt: anyone right by the trunk is knocked back as it goes, and anyone the tree lands on is flung clear, harder the bigger the hit. At `max`, falling leaves and wood hurt, a log on the head takes 12 health (six hearts), and a trunk landing on someone kills them. If LanternGraves is installed, their things go in a grave as with any death. Set a level per world with `fall-danger-worlds`. Players in creative or spectator are never hurt.

A tree cut above the ground leaves its stump standing, and no sapling is planted. Stumps don't regrow yet.

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

GriefPrevention, CoreProtect and Prism (4.4 or newer) are used if they're installed. None is needed. Felled logs are recorded in CoreProtect and Prism under the player who chopped.

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

`./gradlew build` runs the unit tests: the tree scanner, the fall plan and the landing rules, against a made-up world.

`test/suite.mjs` builds a throwaway Paper server on a flat world on 127.0.0.1:25612, grows trees with `/place feature` (the way world generation does), and drives it with [mineflayer](https://github.com/PrismarineJS/mineflayer) bots. It checks:

- every species, including 2x2 spruce, pine and jungle, branching acacia, dark and pale oak, cherry and azalea, plus mangroves in mud and water, and both huge fungi
- that every log ends up somewhere: as blocks, as items or in a bot's inventory, adding up to the tree's log count
- log cabins, log fences, placed logs with placed leaves, and a house around a real tree (none of them fall)
- a torch on a trunk, two trees sharing leaves, and a stray placed log
- landings on torches, slabs, signs, flowers, crops, chests (contents checked), water, lava, a cliff and the void, in both modes
- GriefPrevention claims, a bot under the fall, a bot on top of the tree, and a boat
- replanting on dirt, grass, mud, moss and stone, a blocked spot, and 2x2 trees with one bad corner
- a chunk unloading mid-fall, a normal restart, and `kill -9` before and after a save
- felling from the cut: a chop above the base leaves a stump and plants nothing, a ground chop replants, trees lean toward their heavy side, the fall starts after the warning, and fall-danger off, max, per world and in creative
- Prism lookups of a fall (26.3 only, written but not yet run)
- the size cap, ten bots felling at once (average tick under 50 ms), CoreProtect lookups, creative mode, sneaking and axe wear

```sh
./gradlew clean build
cd test && npm install && cd ..
node test/fetch-jars.mjs /tmp/pb-jars
PB_TEST_WORK=/tmp/pb-work PB_TEST_JARS=/tmp/pb-jars PB_TEST_JAVA=<path to Java 25> node test/suite.mjs 26.3
```

`fetch-jars.mjs` downloads Paper, ViaVersion, ViaBackwards, GriefPrevention, CoreProtect and Prism and checks each against the hash its publisher lists. The suite writes `eula=true` into the server it creates, which accepts Mojang's EULA for it, so only run it if you accept that. It deletes the server when it's done.

What the bots can't show:

- The bots speak 1.21.11 through ViaVersion and ViaBackwards. They dig, place, pick up and take damage like players, but they draw nothing.
- On 26.3 a 1.21.11 bot is kicked if it sends movement, so there the bots hover where `/tp` puts them. The checks that need a bot to fall or place blocks (a bot on top of the tree, placed logs with placed leaves, and a stray placed log) run on 26.1.2 and 26.2 only.
- CoreProtect 24.1 refuses to start on 26.3, so block logging was tested on 26.1.2 and 26.2 only.
- "Player-placed leaves" are set from the console with `persistent=true`, which is exactly what the game stores when a player places leaves.

## License

MIT. See [LICENSE](LICENSE).
