# 服务器部署说明

## 首次部署

1. 服务器安装 Docker Engine 和 Compose v2，开放 HTTP/HTTPS 端口；MySQL、Redis、MinIO 端口默认只绑定到服务器本机。
2. 上传项目后复制环境模板：`cp .env.example .env`，并修改数据库密码、MinIO 密码、JWT_SECRET、QR_SECRET。生产环境不要使用模板中的默认值。
3. 构建并启动：`docker compose --env-file .env -p wjys up -d --build`。
4. 查看状态：`docker compose --env-file .env -p wjys ps`；健康检查至少确认 `curl -f http://服务器地址/` 和 `curl -f http://服务器地址:8080/actuator/health` 返回成功。

Flyway 会在后端首次连接 MySQL 时自动执行 `backend/src/main/resources/db/migration` 下的迁移；MinIO 服务启动后，后端会自动创建配置的 bucket。

## 更新与回滚

发布新版本前设置唯一的 `IMAGE_TAG` 并重新构建镜像。确认新版本健康后保留旧镜像；出现问题时执行：

Linux 服务器可将 `.env` 中的 `IMAGE_TAG` 改回旧标签后执行 `docker compose --env-file .env -p wjys up -d --no-build backend frontend`；Windows 管理机可使用 `deploy/scripts/rollback.ps1`。

生产环境建议在服务器外层再配置 HTTPS、域名、备份和防火墙策略。APK/AAB 不属于 Compose 产物，需要使用 HBuilderX 的发行流程配置 AppID 和签名证书生成。
