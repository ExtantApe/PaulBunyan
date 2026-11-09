// Test harness: throwaway Paper servers in fresh temp folders on 127.0.0.1:25612, driven from the console,
// with mineflayer bots as players. It never copies or touches a real server: every server is built here
// from downloaded jars, on a flat world with made-up names. Everything it makes lives under PB_TEST_WORK.
import { spawn } from 'node:child_process'
import { copyFileSync, existsSync, mkdirSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import mineflayer from 'mineflayer'

export const HOST = '127.0.0.1'
export const PORT = Number(process.env.PB_TEST_PORT ?? 25612) // PaulBunyan's assigned test port
export const WORK = process.env.PB_TEST_WORK
export const JARS = process.env.PB_TEST_JARS
export const JAVA = process.env.PB_TEST_JAVA ?? 'java'
if (!WORK || !JARS) throw new Error('Set PB_TEST_WORK (an empty temp folder) and PB_TEST_JARS (from test/fetch-jars.mjs)')

const ROOT = new URL('../', import.meta.url).pathname
const LIBS = path.join(ROOT, 'build/libs')
const built = existsSync(LIBS) ? readdirSync(LIBS).filter(f => /^PaulBunyan-.*\.jar$/.test(f)) : []
if (built.length !== 1) throw new Error(`Run ./gradlew clean build first: expected one jar in build/libs, found ${built.length}`)
export const JAR = path.join(LIBS, built[0])
export const DEFAULT_CONFIG = path.join(ROOT, 'src/main/resources/config.yml')

process.setMaxListeners(200)
export const sleep = ms => new Promise(r => setTimeout(r, ms))
export async function until(fn, ms = 10000, step = 100) {
  const end = Date.now() + ms
  while (Date.now() < end) { if (await fn()) return true; await sleep(step) }
  return !!(await fn())
}

// ---------- results ----------
let passed = 0; const failures = []
export function check(name, ok, detail = '') {
  if (ok) { passed++; console.log('  ok   ' + name + (process.env.PB_TEST_DETAIL && detail ? '  (' + detail + ')' : '')) }
  else { failures.push(name); console.log('  FAIL ' + name + (detail ? '\n         ' + String(detail).slice(0, 800) : '')) }
  return ok
}
export function section(title) { console.log('\n# ' + title) }
export function summary() {
  console.log(`\n${passed} passed, ${failures.length} failed`)
  for (const f of failures) console.log('  - ' + f)
  return failures.length
}

// ---------- servers ----------
export function jar(name) {
  const f = path.join(JARS, name)
  if (!existsSync(f)) throw new Error('missing ' + f + ': run test/fetch-jars.mjs')
  return f
}

/**
 * A fresh server folder. Writing eula=true accepts Mojang's EULA for this throwaway local server, which
 * the owner allows for test servers on 127.0.0.1.
 */
export function freshServer(name, { paper, plugins = [], props = {} }) {
  const dir = path.join(WORK, name)
  rmSync(dir, { recursive: true, force: true, maxRetries: 5, retryDelay: 500 })
  mkdirSync(path.join(dir, 'plugins'), { recursive: true })
  copyFileSync(paper, path.join(dir, 'paper.jar'))
  for (const p of [JAR, ...plugins]) copyFileSync(p, path.join(dir, 'plugins', path.basename(p)))
  writeFileSync(path.join(dir, 'eula.txt'), 'eula=true\n')
  const all = { 'server-ip': HOST, 'server-port': PORT, 'online-mode': 'false', 'spawn-protection': 0,
    'enable-query': 'false', 'enable-rcon': 'false', 'view-distance': 6, 'simulation-distance': 4, 'max-players': 40,
    'enforce-secure-profile': 'false', 'spawn-monsters': 'false', 'spawn-animals': 'false', difficulty: 'peaceful',
    'level-type': 'minecraft\\:flat', 'generate-structures': 'false', 'level-name': 'testworld', 'white-list': 'false',
    'enforce-whitelist': 'false',
    // grass on top at y=-61, so trees stand at y=-60
    'generator-settings': JSON.stringify({ layers: [{ block: 'minecraft:bedrock', height: 1 }, { block: 'minecraft:dirt', height: 2 },
      { block: 'minecraft:grass_block', height: 1 }], biome: 'minecraft:plains' }), ...props }
  writeFileSync(path.join(dir, 'server.properties'), Object.entries(all).map(([k, v]) => `${k}=${v}`).join('\n') + '\n')
  return dir
}

export class Server {
  constructor({ dir, memory = '4G', jvm = [] }) { Object.assign(this, { dir, memory, jvm }); this.log = []; this.all = []; this.proc = null }
  async start(ms = 300000) {
    this.log = []
    this.proc = spawn(JAVA, [`-Xmx${this.memory}`, ...this.jvm, '-jar', 'paper.jar', '--nogui'], { cwd: this.dir, stdio: ['pipe', 'pipe', 'pipe'] })
    let buf = ''
    const onData = d => {
      buf += d.toString()
      let i; while ((i = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, i).replace(/\x1b\[[0-9;]*m/g, ''); buf = buf.slice(i + 1)
        this.log.push(line); this.all.push(line); if (process.env.PB_TEST_VERBOSE) console.log('    | ' + line)
      }
    }
    this.proc.stdout.on('data', onData); this.proc.stderr.on('data', onData)
    this.exited = new Promise(r => this.proc.once('exit', r))
    const proc = this.proc
    process.once('exit', () => { if (proc.exitCode === null) proc.kill('SIGKILL') }) // never leave a server behind
    if (!await until(() => this.log.some(l => /Done \([\d.,]+s\)! For help/.test(l)) || this.proc.exitCode !== null, ms, 250)) throw new Error('server did not start')
    if (this.proc.exitCode !== null) throw new Error('server exited:\n' + this.log.slice(-30).join('\n'))
  }
  cmd(c) { this.proc.stdin.write(c + '\n') }
  async ask(c, ms = 600) { const from = this.log.length; this.cmd(c); await sleep(ms); return this.log.slice(from).map(l => l.replace(/^\[[\d:]+ \w+\]: /, '')) }
  mark() { return this.log.length }
  since(mark) { return this.log.slice(mark) }
  async waitLog(re, ms = 10000, from = 0) { return until(() => this.log.slice(from).some(l => re.test(l)), ms) }
  exceptions() { return this.all.filter(l => /at com\.extantape\.paulbunyan|PaulBunyan.*(Exception|Error occurred)|\[PaulBunyan\] .*(SEVERE|ERROR)/i.test(l)) }
  async stop() {
    if (!this.proc || this.proc.exitCode !== null) return
    this.cmd('stop')
    const t = setTimeout(() => this.proc.kill('SIGKILL'), 90000)
    await this.exited; clearTimeout(t)
  }
  /** kill -9: no shutdown, no saving. */
  async kill9() { if (this.proc && this.proc.exitCode === null) { this.proc.kill('SIGKILL'); await this.exited } }

  /** /paulbunyan check, parsed. */
  async treeCheck(x, y, z) {
    const out = (await this.ask(`paulbunyan check ${x} ${y} ${z} minecraft:overworld`, 500)).join('\n')
    const m = out.match(/check \S+ [-\d,]+: falls, (\w+), (\d+) logs, (\d+) leaves, (\d+) placed/)
    if (m) return { falls: true, species: m[1], logs: +m[2], leaves: +m[3], strays: +m[4] }
    const v = out.match(/check \S+ [-\d,]+: breaks like vanilla, (.*)/)
    return { falls: false, reason: v ? v[1] : 'no answer: ' + out.slice(0, 200) }
  }
  /** How many entities a selector finds. */
  async count(selector) {
    const out = (await this.ask(`execute if entity ${selector}`, 400)).join(' ')
    const m = out.match(/count: (\d+)/i)
    return m ? +m[1] : 0
  }
  /** The block at a spot, as the console sees it: tries to set it to itself and reads the error. */
  async place(feature, x, y, z) {
    const out = (await this.ask(`place feature ${feature} ${x} ${y} ${z}`, 600)).join(' ')
    return /Placed "?(feature|minecraft)/i.test(out)
  }
}

// ---------- bots ----------
export const BOT_VERSION = '1.21.11'
/**
 * physics: false stops the bot sending movement packets. On Paper 26.3 behind ViaVersion 5.12.2 a 1.21.11
 * bot is kicked for "Invalid move player packet" after its second one (the ReturnGate suite found this
 * first), so there the bots stand where /tp puts them and never fall.
 */
export async function bot(name, { physics = true } = {}) {
  const b = mineflayer.createBot({ host: HOST, port: PORT, username: name, auth: 'offline', version: BOT_VERSION,
    checkTimeoutInterval: 120000, hideErrors: true, physicsEnabled: physics })
  if (!physics) {
    const send = b._client.write.bind(b._client)
    b._client.write = (n, d) => (/^(position|position_look|look|flying)$/.test(n) ? undefined : send(n, d))
  }
  b.physicsOn = physics
  b.on('error', () => {})
  b.on('kicked', r => { b.kick = typeof r === 'string' ? r : JSON.stringify(r) })
  await new Promise((ok, fail) => {
    const t = setTimeout(() => fail(new Error(name + ' did not spawn')), 60000)
    b.once('spawn', () => { clearTimeout(t); ok() })
    b.once('end', r => { clearTimeout(t); fail(new Error(name + ' ended: ' + r)) })
  })
  b.chatLog = []
  b.on('messagestr', m => b.chatLog.push(m))
  b.hurt = 0
  b.on('health', () => { if (b.health < 20) b.hurt = Math.max(b.hurt, 20 - b.health) })
  return b
}
export function quit(b) { return new Promise(r => { if (!b || b._ended) return r(); b._ended = true; b.once('end', r); b.quit(); setTimeout(r, 3000) }) }

/** Item counts by name in the bot's inventory. */
export function inv(b) {
  const out = {}
  for (const it of b.inventory.items()) out[it.name] = (out[it.name] || 0) + it.count
  return out
}

/** Blocks named {@code name} the bot can see within {@code r} of a point. */
export function blocksNear(b, name, center, r = 40) {
  const id = b.registry.blocksByName[name]?.id
  if (id === undefined) return []
  return b.findBlocks({ matching: id, point: center, maxDistance: r, count: 100000 })
}

/** Dropped item stacks the bot can see near a point, summed by name. */
export function itemsNear(b, center, r = 48) {
  const out = {}
  for (const e of Object.values(b.entities)) {
    if (e.name !== 'item' || e.position.distanceTo(center) > r) continue
    const it = e.getDroppedItem?.()
    if (it) out[it.name] = (out[it.name] || 0) + it.count
  }
  return out
}
