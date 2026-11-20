import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

const uni = typeof uniModule === 'function' ? uniModule : uniModule.default
const isH5 = process.env.UNI_PLATFORM === 'h5'

export default defineConfig({
  plugins: [uni()],
  resolve: isH5
    ? {
        alias: {
          vue: '@dcloudio/uni-h5-vue',
          'vue/package.json': '@dcloudio/uni-h5-vue/package.json',
        },
      }
    : undefined,
  server: {
    port: 5174,
  },
})
