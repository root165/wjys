# 维检云枢移动端

移动端采用 UniApp Vue 3。当前已落地离线巡检提交的本地契约：

- `src/repositories/storageRepository.js` 统一管理 Storage 键、容量预警和可重拉取缓存清理；不删除待同步队列、表单草稿和本地照片索引。
- `src/repositories/syncQueue.js` 保存 `submitNo`、业务类型、业务主键、请求摘要、创建时间、重试次数和状态；网络恢复后按创建顺序同步，409 冲突保留给用户处理。
- `src/api/http.js` 统一携带访问令牌，并提供照片上传入口。
- 照片只保存私有路径和上传结果，业务提交成功且服务端绑定完成后才允许业务层清理本地照片。
- 登录会保存 access/refresh token；收到 401 时单例刷新并重试原请求，刷新失败则回到登录页。
- 移动端登录允许 `inspector` 巡检员和 `repairer` 维修工角色；巡检同步/领取/提交仍由后端 `inspector` 权限保护，维修工通过工单与备件接口执行接单、完工和领用。
- 巡检任务遵循“先领取、再填写、再提交”：待领取任务由巡检员领取，固定执行人任务只能由被分配巡检员领取；任务生成前不会绑定管理员或用户 #1。
- 巡检填写页支持草稿、正常/异常结果、数字/文本/下拉检查值、数值范围校验、必拍照片、扫码核验、相机/相册选择和离线提交；照片绑定到具体检查项后才允许服务端提交。
- 任务和消息都会保存本地快照；应用重启或断网时仍可打开已同步任务，联网进入首页时会自动重试离线队列，再拉取最新任务和消息；首页每 30 秒轮询一次，网络恢复立即拉取，移动同步接口禁止代理缓存。
- 消息中心支持增量游标列表和移动端标记已读；任务详情从增量同步快照读取，避免离线时依赖接口。

## 本地构建

1. 安装 Node.js 22.12+，并在本目录执行 `npm install`；源码入口采用标准 `src/` UniApp CLI/HBuilderX 布局。
2. 复制 `.env.example` 为 `.env.production`，把 `VITE_API_BASE_URL` 改成手机可访问的后端地址；真机不能填写 `localhost`。
3. H5 调试执行 `npm run dev:h5`；Android 调试执行 `npm run dev:app`，也可以用 HBuilderX 导入本目录运行到 Android 真机/模拟器。
4. Android 前端编译执行 `npm run build:app`，产物位于 `dist/build/app`，仅用于检查编译结果；HBuilderX 运行时应导入本目录根目录。正式 APK/AAB 需要在 HBuilderX 的发行菜单配置正式 appid、签名证书后生成；本仓库已声明相机、图片读取权限。

## HBuilderX 识别与 Android 运行

HBuilderX 识别的是项目类型，不是 `npm run build:app` 的输出类型。请导入 `mobile` 项目根目录，不要把 `dist/build/app` 当作工程导入。若项目仍显示 Web：

1. 在项目管理器中移除当前 `mobile` 项目（只移除，不删除磁盘文件）。
2. 使用“文件 → 导入 → 从本地目录导入”，选择 `mobile` 根目录。
3. 右键 `mobile` 项目，选择“重新识别项目类型”。
4. 项目图标应变为方形 `U`（uni-app），随后使用“运行 → 运行到手机或模拟器”。

不要删除或移动 `src/manifest.json`、`src/pages.json`；这是 CLI 项目被 HBuilderX 识别为 uni-app 的关键配置。`dist/build/app` 只是 App 资源编译产物，不是 APK/AAB；正式安装包应在 HBuilderX 的“发行 → 原生 App-云打包”中配置 AppID 和 Android 签名后生成。

当前仓库已补齐并验证 Vite/UniApp CLI 配置：`npm install`、`npm test`、`npm run build:h5`、`npm run build:app` 均可执行；本机未检测到 HBuilderX、adb 或 Gradle，因此尚未生成签名 APK/AAB。真机还需验证飞行模式、杀进程、Storage 容量、扫码、相机目录和弱网恢复。

不依赖 UniApp CLI 的队列契约测试可直接执行 `npm test`，覆盖离线提交成功重试和 409 冲突保留。
