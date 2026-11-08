# Paper 26.x audit

Paul Bunyan targets Paper 26.1 and up, on Java 25, and is compiled against the 26.1.2 API. It was written
for 26.x from the start, so there was nothing older to remove. This note records which modern APIs it uses
and which older ones it keeps, and why. Each was checked against the 26.1.2 API jar, not assumed.

## Uses

- **`paper-plugin.yml`** instead of `plugin.yml`. GriefPrevention and CoreProtect are optional
  dependencies there, loaded first when present.
- **Brigadier commands** registered through `LifecycleEvents.COMMANDS`, with Paper's `blockPosition` and
  `world` argument types, so `/paulbunyan check` gets tab completion and works from the console.
- **Adventure components** for every message, including the action bar.
- **Data components** (`MAX_DAMAGE`, `DAMAGE`, `UNBREAKABLE`) to work out how much wear an axe can take,
  instead of `ItemMeta` and `Damageable`. Wear itself goes through `damageItemStack`, so Unbreaking and
  the item damage event apply as they do in vanilla.
- **Block displays** with client-side interpolation for the fall. They're never saved with the world,
  so a crash can't leave one behind.
- **The chunk's persistent data container** to remember logs that players place. That's saved with the
  chunk itself, with no separate database.
- **Java 25:** records, pattern matching for `instanceof`, switch expressions and `List.getFirst`.

## Keeps, and why

- **`Material` and `Tag`** for reading blocks. `Block#getType` still returns `Material` in 26.1.2, which
  isn't deprecated there. Going through `BlockType` would add a conversion at every block read and gain
  nothing.
- **The Bukkit scheduler and events.** The plugin doesn't target Folia, and on Paper these are current.
- **Reflection for GriefPrevention and CoreProtect** instead of compiling against them. Both are optional,
  and reflection keeps the plugin from being tied to one version of either. If GriefPrevention is
  installed but can't be asked, no tree falls.

## Doesn't use

- **Dialogs.** The plugin never needs to ask a player anything.
- **The registry API.** It doesn't add or look up any registry entries.
