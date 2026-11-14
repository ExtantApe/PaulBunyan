// Downloads the test servers' jars into a folder and checks each against the hash its publisher lists:
// the latest stable Paper 26.1.2, 26.2 and 26.3 builds, and from Modrinth ViaVersion and ViaBackwards
// (the bots speak 1.21.11), GriefPrevention, CoreProtect and Prism.
// Usage: node test/fetch-jars.mjs <folder>
import { createHash } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'

const dir = process.argv[2]
if (!dir) throw new Error('Usage: node test/fetch-jars.mjs <folder>')
mkdirSync(dir, { recursive: true })
const headers = { 'User-Agent': 'PaulBunyan-tests (github.com/ExtantApe/PaulBunyan)' }
const json = async url => { const r = await fetch(url, { headers }); if (!r.ok) throw new Error(`${url}: ${r.status}`); return r.json() }

async function save(url, name, algo, expected) {
  const r = await fetch(url, { headers })
  if (!r.ok) throw new Error(`${url}: ${r.status}`)
  const bytes = Buffer.from(await r.arrayBuffer())
  const got = createHash(algo).update(bytes).digest('hex')
  if (got !== expected) throw new Error(`${name}: ${algo} doesn't match what the publisher lists`)
  writeFileSync(path.join(dir, name), bytes)
  console.log(`${name}  ${url.split('/').pop()}`)
}

for (const v of ['26.1.2', '26.2', '26.3']) {
  const b = await json(`https://fill.papermc.io/v3/projects/paper/versions/${v}/builds/latest`)
  const d = b.downloads['server:default']
  await save(d.url, `paper-${v}.jar`, 'sha256', d.checksums.sha256)
}
// [project, file name, the game version it has to list, or a version number to pin]
for (const [project, name, pick] of [
  ['viaversion', 'ViaVersion.jar', { game: '26.3' }],
  ['viabackwards', 'ViaBackwards.jar', { game: '26.3' }],
  ['griefprevention', 'GriefPrevention.jar', { version: '16.18.7' }], // the one release listed for 26.1.2 to 26.3
  ['coreprotect', 'CoreProtect.jar', { version: '24.1' }],
  ['prism', 'Prism.jar', { game: '26.3' }], // 4.5, the first 4.x listed for 26.3
]) {
  const versions = await json(`https://api.modrinth.com/v2/project/${project}/version`)
  const v = pick.version ? versions.find(x => x.version_number === pick.version)
    : versions.find(x => x.version_type === 'release' && x.loaders.includes('paper') && x.game_versions.includes(pick.game))
  if (!v) throw new Error(`${project}: no matching version`)
  const f = v.files.find(x => x.primary) ?? v.files[0]
  await save(f.url, name, 'sha512', f.hashes.sha512)
}
