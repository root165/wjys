import { spawnSync } from 'node:child_process'
import path from 'node:path'

const binary = path.join(process.cwd(), 'node_modules', '@dcloudio', 'vite-plugin-uni', 'bin', 'uni.js')
const result = spawnSync(process.execPath, [binary, ...process.argv.slice(2)], {
  stdio: 'inherit',
})

if (result.error) {
  console.error(result.error.message)
  process.exit(1)
}
process.exit(result.status ?? 1)
