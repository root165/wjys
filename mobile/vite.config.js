import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

const uni = typeof uniModule === 'function' ? uniModule : uniModule.default

export default defineConfig({
  plugins: [uni()],
  // uni-app 运行时需要 H5 版 Vue 暴露的内部运行时状态，
  // 否则 CLI 打包会从普通 Vue 中找不到 isInSSRComponentSetup。
  resolve: {
    alias: {
      vue: '@dcloudio/uni-h5-vue',
      'vue/package.json': '@dcloudio/uni-h5-vue/package.json',
    },
  },
  server: {
    port: 5174,
  },
})
