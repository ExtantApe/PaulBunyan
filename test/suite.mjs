// PaulBunyan acceptance suite. Usage: node suite.mjs <26.1.2|26.2|26.3> [only=section,section]
// Builds a throwaway server on a flat world from downloaded jars and drives it with bots. Trees are grown
// with /place feature, which builds them the way world generation does: natural logs and natural leaves.
//
// What's real and what isn't:
// - Bots speak 1.21.11 through ViaVersion and ViaBackwards. They dig, place blocks and pick up items like
//   players, but they don't draw anything: how the fall looks is untested here.
// - On 26.3 the bots can't send movement (see harness.mjs), so they hover where /tp puts them, and the
//   "bot on top of the tree" check, which needs a bot to fall, runs on 26.1.2 and 26.2 only.
// - "Player-placed leaves" are set with persistent=true from the console, which is exactly what the game
//   stores when a player places leaves. Player-placed logs are placed by a bot.
import { existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { Vec3 } from 'vec3'
import { blocksNear, bot, check, DEFAULT_CONFIG, freshServer, inv, itemsNear, jar, quit, section, Server, sleep, summary, until } from './harness.mjs'

const MC = process.argv[2]
if (!['26.1.2', '26.2', '26.3'].includes(MC)) throw new Error('usage: node suite.mjs <26.1.2|26.2|26.3> [only=a,b]')
const ONLY = (process.argv.find(a => a.startsWith('only=')) || '').slice(5).split(',').filter(Boolean)
const want = n => ONLY.length === 0 || ONLY.includes(n)
const PHYSICS = MC !== '26.3'
const Y = -60 // the flat world's grass is at -61

const prismJar = process.env.PB_TEST_JARS && path.join(process.env.PB_TEST_JARS, 'Prism.jar')
const prism = MC === '26.3' && existsSync(prismJar) // fetch-jars.mjs gets the Prism build listed for 26.3
const dir = freshServer('suite-' + MC, { paper: jar(`paper-${MC}.jar`),
  plugins: [jar('ViaVersion.jar'), jar('ViaBackwards.jar'), jar('GriefPrevention.jar'), jar('CoreProtect.jar'), ...(prism ? [prismJar] : [])] })
const s = new Server({ dir })
const cfgFile = path.join(dir, 'plugins/PaulBunyan/config.yml')
const baseConfig = readFileSync(DEFAULT_CONFIG, 'utf8')
async function config(changes = {}) {
  let text = baseConfig
  for (const [k, v] of Object.entries(changes)) {
    const re = new RegExp(`^${k}: .*$`, 'm')
    if (!re.test(text)) throw new Error('no setting ' + k)
    text = text.replace(re, `${k}: ${v}`)
  }
  writeFileSync(cfgFile, text)
  const out = (await s.ask('paulbunyan reload', 800)).join(' ')
  if (!/reloaded/.test(out)) throw new Error('reload failed: ' + out)
}

console.log(`PaulBunyan suite on Paper ${MC}, bots ${PHYSICS ? 'with' : 'without'} physics, in ${dir}`)
await s.start()
check('PaulBunyan enabled', s.log.some(l => /Enabling PaulBunyan/.test(l)))
check('GriefPrevention enabled', s.log.some(l => /Enabling GriefPrevention/.test(l)))
const coreProtect = s.log.some(l => /Enabling CoreProtect/.test(l)) && !s.log.some(l => /CoreProtect.*(not compatible|disabl|Error occurred)/i.test(l))
console.log(`  CoreProtect ${coreProtect ? 'enabled' : 'did not enable on this version'}`)
for (const g of ['randomTickSpeed 0', 'random_tick_speed 0', 'doDaylightCycle false', 'advance_time false', 'doMobSpawning false', 'spawn_mobs false'])
  await s.ask('gamerule ' + g, 150)
await config()

let me = await bot('Chopper', { physics: PHYSICS })
await s.ask('gamemode survival Chopper')

// ---------- helpers ----------
let plotNo = 0
/** A fresh plot of land far from the others, with the bot standing just west of its centre, facing east. */
async function plot({ standY = Y, back = 2 } = {}) {
  const X = 2000 + 160 * plotNo++, Z = 2000
  await s.ask(`tp Chopper ${X - back + 0.5} ${standY} ${Z + 0.5} -90 10`, 300)
  await until(() => me.entity.position.distanceTo(new Vec3(X, standY, Z)) < 8, 10000)
  await s.ask(`forceload add ${X - 24} ${Z - 24} ${X + 40} ${Z + 24}`, 500)
  await until(async () => (await s.ask(`execute if loaded ${X} ${Y} ${Z}`, 200)).join(' ').includes('passed'), 20000, 300)
  await until(() => me.blockAt(new Vec3(X, Y - 1, Z)) !== null, 10000)
  await sleep(500)
  await s.ask(`kill @e[type=item,x=${X},y=${Y},z=${Z},distance=..80]`, 200)
  await s.ask('clear Chopper', 200)
  return { X, Z, c: new Vec3(X, Y, Z) }
}
async function release(p) { await s.ask(`forceload remove ${p.X - 24} ${p.Z - 24} ${p.X + 40} ${p.Z + 24}`, 200) }
async function axe(b = me, name = 'Chopper', kind = 'diamond_axe') {
  await s.ask(`give ${name} minecraft:${kind}`, 300)
  await until(() => b.inventory.items().some(i => i.name === kind), 5000)
  await b.equip(b.inventory.items().find(i => i.name === kind), 'hand')
}
/** The lowest log within two blocks across of a point, as the bot sees it. */
function lowestLog(b, X, Z, name) {
  let best = null
  for (let y = Y - 2; y < Y + 12 && !best; y++)
    for (let dx = -2; dx <= 2 && !best; dx++)
      for (let dz = -2; dz <= 2 && !best; dz++) {
        const bl = b.blockAt(new Vec3(X + dx, y, Z + dz))
        if (bl && bl.name === name) best = bl
      }
  return best
}
async function chop(b, block) {
  if (b.physicsOn) await b.lookAt(block.position.offset(0.5, 0.5, 0.5), true)
  await b.dig(block, 'ignore')
}
/** Every log of a kind, wherever it is: as blocks near the plot, as items near it, and in the bots' pockets. */
function account(p, name, bots = [me]) {
  const blocks = blocksNear(me, name, p.c, 44).length
  const items = itemsNear(me, p.c, 60)[name] || 0
  const held = bots.reduce((n, b) => n + (inv(b)[name] || 0), 0)
  return { blocks, items, held, total: blocks + items + held }
}
const waitFall = (ticks = 24) => sleep(ticks * 50 + 2500)
/**
 * The account once the bots' view has caught up: picked-up items and merged stacks can briefly show
 * twice, so read until two readings a second apart agree. Real duplicates would persist.
 */
async function settledAccount(p, name, bots = [me]) {
  let last = JSON.stringify(account(p, name, bots))
  for (let i = 0; i < 10; i++) {
    await sleep(1000)
    const now = JSON.stringify(account(p, name, bots))
    if (now === last) return JSON.parse(now)
    last = now
  }
  return JSON.parse(last)
}
async function displays() { return s.count('@e[type=block_display]') }

/** Grows a tree with /place feature and checks the plugin agrees it's natural. */
async function grow(p, feature, log, { at = [p.X, Y, p.Z], before } = {}) {
  if (before) await before()
  // a tree feature sometimes fails at random, as it does in world generation: try a few times
  let out = ''
  for (let i = 0; i < 4 && !/Placed "?(feature|minecraft)/.test(out); i++) out = (await s.ask(`place feature minecraft:${feature} ${at.join(' ')}`, 1200)).join(' ')
  if (!/Placed "?(feature|minecraft)/.test(out)) return { ok: false, why: out.slice(0, 200) }
  await sleep(600)
  const base = lowestLog(me, at[0], at[2], log)
  if (!base) return { ok: false, why: 'no ' + log + ' visible' }
  const t = await s.treeCheck(base.position.x, base.position.y, base.position.z)
  return { ok: true, base, t }
}

/** Fells a tree and checks the essentials: everything accounted for, the stump empty, no displays left. */
async function fellAndAccount(p, base, log, t, label, { ticks = 24, bots = [me] } = {}) {
  const origin = blocksNear(me, log, p.c, 44).map(v => v.toString())
  await chop(me, base)
  await waitFall(ticks)
  const a = await settledAccount(p, log, bots)
  check(`${label}: every log is accounted for (${t.logs})`, a.total === t.logs, JSON.stringify(a))
  // A tree falls toward its heavy side now, so a branch of a fallen cherry can land on a spot another log stood on.
  // What can't happen is a log still standing over the trunk's own footprint: nothing lands there.
  const lowest = Math.min(...origin.map(v => +v.match(/-?\d+/g)[1]))
  const footprint = new Set(origin.filter(v => +v.match(/-?\d+/g)[1] === lowest).map(v => { const n = v.match(/-?\d+/g); return n[0] + ',' + n[2] }))
  const still = blocksNear(me, log, p.c, 44).filter(v => footprint.has(v.x + ',' + v.z) && origin.includes(v.toString())).map(v => v.toString())
  check(`${label}: nothing is left standing over the trunk`, still.length === 0, still.join(' '))
  check(`${label}: no block displays left behind`, (await displays()) === 0)
  return a
}

// ---------- every species ----------
const SPECIES = ONLY.includes('giant') ? [['mega_spruce', 'spruce_log', 'spruce_sapling', 4], ['mega_pine', 'spruce_log', 'spruce_sapling', 4]] : [
  // feature, log, sapling, expected saplings
  ['oak', 'oak_log', 'oak_sapling', 1], ['fancy_oak', 'oak_log', 'oak_sapling', 1], ['birch', 'birch_log', 'birch_sapling', 1],
  ['spruce', 'spruce_log', 'spruce_sapling', 1], ['pine', 'spruce_log', 'spruce_sapling', 1],
  ['mega_spruce', 'spruce_log', 'spruce_sapling', 4], ['mega_pine', 'spruce_log', 'spruce_sapling', 4],
  ['jungle_tree', 'jungle_log', 'jungle_sapling', 1], ['mega_jungle_tree', 'jungle_log', 'jungle_sapling', 4],
  ['acacia', 'acacia_log', 'acacia_sapling', 1], ['dark_oak', 'dark_oak_log', 'dark_oak_sapling', 4],
  ['cherry', 'cherry_log', 'cherry_sapling', 1], ['pale_oak', 'pale_oak_log', 'pale_oak_sapling', 4],
  ['azalea_tree', 'oak_log', 'azalea', 1],
]
if (want('species') || want('giant')) {
  section('Every species falls, lands and replants')
  for (const [feature, log, sapling, saplings] of SPECIES) {
    const p = await plot()
    // azalea trees grow on rooted dirt; the rest on the flat world's grass
    const g = await grow(p, feature, log)
    if (!check(`${feature}: grows and is seen as natural`, g.ok && g.t.falls, g.ok ? JSON.stringify(g.t) : g.why)) { await release(p); continue }
    await axe()
    await fellAndAccount(p, g.base, log, g.t, feature)
    const planted = blocksNear(me, sapling, p.c, 6).length
    check(`${feature}: ${saplings} ${sapling} replanted`, planted === saplings, `found ${planted}`)
    await release(p)
  }
}

// ---------- mangroves ----------
if (want('mangrove')) {
  section('Mangroves: roots, propagules, moss, mud and water')
  for (const feature of ['mangrove', 'tall_mangrove']) {
    const p = await plot()
    await s.ask(`fill ${p.X - 10} ${Y - 2} ${p.Z - 10} ${p.X + 24} ${Y - 1} ${p.Z + 10} minecraft:mud`, 400)
    await s.ask(`fill ${p.X - 10} ${Y} ${p.Z - 10} ${p.X + 24} ${Y} ${p.Z + 10} minecraft:water`, 400)
    const g = await grow(p, feature, 'mangrove_log', { at: [p.X, Y, p.Z] })
    if (!check(`${feature}: grows in water and is seen as natural`, g.ok && g.t.falls, g.ok ? JSON.stringify(g.t) : g.why)) { await release(p); continue }
    const roots = blocksNear(me, 'mangrove_roots', p.c, 10).length + blocksNear(me, 'muddy_mangrove_roots', p.c, 10).length
    await s.ask(`tp Chopper ${g.base.position.x - 2 + 0.5} ${g.base.position.y} ${p.Z + 0.5} -90 10`, 600)
    await axe()
    await fellAndAccount(p, g.base, 'mangrove_log', g.t, feature)
    const rootsAfter = blocksNear(me, 'mangrove_roots', p.c, 10).length + blocksNear(me, 'muddy_mangrove_roots', p.c, 10).length
    check(`${feature}: the roots stay where they are`, rootsAfter === roots, `${roots} before, ${rootsAfter} after`)
    // the tree's own hanging propagules are left where they hang, so only the standing ones count as the replanted one
    const prop = blocksNear(me, 'mangrove_propagule', p.c, 16).filter(v => me.blockAt(v)?.getProperties().hanging !== true)
    const sensible = prop.length === 1 && ['mud', 'muddy_mangrove_roots', 'dirt', 'grass_block', 'moss_block', 'clay'].includes(me.blockAt(prop[0].offset(0, -1, 0))?.name)
    check(`${feature}: one propagule replanted near the trunk, on mud`, sensible,
      prop.map(v => v + ' on ' + me.blockAt(v.offset(0, -1, 0))?.name + ', ' + v.distanceTo(p.c).toFixed(1) + ' from the trunk').join('; ') || 'none within 16')
    await release(p)
  }
}

// ---------- Nether fungi ----------
if (want('nether')) {
  section('Huge fungi fall and replant on nylium')
  for (const [feature, nylium, stem, fungus] of [['crimson_fungus_planted', 'crimson_nylium', 'crimson_stem', 'crimson_fungus'],
    ['warped_fungus_planted', 'warped_nylium', 'warped_stem', 'warped_fungus']]) {
    const p = await plot()
    await s.ask(`fill ${p.X - 4} ${Y - 1} ${p.Z - 4} ${p.X + 4} ${Y - 1} ${p.Z + 4} minecraft:${nylium}`, 300)
    const g = await grow(p, feature, stem)
    if (!check(`${feature}: grows and is seen as natural`, g.ok && g.t.falls, g.ok ? JSON.stringify(g.t) : g.why)) { await release(p); continue }
    await axe()
    await fellAndAccount(p, g.base, stem, g.t, feature)
    check(`${feature}: a ${fungus} replanted on nylium`, blocksNear(me, fungus, p.c, 3).length === 1)
    await release(p)
  }
  const p = await plot()
  const g = await grow(p, 'crimson_fungus_planted', 'crimson_stem', { before: () => s.ask(`setblock ${p.X} ${Y - 1} ${p.Z} minecraft:crimson_nylium`, 200) })
  await s.ask(`setblock ${p.X} ${Y - 1} ${p.Z} minecraft:dirt`, 200)
  if (g.ok) check('a fungus not standing on nylium is left alone', !(await s.treeCheck(g.base.position.x, g.base.position.y, g.base.position.z)).falls)
  await release(p)
}

// ---------- not natural ----------
if (want('notnatural')) {
  section('Builds never fall')
  {
    const p = await plot()
    // a hollow log cabin, built from the console, so the plugin has no record of it being placed
    await s.ask(`fill ${p.X} ${Y} ${p.Z - 3} ${p.X + 6} ${Y + 3} ${p.Z + 3} minecraft:spruce_log hollow`, 400)
    await s.ask(`fill ${p.X} ${Y + 4} ${p.Z - 3} ${p.X + 6} ${Y + 4} ${p.Z + 3} minecraft:oak_leaves[persistent=true]`, 400)
    const t = await s.treeCheck(p.X, Y, p.Z)
    check('log cabin: seen as a build', !t.falls, JSON.stringify(t))
    const before = blocksNear(me, 'spruce_log', p.c, 20).length
    await axe(); await chop(me, me.blockAt(new Vec3(p.X, Y, p.Z))); await waitFall()
    check('log cabin: only the chopped log goes', blocksNear(me, 'spruce_log', p.c, 20).length === before - 1)
    await release(p)
  }
  {
    const p = await plot()
    await s.ask(`fill ${p.X} ${Y} ${p.Z} ${p.X + 10} ${Y} ${p.Z} minecraft:oak_log`, 300)
    await s.ask(`fill ${p.X} ${Y + 1} ${p.Z} ${p.X + 10} ${Y + 1} ${p.Z} minecraft:oak_fence`, 300)
    check('log fence: seen as a build', !(await s.treeCheck(p.X, Y, p.Z)).falls)
    const before = blocksNear(me, 'oak_log', p.c, 20).length
    await axe(); await chop(me, me.blockAt(new Vec3(p.X, Y, p.Z))); await waitFall()
    check('log fence: only the chopped log goes', blocksNear(me, 'oak_log', p.c, 20).length === before - 1)
    await release(p)
  }
  if (!PHYSICS) console.log('  skip placed logs with placed leaves: on 26.3 the server refuses blocks from a bot that can\'t move (see harness.mjs)')
  else {
    const p = await plot()
    // a "tree" a player built: logs placed by the bot, leaves with the persistent flag players' leaves get
    await s.ask(`give Chopper minecraft:birch_log 4`, 400)
    await until(() => me.inventory.items().some(i => i.name === 'birch_log'), 5000)
    await me.equip(me.inventory.items().find(i => i.name === 'birch_log'), 'hand')
    for (let y = Y; y < Y + 3; y++) await me.placeBlock(me.blockAt(new Vec3(p.X, y - 1, p.Z)), new Vec3(0, 1, 0)).catch(e => console.log('    place: ' + e.message))
    await sleep(500)
    const placed = blocksNear(me, 'birch_log', p.c, 6).length
    await s.ask(`fill ${p.X - 2} ${Y + 3} ${p.Z - 2} ${p.X + 2} ${Y + 4} ${p.Z + 2} minecraft:birch_leaves[persistent=true]`, 300)
    check('placed logs with placed leaves: bot placed 3 logs', placed === 3, 'placed ' + placed)
    check('placed logs with placed leaves: seen as a build', !(await s.treeCheck(p.X, Y, p.Z)).falls)
    await axe(); await chop(me, me.blockAt(new Vec3(p.X, Y + 1, p.Z))); await waitFall()
    check('placed logs with placed leaves: only the chopped log goes', blocksNear(me, 'birch_log', p.c, 10).length === placed - 1)
    await release(p)
  }
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    // a little house built around the trunk: plank walls and a roof the trunk passes through
    for (const [a, b] of [[[-3, -3], [3, -3]], [[-3, 3], [3, 3]], [[-3, -3], [-3, 3]], [[3, -3], [3, 3]]])
      await s.ask(`fill ${p.X + a[0]} ${Y} ${p.Z + a[1]} ${p.X + b[0]} ${Y} ${p.Z + b[1]} minecraft:oak_planks`, 200)
    // a low roof the trunk passes through
    await s.ask(`fill ${p.X - 3} ${Y + 1} ${p.Z - 3} ${p.X + 3} ${Y + 1} ${p.Z + 3} minecraft:oak_planks replace minecraft:air`, 300)
    // and right against the trunk on every side, even where a short oak's leaves hang that low
    for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) await s.ask(`setblock ${p.X + dx} ${Y + 1} ${p.Z + dz} minecraft:oak_planks`, 100)
    const t = await s.treeCheck(p.X, Y, p.Z)
    check('house built around a real tree: left alone', !t.falls, JSON.stringify(t))
    await s.ask(`tp Chopper ${p.X - 1.5} ${Y} ${p.Z + 0.5} -90 10`, 500)
    const planks = blocksNear(me, 'oak_planks', p.c, 10).length
    await axe(); await chop(me, me.blockAt(new Vec3(p.X, Y, p.Z))); await waitFall()
    check('house built around a real tree: the house is untouched', blocksNear(me, 'oak_planks', p.c, 10).length === planks)
    check('house built around a real tree: only the chopped log goes', blocksNear(me, 'oak_log', p.c, 10).length === (g.t?.logs ?? 0) - 1,
      `${blocksNear(me, 'oak_log', p.c, 10).length} left of ${g.t?.logs}`)
    await release(p)
  }
}

// ---------- doubtful ----------
if (want('doubtful')) {
  section('Doubtful trees: the owner chose "fell the sure part", but nothing touching a build')
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await s.ask(`setblock ${p.X + 1} ${Y + 1} ${p.Z} minecraft:wall_torch[facing=east]`, 200)
    check('tree with a torch on it: left alone', !(await s.treeCheck(p.X, Y, p.Z)).falls)
    await axe(); await chop(me, g.base); await waitFall()
    check('tree with a torch on it: only the chopped log goes', blocksNear(me, 'oak_log', p.c, 10).length === g.t.logs - 1)
    check('tree with a torch on it: the torch is still there', blocksNear(me, 'wall_torch', p.c, 4).length === 1)
    await release(p)
  }
  {
    const p = await plot()
    const g1 = await grow(p, 'oak', 'oak_log')
    await s.ask(`place feature minecraft:oak ${p.X} ${Y} ${p.Z + 4}`, 1000)
    await sleep(500)
    const other = blocksNear(me, 'oak_log', new Vec3(p.X, Y, p.Z + 4), 1.5).length
    const t2 = await s.treeCheck(p.X, Y, p.Z + 4)
    const t1 = await s.treeCheck(p.X, Y, p.Z)
    check('two trees sharing leaves: each is its own tree', t1.falls && t2.falls && t1.logs + t2.logs === blocksNear(me, 'oak_log', p.c, 12).length, JSON.stringify([t1, t2]))
    const otherLogs = blocksNear(me, 'oak_log', p.c, 12).filter(v => v.z >= p.Z + 3).map(String)
    await axe(); await chop(me, g1.base); await waitFall()
    const stillOther = blocksNear(me, 'oak_log', p.c, 12).filter(v => otherLogs.includes(v.toString())).length
    check('two trees sharing leaves: the other tree keeps every log', stillOther === otherLogs.length, `${stillOther}/${otherLogs.length}`)
    const t2after = await s.treeCheck(p.X, Y, p.Z + 4)
    check('two trees sharing leaves: the other tree keeps its leaves', t2after.falls && t2after.leaves >= t2.leaves - 2, `${t2.leaves} before, ${t2after.leaves} after`)
    check('two trees sharing leaves: the chopped tree fell', blocksNear(me, 'oak_log', p.c, 1.5).length === 0 && other > 0)
    await release(p)
  }
  if (!PHYSICS) console.log('  skip tree with a stray placed log: on 26.3 the server refuses blocks from a bot that can\'t move')
  else {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    // the bot places a log against the trunk, on the side away from the fall
    await s.ask('give Chopper minecraft:oak_log 1', 400)
    await until(() => me.inventory.items().some(i => i.name === 'oak_log'), 5000)
    await me.equip(me.inventory.items().find(i => i.name === 'oak_log'), 'hand')
    await me.placeBlock(me.blockAt(new Vec3(p.X, Y, p.Z)), new Vec3(0, 0, -1)).catch(e => console.log('    place: ' + e.message))
    await sleep(500)
    const stray = me.blockAt(new Vec3(p.X, Y, p.Z - 1))
    const t = await s.treeCheck(p.X, Y, p.Z)
    check('tree with a stray placed log: the natural part falls', t.falls && t.strays === 1, JSON.stringify(t))
    await axe(); await chop(me, g.base); await waitFall()
    check('tree with a stray placed log: the stray stays put', me.blockAt(new Vec3(p.X, Y, p.Z - 1))?.name === 'oak_log' && stray?.name === 'oak_log')
    check('tree with a stray placed log: the trunk is gone', me.blockAt(new Vec3(p.X, Y + 2, p.Z))?.name !== 'oak_log')
    await release(p)
  }
}

// ---------- landing ----------
const LANDINGS = [
  ['torches', x => `setblock ${x} ${Y} ZZ minecraft:torch`, 'torch'],
  ['slabs', x => `setblock ${x} ${Y} ZZ minecraft:stone_slab`, 'stone_slab'],
  ['signs', x => `setblock ${x} ${Y} ZZ minecraft:oak_sign`, 'oak_sign'],
  ['flowers', x => `setblock ${x} ${Y} ZZ minecraft:poppy`, 'poppy'],
  ['crops', x => `setblock ${x} ${Y - 1} ZZ minecraft:farmland\nsetblock ${x} ${Y} ZZ minecraft:wheat[age=7]`, 'wheat'],
  ['chests', x => `setblock ${x} ${Y} ZZ minecraft:chest{Items:[{Slot:0b,id:"minecraft:diamond",count:3}]}`, 'chest'],
  ['water', x => `fill ${x} ${Y - 2} ZZ-1 ${x} ${Y} ZZ+1 minecraft:water`, null],
  // sunk to ground level, so it can't flow back over the grass to the stump
  ['lava', x => `fill ${x} ${Y - 2} ZZ-1 ${x} ${Y - 1} ZZ+1 minecraft:lava`, null],
  ['a cliff', x => `fill ${x} ${Y - 20} ZZ-1 ${x} ${Y - 1} ZZ+1 minecraft:air`, null],
  ['the void', x => `fill ${x} -64 ZZ-1 ${x} ${Y - 1} ZZ+1 minecraft:air`, null],
]
async function landingCase(name, make, keep, itemsMode) {
  const p = await plot()
  const g = await grow(p, 'jungle_tree', 'jungle_log')
  if (!g.ok || !g.t.falls) return check(`${name}: tree grew`, false, g.why ?? JSON.stringify(g.t))
  // from two blocks out: an obstacle right against the trunk would make it a tree touching a build
  for (let x = p.X + 2; x <= p.X + 16; x++) for (const line of make(x).replaceAll('ZZ+1', p.Z + 1).replaceAll('ZZ-1', p.Z - 1).replaceAll('ZZ', p.Z).split('\n')) await s.ask(line, 60)
  await sleep(800)
  const kept = keep ? blocksNear(me, keep, p.c, 30).length : 0
  await axe()
  const a = await fellAndAccount(p, g.base, 'jungle_log', g.t, `${itemsMode ? 'items mode, ' : ''}landing on ${name}`)
  if (keep) check(`${itemsMode ? 'items mode, ' : ''}landing on ${name}: none replaced`, blocksNear(me, keep, p.c, 30).length === kept, `${kept} before, ${blocksNear(me, keep, p.c, 30).length} after`)
  if (keep === 'chest') {
    const out = (await s.ask(`data get block ${p.X + 3} ${Y} ${p.Z} Items`, 400)).join(' ')
    check(`${itemsMode ? 'items mode, ' : ''}landing on chests: contents untouched`, /diamond.*count: 3|count: 3.*diamond/.test(out), out.slice(0, 200))
  }
  if (itemsMode) check(`items mode, landing on ${name}: no log blocks placed`, a.blocks === 0, JSON.stringify(a))
  await release(p)
}
if (want('landing')) {
  section('Where logs land')
  for (const [name, make, keep] of LANDINGS) await landingCase(name, make, keep, false)
}
if (want('items')) {
  section('Items mode')
  await config({ 'land-as': 'items' })
  for (const [name, make, keep] of LANDINGS) await landingCase(name, make, keep, true)
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await axe()
    const a = await fellAndAccount(p, g.base, 'oak_log', g.t, 'items mode, open ground')
    check('items mode, open ground: all logs are items', a.blocks === 0 && a.items + a.held === g.t.logs, JSON.stringify(a))
    await release(p)
  }
  await config()
}

// ---------- protection ----------
if (want('protection')) {
  section('GriefPrevention claims')
  const owner = await bot('Owner', { physics: PHYSICS })
  const p = await plot()
  await s.ask(`tp Owner ${p.X + 8.5} ${Y} ${p.Z + 0.5}`, 800)
  await s.ask('acb Owner 20000', 400)
  await s.ask('give Owner minecraft:golden_shovel', 400) // GriefPrevention's /claim wants one in hand
  await until(() => owner.inventory.items().some(i => i.name === 'golden_shovel'), 5000)
  await owner.equip(owner.inventory.items().find(i => i.name === 'golden_shovel'), 'hand')
  owner.chat('/claim 5')
  await sleep(1500)
  const claimed = owner.chatLog.join(' ')
  check('the owner bot made a claim', /claim|Claim/.test(claimed), claimed.slice(-300))
  // a tree inside the claim
  const inside = await grow(p, 'oak', 'oak_log', { at: [p.X + 8, Y, p.Z + 3] })
  await s.ask(`tp Chopper ${p.X + 6.5} ${Y} ${p.Z + 3.5} -90 10`, 600)
  const insideAt = new Vec3(p.X + 8, Y, p.Z + 3)
  const insideBefore = blocksNear(me, 'oak_log', insideAt, 8).length
  await axe()
  await chop(me, inside.base).catch(() => {})
  await waitFall()
  check('a player without rights can\'t fell a tree in a claim', insideBefore === inside.t.logs && blocksNear(me, 'oak_log', insideAt, 8).length === insideBefore,
    `${insideBefore} logs before, ${blocksNear(me, 'oak_log', insideAt, 8).length} after`)
  // a tree outside the claim, falling east into it
  // four blocks from the claim's west edge (x = X+3): its leaves stay outside (a tree with any part in
  // the claim doesn't fall at all), but its trunk lands across the edge
  const outside = await grow(p, 'jungle_tree', 'jungle_log', { at: [p.X - 1, Y, p.Z] })
  await s.ask(`tp Chopper ${p.X - 3 + 0.5} ${Y} ${p.Z + 0.5} -90 10`, 600)
  await s.ask('clear Chopper', 200); await axe()
  check('the outside tree falls', outside.ok && outside.t.falls)
  await chop(me, outside.base); await waitFall()
  const inClaim = blocksNear(me, 'jungle_log', p.c, 44).filter(v => v.x >= p.X + 3 && v.x <= p.X + 13 && v.z >= p.Z - 5 && v.z <= p.Z + 5)
  check('a fall never lands blocks in a claim the player can\'t build in', inClaim.length === 0, inClaim.join(' '))
  const a = await settledAccount({ c: new Vec3(p.X - 1, Y, p.Z) }, 'jungle_log')
  // the log h above the stump lands at x = X+h, so those from h = 3 up are in the claim
  const intoClaim = Math.max(0, (outside.t?.logs ?? 0) - 3)
  check(`logs that would land in the claim go to the player (${intoClaim})`, a.total === outside.t.logs && a.held >= intoClaim && intoClaim > 0,
    JSON.stringify(a) + ' of ' + outside.t?.logs)
  await quit(owner)
  await release(p)
}

// ---------- players ----------
if (want('players')) {
  section('Nobody gets hurt')
  const victim = await bot('Standby', { physics: PHYSICS })
  await s.ask('gamemode survival Standby', 200)
  {
    const p = await plot()
    const g = await grow(p, 'jungle_tree', 'jungle_log')
    await s.ask(`tp Standby ${p.X + 3.5} ${Y} ${p.Z + 0.5}`, 800)
    await s.ask(`summon minecraft:oak_boat ${p.X + 5.5} ${Y} ${p.Z + 0.5}`, 300)
    victim.hurt = 0
    await axe()
    await fellAndAccount(p, g.base, 'jungle_log', g.t, 'a bot underneath and a boat', { bots: [me, victim] })
    check('the bot underneath isn\'t hurt', victim.hurt === 0 && victim.health === 20, `health ${victim.health}`)
    check('no log was placed inside the bot', me.blockAt(new Vec3(p.X + 3, Y, p.Z))?.name !== 'jungle_log' && me.blockAt(new Vec3(p.X + 3, Y + 1, p.Z))?.name !== 'jungle_log')
    check('the boat is still there', (await s.count(`@e[type=oak_boat,x=${p.X},y=${Y},z=${p.Z},distance=..20]`)) === 1)
    check('no log was placed inside the boat', me.blockAt(new Vec3(p.X + 5, Y, p.Z))?.name !== 'jungle_log')
    await s.ask(`kill @e[type=oak_boat]`, 200)
    await release(p)
  }
  if (PHYSICS) {
    const p = await plot()
    const g = await grow(p, 'jungle_tree', 'jungle_log')
    const top = blocksNear(me, 'jungle_leaves', p.c, 20).reduce((m, v) => (v.y > m.y ? v : m), new Vec3(0, -999, 0))
    await s.ask(`tp Standby ${top.x + 0.5} ${top.y + 1} ${top.z + 0.5}`, 2000)
    victim.hurt = 0
    await axe(); await chop(me, g.base); await waitFall(); await sleep(3000)
    check('a bot on top of the tree takes no damage', victim.hurt === 0, `hurt ${victim.hurt}, now at y=${victim.entity.position.y.toFixed(1)}`)
    check('and it really fell', victim.entity.position.y < top.y - 3, `y=${victim.entity.position.y}`)
    await release(p)
  } else {
    console.log('  skip a bot on top of the tree: bots can\'t move on 26.3 (see harness.mjs)')
  }
  await quit(victim)
}

// ---------- replant ----------
if (want('replant')) {
  section('Replanting')
  for (const ground of ['dirt', 'grass_block', 'mud', 'moss_block', 'stone']) {
    const p = await plot()
    const g = await grow(p, 'birch', 'birch_log')
    await s.ask(`setblock ${p.X} ${Y - 1} ${p.Z} minecraft:${ground}`, 200)
    await axe(); await chop(me, g.base); await waitFall()
    const n = blocksNear(me, 'birch_sapling', p.c, 3).length
    check(`replant on ${ground}: ${ground === 'stone' ? 'none' : 'one sapling'}`, n === (ground === 'stone' ? 0 : 1), 'found ' + n)
    await release(p)
  }
  {
    await config({ 'fall-ticks': 100 })
    const p = await plot()
    const g = await grow(p, 'birch', 'birch_log')
    await axe(); await chop(me, g.base); await sleep(2000) // past the one-second warning, into the fall
    await s.ask(`setblock ${p.X} ${Y} ${p.Z} minecraft:cobblestone`, 200)
    await waitFall(100)
    check('a blocked spot gets no sapling', blocksNear(me, 'birch_sapling', p.c, 3).length === 0)
    check('and what blocked it is untouched', me.blockAt(new Vec3(p.X, Y, p.Z))?.name === 'cobblestone')
    await config()
    await release(p)
  }
  for (const [feature, log, sapling, expect] of [['dark_oak', 'dark_oak_log', 'dark_oak_sapling', 0], ['mega_spruce', 'spruce_log', 'spruce_sapling', 1]]) {
    const p = await plot()
    const g = await grow(p, feature, log)
    const corners = blocksNear(me, log, p.c, 3).filter(v => v.y === g.base.position.y)
    const minX = Math.min(...corners.map(v => v.x)), minZ = Math.min(...corners.map(v => v.z))
    await s.ask(`setblock ${minX} ${g.base.position.y - 1} ${minZ} minecraft:stone`, 200)
    await s.ask(`tp Chopper ${minX - 2 + 0.5} ${g.base.position.y} ${minZ + 1.5} -90 10`, 500)
    await axe(); await chop(me, me.blockAt(new Vec3(minX, g.base.position.y, minZ + 1))); await waitFall()
    const n = blocksNear(me, sapling, p.c, 4).length
    check(`${feature} with one corner on stone: ${expect === 0 ? 'no saplings (one alone never grows)' : 'one sapling (grows small)'}`, n === expect, 'found ' + n)
    await release(p)
  }
}

// ---------- felling from the cut ----------
// A tree built from the console: a straight oak trunk of six logs and natural (non-persistent) leaves packed on
// one side, so which way it leans is known. The console leaves no record of placing it, so the plugin sees a natural
// tree, as with /place feature.
const HEAVY = { east: [-1, 6, -2, 2], west: [-6, 1, -2, 2], north: [-2, 2, -6, 1], south: [-2, 2, -1, 6], none: [-3, 3, -3, 3] }
async function craft(p, heavy) {
  const [x0, x1, z0, z1] = HEAVY[heavy]
  await s.ask(`fill ${p.X} ${Y} ${p.Z} ${p.X} ${Y + 5} ${p.Z} minecraft:oak_log`, 300)
  await s.ask(`fill ${p.X + x0} ${Y + 3} ${p.Z + z0} ${p.X + x1} ${Y + 5} ${p.Z + z1} minecraft:oak_leaves[persistent=false,distance=1] keep`, 400)
  await sleep(600)
  return s.treeCheck(p.X, Y, p.Z)
}
/** Which way a fallen tree lies: the side its logs are on, relative to the stump's column. */
function lies(p, name = 'oak_log') {
  let sx = 0, sz = 0
  for (const v of blocksNear(me, name, p.c, 20)) if (!(v.x === p.X && v.z === p.Z)) { sx += v.x - p.X; sz += v.z - p.Z }
  if (sx === 0 && sz === 0) return 'nowhere'
  return Math.abs(sx) >= Math.abs(sz) ? (sx > 0 ? 'east' : 'west') : (sz > 0 ? 'south' : 'north')
}
async function stand(p, dx, dz, yaw) {
  await s.ask(`tp Chopper ${p.X + dx + 0.5} ${Y} ${p.Z + dz + 0.5} ${yaw} 10`, 600)
  await until(() => me.entity.position.distanceTo(new Vec3(p.X + dx, Y, p.Z + dz)) < 3, 5000)
}

if (want('felling')) {
  section('Felling from the cut (default config: a one-second warning)')
  const sounds = []
  const noted = (name, pos) => sounds.push({ name: String(name), at: Date.now() })
  me.on('soundEffectHeard', (name, pos) => noted(name, pos))
  me.on('hardcodedSoundEffectHeard', (id, cat, pos) => noted(id, pos))
  {
    // 1. a chop above the base leaves a standing stump, and plants nothing
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    if (check('stump: an oak grows, is natural and has 4 or more logs', g.ok && g.t.falls && g.t.logs >= 4, g.ok ? JSON.stringify(g.t) : g.why)) {
      const cut = g.base.position.offset(0, 2, 0)
      const atCut = me.blockAt(cut)
      await axe()
      await chop(me, atCut)
      await waitFall(44)
      const a = await settledAccount(p, 'oak_log')
      const bx = g.base.position.x, bz = g.base.position.z, by = g.base.position.y
      const column = y => me.blockAt(new Vec3(bx, y, bz))?.name
      check('stump: the two logs below the cut stand', column(by) === 'oak_log' && column(by + 1) === 'oak_log', `${column(by)}, ${column(by + 1)}`)
      check('stump: the cut log and everything above it is gone from the trunk', column(by + 2) !== 'oak_log' && blocksNear(me, 'oak_log', new Vec3(bx, by + 4, bz), 6).filter(v => v.x === bx && v.z === bz && v.y >= by + 2).length === 0)
      check(`stump: every log is accounted for, the stump's included (${g.t.logs})`, a.total === g.t.logs, JSON.stringify(a))
      check('stump: no sapling is planted', blocksNear(me, 'oak_sapling', p.c, 8).length === 0)
      check('stump: no block displays left behind', (await displays()) === 0)
    }
    await release(p)
  }
  {
    // 2. a chop at the ground fells everything and replants
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await axe()
    await chop(me, g.base)
    await waitFall(44)
    const a = await settledAccount(p, 'oak_log')
    const bp = g.base.position
    check('ground chop: no log is left in the trunk column', blocksNear(me, 'oak_log', p.c, 8).filter(v => v.x === bp.x && v.z === bp.z).length === 0)
    check(`ground chop: every log is accounted for (${g.t.logs})`, a.total === g.t.logs, JSON.stringify(a))
    check('ground chop: one sapling replanted', blocksNear(me, 'oak_sapling', p.c, 4).length === 1)
    await release(p)
  }
  // 3. toward the heavy side, whichever way the chopper faces
  for (const [heavy, dx, dz, yaw, facing] of [['west', 0, 2, 180, 'north'], ['north', 2, 0, 90, 'west'], ['east', -2, 0, 180, 'north'], ['south', 0, -2, 90, 'west']]) {
    const p = await plot()
    const t = await craft(p, heavy)
    if (!check(`lean ${heavy}: the built tree is seen as natural`, t.falls, JSON.stringify(t))) { await release(p); continue }
    await stand(p, dx, dz, yaw)
    await axe()
    await chop(me, me.blockAt(new Vec3(p.X, Y, p.Z)))
    await waitFall(44)
    const way = lies(p)
    check(`lean ${heavy}: falls ${heavy} with the bot facing ${facing}`, way === heavy, `fell ${way}`)
    await release(p)
  }
  for (const [yaw, facing, dx, dz] of [[180, 'north', 0, 2], [-90, 'east', -2, 0]]) {
    const p = await plot()
    const t = await craft(p, 'none')
    if (!check(`lean none (${facing}): the balanced tree is seen as natural`, t.falls, JSON.stringify(t))) { await release(p); continue }
    await stand(p, dx, dz, yaw)
    await axe()
    await chop(me, me.blockAt(new Vec3(p.X, Y, p.Z)))
    await waitFall(44)
    const way = lies(p)
    check(`a tree standing straight falls away from the chopper (facing ${facing})`, way === facing, `fell ${way}`)
    await release(p)
  }
  {
    // 4. the fall starts about warning-ticks after the chop, with creaks first
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    const watch = g.base.position.offset(0, 3, 0)
    sounds.length = 0
    await axe()
    await chop(me, me.blockAt(g.base.position.offset(0, 1, 0)))
    const t0 = Date.now()
    let standingAt300 = null, goneAt = null
    while (Date.now() - t0 < 6000) {
      const standing = me.blockAt(watch)?.name === 'oak_log'
      if (Date.now() - t0 >= 300 && standingAt300 === null) standingAt300 = standing
      if (!standing) { goneAt = Date.now() - t0; break }
      await sleep(20)
    }
    check('warning: the trunk above the cut still stands 300 ms after the chop', standingAt300 === true, `standing at 300 ms: ${standingAt300}`)
    check('warning: it comes down about a second after the chop (20 ticks)', goneAt !== null && goneAt >= 750 && goneAt <= 1900, `gone after ${goneAt} ms`)
    // Through ViaVersion the bot gets sound ids shifted onto other names (a creak arrives as "crafter.fail"), so what's
    // checked is the pattern: the chop's own sounds, then pairs about 7 ticks apart (0.35 s) until the tree goes.
    const heard = sounds.filter(x => x.at < t0 + (goneAt ?? 0))
    const span = heard.length ? heard[heard.length - 1].at - heard[0].at : 0
    console.log(`  sounds heard before the fall: ${heard.length} over ${span} ms (${[...new Set(sounds.map(x => x.name))].join(', ')}; names are shifted by ViaVersion)`)
    check('warning: the bot hears repeated creaks spread over the wait before the tree goes', heard.length >= 6 && span >= 500, JSON.stringify(heard.slice(0, 10)))
    await waitFall(44)
    await release(p)
    await config({ 'warning-ticks': 0 })
    const q = await plot()
    const h = await grow(q, 'oak', 'oak_log')
    const w2 = h.base.position.offset(0, 3, 0)
    await axe()
    await chop(me, me.blockAt(h.base.position.offset(0, 1, 0)))
    const u0 = Date.now()
    let gone2 = null
    while (Date.now() - u0 < 4000) { if (me.blockAt(w2)?.name !== 'oak_log') { gone2 = Date.now() - u0; break } await sleep(20) }
    check('warning-ticks 0: the tree falls at once', gone2 !== null && gone2 < 600, `gone after ${gone2} ms`)
    await waitFall(24)
    await config()
    await release(q)
  }
}

// ---------- fall-danger ----------
// The trunk is cut at Y+2 so its logs come down through the head and feet of a bot at ground level: a log cut at
// the ground lands at foot height only. The bots can't move on 26.3, so they stand where /tp puts them (the victim
// two blocks short of where the trunk ends, on the line the tree falls along).
if (want('danger')) {
  section('fall-danger')
  const victim = await bot('Standby', { physics: PHYSICS })
  await s.ask('gamemode survival Standby', 200)
  victim.died = 0; victim.flung = 0
  victim.on('death', () => victim.died++)
  // a bot's own movement is its client's business, so the fling is read from the velocity packet the server sends it
  victim._client.on('entity_velocity', d => { if (d.entityId === victim.entity.id && Math.hypot(d.velocity.x, d.velocity.z) > 0.5) victim.flung++ })
  async function scene(label, cfg, cutDy, mode = 'survival') {
    await config(cfg)
    const p = await plot()
    const t = await craft(p, 'east')
    check(`${label}: built tree is natural`, t.falls, JSON.stringify(t))
    await s.ask(`gamemode ${mode} Standby`, 200)
    await s.ask(`tp Standby ${p.X + 3.5} ${Y} ${p.Z + 0.5}`, 800)
    await until(() => victim.entity && victim.entity.position.distanceTo(new Vec3(p.X + 3.5, Y, p.Z + 0.5)) < 2, 5000)
    victim.died = 0; victim.flung = 0; victim.hurt = 0
    await s.ask('effect clear Standby', 100)
    await s.ask('attribute Standby minecraft:max_health base set 20', 100)
    await s.ask('effect give Standby minecraft:saturation 30 5 true', 100)
    await stand(p, -2, 0, -90)
    await axe()
    await chop(me, me.blockAt(new Vec3(p.X, Y + cutDy, p.Z)))
    await waitFall(44)
    await sleep(1500)
    const out = { p, flung: victim.flung, health: victim.health, hurt: victim.hurt, died: victim.died }
    await release(p)
    return out
  }
  {
    const r = await scene('danger off', {}, 2)
    check('fall-danger off: a survival bot where the trunk lands keeps full health', r.died === 0 && r.hurt === 0 && r.health === 20, `health ${r.health}, hurt ${r.hurt}, died ${r.died}`)
    check('fall-danger off: it was flung clear of the trunk', r.flung >= 1, `${r.flung} velocity packets over 0.5`)
  }
  {
    const r = await scene('danger max, one world', { 'fall-danger-worlds': '{ testworld: max }' }, 2)
    check('fall-danger max in one world: a survival bot under the trunk dies', r.died === 1, `health ${r.health}, hurt ${r.hurt}, died ${r.died}`)
  }
  {
    const r = await scene('danger max, feet only', { 'fall-danger': 'max' }, 0)
    check('fall-danger max: a log landing at foot height deals 4', r.died === 0 && r.hurt === 4, `health ${r.health}, hurt ${r.hurt}, died ${r.died}`)
  }
  {
    const r = await scene('danger max, creative', { 'fall-danger': 'max' }, 2, 'creative')
    check('fall-danger max: a creative bot is never hurt', r.died === 0 && r.hurt === 0, `health ${r.health}, hurt ${r.hurt}, died ${r.died}`)
  }
  {
    const r = await scene('danger max, world set to off', { 'fall-danger': 'max', 'fall-danger-worlds': '{ testworld: off }' }, 2)
    check('a world set to off overrides fall-danger max', r.died === 0 && r.hurt === 0, `health ${r.health}, hurt ${r.hurt}, died ${r.died}`)
  }
  await config({ 'fall-danger': 'off', 'fall-danger-worlds': '{}' })
  await s.ask('gamemode survival Standby', 200)
  await quit(victim)
}

// ---------- Prism ----------
if (want('prism')) {
  section('Prism')
  if (!prism) console.log('  skip Prism: it is not installed (run test/fetch-jars.mjs)')
  else {
    check('PaulBunyan found Prism and records in it', s.all.some(l => /Recording in Prism too/.test(l)))
    await s.ask('op Chopper', 300)
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await axe()
    await fellAndAccount(p, g.base, 'oak_log', g.t, 'prism fall')
    await sleep(8000) // Prism writes in batches
    await s.ask(`tp Chopper ${p.X + 0.5} ${Y} ${p.Z + 0.5}`, 600)
    me.chatLog.length = 0
    me.chat('/prism lookup a:block-break r:40 since:10m')
    await sleep(4000)
    const out = me.chatLog.join('\n')
    const broke = (out.match(/oak_log/gi) || []).length
    check('Prism lookup shows the fall\'s logs broken by the chopping bot', /Chopper/.test(out) && broke >= 1, out.slice(0, 800))
    await release(p)
  }
}

// ---------- mid-fall ----------
async function midFall(label, interrupt) {
  await config({ 'fall-ticks': 160 })
  const p = await plot()
  const g = await grow(p, 'jungle_tree', 'jungle_log')
  await s.ask('save-all flush', 1500)
  await axe()
  await chop(me, g.base)
  await sleep(1500)
  await interrupt(p)
  // back on the plot, with the bot reconnected if the server restarted
  await s.ask(`tp Chopper ${p.X - 1.5} ${Y} ${p.Z + 0.5} -90 10`, 800)
  await s.ask(`forceload add ${p.X - 24} ${p.Z - 24} ${p.X + 40} ${p.Z + 24}`, 2000)
  await sleep(10000) // past the end of the fall either way
  const a = await settledAccount(p, 'jungle_log')
  check(`${label}: no log lost or doubled (${g.t.logs})`, a.total === g.t.logs, JSON.stringify(a))
  check(`${label}: no block displays left`, (await displays()) === 0)
  await config()
  await release(p)
  return a
}
async function reconnect() {
  await quit(me)
  me = await bot('Chopper', { physics: PHYSICS })
}
if (want('midfall')) {
  section('Interrupted falls')
  await midFall('chunk unloaded mid-fall', async p => {
    await release(p)
    await s.ask(`tp Chopper ${p.X + 3000} ${Y} ${p.Z}`, 500)
    const unloaded = await until(async () => (await s.ask(`execute if loaded ${p.X} ${Y} ${p.Z}`, 300)).join(' ').includes('failed'), 20000, 500)
    check('chunk unloaded mid-fall: the chunk really unloaded', unloaded)
    await sleep(9000)
  })
  await midFall('server stopped mid-fall', async () => {
    await s.stop(); await s.start(); await reconnect()
  })
  await midFall('kill -9 mid-fall, before any save', async () => {
    await s.kill9(); await s.start(); await reconnect()
  })
  await midFall('kill -9 mid-fall, after the world saved', async () => {
    await s.ask('save-all flush', 2500); await s.kill9(); await s.start(); await reconnect()
  })
  check('the fall journal is replayed after a kill -9', s.all.some(l => /Finishing a fall the server stopped during/.test(l)))
}

// ---------- load ----------
if (want('load')) {
  section('Load')
  {
    await config({ 'max-logs': 20 })
    const p = await plot()
    const g = await grow(p, 'mega_jungle_tree', 'jungle_log')
    const t = await s.treeCheck(g.base.position.x, g.base.position.y, g.base.position.z)
    check('an enormous tree hits the size cap', !t.falls && /size cap/.test(t.reason), JSON.stringify(t))
    const before = blocksNear(me, 'jungle_log', p.c, 30).length
    await axe(); await chop(me, g.base); await waitFall()
    check('and breaks like vanilla', blocksNear(me, 'jungle_log', p.c, 30).length === before - 1)
    await config()
    await release(p)
  }
  {
    const bots = []
    const plots = []
    for (let i = 0; i < 10; i++) {
      const name = 'Feller' + i
      const b = await bot(name, { physics: PHYSICS })
      await s.ask(`gamemode survival ${name}`, 100)
      const X = 2000 + 160 * plotNo++, Z = 2000
      await s.ask(`forceload add ${X - 8} ${Z - 8} ${X + 24} ${Z + 8}`, 300)
      await s.ask(`place feature minecraft:fancy_oak ${X} ${Y} ${Z}`, 600)
      await s.ask(`tp ${name} ${X - 1.5} ${Y} ${Z + 0.5} -90 10`, 300)
      await s.ask(`give ${name} minecraft:diamond_axe`, 300)
      bots.push(b); plots.push({ X, Z })
    }
    await sleep(3000)
    for (const b of bots) await b.equip(b.inventory.items().find(i => i.name === 'diamond_axe'), 'hand').catch(() => {})
    await s.ask('mspt', 300)
    await sleep(5500)
    const before = (await s.ask('mspt', 600)).join(' ')
    const digs = bots.map((b, i) => {
      const bl = lowestLog(b, plots[i].X, plots[i].Z, 'oak_log')
      return bl ? chop(b, bl).catch(() => {}) : Promise.resolve()
    })
    await Promise.all(digs)
    await sleep(2500)
    const during = (await s.ask('mspt', 600)).join(' ')
    const tps = (await s.ask('tps', 600)).join(' ')
    const avg = txt => { const m = txt.match(/([\d.]+)\/[\d.]+\/([\d.]+)/); return m ? { avg: +m[1], max: +m[2] } : null }
    const d = avg(during)
    check('ten bots felling at once: average tick stays under 50 ms', d && d.avg < 50, `before: ${before.slice(-120)} | during: ${during.slice(-120)} | ${tps.slice(-80)}`)
    const left = plots.reduce((n, pl) => n + blocksNear(bots[0], 'oak_log', new Vec3(pl.X, Y, pl.Z), 1.5).length, 0)
    check('ten bots felling at once: all ten trees came down', left === 0 || bots.every((b, i) => !lowestLog(b, plots[i].X, plots[i].Z, 'oak_log') || lowestLog(b, plots[i].X, plots[i].Z, 'oak_log').position.x > plots[i].X), 'stumps left: ' + left)
    check('ten bots felling at once: no displays left', (await displays()) === 0)
    for (const b of bots) await quit(b)
    for (const pl of plots) await s.ask(`forceload remove ${pl.X - 8} ${pl.Z - 8} ${pl.X + 24} ${pl.Z + 8}`, 100)
  }
}

// ---------- other plugins and creative ----------
if (want('other')) {
  section('Block logging and creative mode')
  if (coreProtect) {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await axe()
    await fellAndAccount(p, g.base, 'oak_log', g.t, 'logged fall')
    await sleep(6000) // CoreProtect writes in batches
    const broke = (await s.ask(`co lookup user:Chopper time:5m radius:#global action:-block`, 3000)).join('\n')
    const put = (await s.ask(`co lookup user:Chopper time:5m radius:#global action:+block`, 3000)).join('\n')
    const out = broke + '\n' + put
    const removed = (broke.match(/(broke|removed) oak_log/gi) || []).length
    const placed = (put.match(/placed oak_log/gi) || []).length
    check('CoreProtect shows the logs removed and placed by the fall', removed >= 1 && placed >= 1, out.slice(0, 600))
    await release(p)
  } else {
    console.log(`  skip block logging: CoreProtect 24.1 doesn't start on ${MC} ("not supported"), so it can't be tested here`)
  }
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await s.ask('gamemode creative Chopper', 300)
    await axe(); await chop(me, g.base); await waitFall()
    check('creative mode chops one log, as documented', blocksNear(me, 'oak_log', p.c, 10).length === g.t.logs - 1)
    await s.ask('gamemode survival Chopper', 300)
    await release(p)
  }
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    me.setControlState('sneak', true); await sleep(300)
    await axe(); await chop(me, g.base); await waitFall()
    me.setControlState('sneak', false)
    check('sneaking chops one log', blocksNear(me, 'oak_log', p.c, 10).length === g.t.logs - 1)
    await release(p)
  }
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await s.ask('clear Chopper', 200)
    await s.ask('give Chopper minecraft:wooden_axe[damage=56]', 400) // 59 uses: 3 left, fewer than the logs
    await until(() => me.inventory.items().some(i => i.name === 'wooden_axe'), 4000)
    await me.equip(me.inventory.items().find(i => i.name === 'wooden_axe'), 'hand')
    await chop(me, g.base); await waitFall()
    check('a nearly worn-out axe chops one log and survives', blocksNear(me, 'oak_log', p.c, 10).length === g.t.logs - 1 && me.inventory.items().some(i => i.name === 'wooden_axe'))
    await release(p)
  }
  {
    const p = await plot()
    const g = await grow(p, 'oak', 'oak_log')
    await s.ask('clear Chopper', 200)
    await s.ask('give Chopper minecraft:iron_axe', 400)
    await until(() => me.inventory.items().some(i => i.name === 'iron_axe'), 4000)
    await me.equip(me.inventory.items().find(i => i.name === 'iron_axe'), 'hand')
    await chop(me, g.base); await waitFall()
    const dmg = (await s.ask('data get entity Chopper SelectedItem', 400)).join(' ')
    const m = dmg.match(/"?minecraft:damage"?: (\d+)/)
    check(`the axe wears one point per log (${g.t.logs})`, m && +m[1] === g.t.logs, dmg.slice(0, 200))
    await release(p)
  }
}

const errs = s.exceptions()
check('no errors from PaulBunyan in the server log', errs.length === 0, errs.slice(0, 10).join('\n'))
await quit(me)
await s.stop()
// one throwaway server at a time, and none left behind
if (!process.env.PB_TEST_KEEP) rmSync(dir, { recursive: true, force: true, maxRetries: 5, retryDelay: 500 })
process.exit(summary() ? 1 : 0)
