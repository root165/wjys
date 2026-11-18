# 维检云枢移动端

移动端采用 UniApp Vue 3。当前已落地离线巡检提交的本地契约：

- `src/repositories/storageRepository.js` 统一管理 Storage 键、容量预警和可重拉取缓存清理；不删除待同步队列、表单草稿和本地照片索引。
- `src/repositories/syncQueue.js` 保存 `submitNo`、业务类型、业务主键、请求摘要、创建时间、重试次数和状态；网络恢复后按创建顺序同步，409 冲突保留给用户处理。
- `src/api/http.js` 统一携带访问令牌，并提供照片上传入口。
- 照片只保存私有路径和上传结果，业务提交成功且服务端绑定完成后才允许业务层清理本地照片。
- 登录会保存 access/refresh token；收到 401 时单例刷新并重试原请求，刷新失败则回到登录页。
- 巡检填写页支持草稿、正常/异常结果、数值校验、必拍照片、相机/相册选择和离线提交。
- 消息中心支持列表查看和移动端标记已读；任务详情从增量同步快照读取，避免离线时依赖接口。

## 本地构建

1. 安装 Node.js 18/20，并在本目录执行 `npm install`。
2. 复制 `.env.example` 为 `.env.production`，把 `VITE_API_BASE_URL` 改成手机可访问的后端地址；真机不能填写 `localhost`。
3. H5 调试执行 `npm run dev:h5`；Android 调试执行 `npm run dev:app`，也可以用 HBuilderX 导入本目录运行到 Android 真机/模拟器。
4. Android 前端编译执行 `npm run build:app`，产物位于 `dist/build/app`，可直接在 HBuilderX 导入运行。正式 APK/AAB 需要在 HBuilderX 的发行菜单配置正式 appid、签名证书后生成；本仓库已声明相机、图片读取权限。

当前仓库已补齐并验证 Vite/UniApp CLI 配置：`npm install`、`npm test`、`npm run build:h5`、`npm run build:app` 均可执行；本机未检测到 HBuilderX、adb 或 Gradle，因此尚未生成签名 APK/AAB。真机还需验证飞行模式、杀进程、Storage 容量、扫码、相机目录和弱网恢复。

不依赖 UniApp CLI 的队列契约测试可直接执行 `npm test`，覆盖离线提交成功重试和 409 冲突保留。
