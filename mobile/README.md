# 维检云枢移动端骨架

移动端采用 UniApp Vue 3。当前已落地离线巡检提交的本地契约：

- `src/repositories/storageRepository.js` 统一管理 Storage 键、容量预警和可重拉取缓存清理；不删除待同步队列、表单草稿和本地照片索引。
- `src/repositories/syncQueue.js` 保存 `submitNo`、业务类型、业务主键、请求摘要、创建时间、重试次数和状态；网络恢复后按创建顺序同步，409 冲突保留给用户处理。
- `src/api/http.js` 统一携带访问令牌，并提供照片上传入口。
- 照片只保存私有路径和上传结果，业务提交成功且服务端绑定完成后才允许业务层清理本地照片。

本目录尚未安装 UniApp CLI 依赖，因此本轮未执行移动端构建和真机验证。安装依赖后执行 `npm install`、`npm run build`，并按规约补测飞行模式、杀进程、Storage 容量和相机目录。

不依赖 UniApp CLI 的队列契约测试可直接执行 `npm test`，覆盖离线提交成功重试和 409 冲突保留。
