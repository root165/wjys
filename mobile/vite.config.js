import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

// 本项目将 pages.json、manifest.json 与 pages/ 放在 mobile 根目录，
// 显式指定 UniApp 输入目录，兼容 CLI 与 HBuilderX 的默认 src/ 约定。
process.env.UNI_INPUT_DIR = process.env.UNI_INPUT_DIR || process.cwd()
const uni = typeof uniModule === 'function' ? uniModule : uniModule.default

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5174,
  },
})
