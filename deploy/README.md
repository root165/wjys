# 服务器部署说明

## 首次部署

1. 服务器安装 Docker Engine 和 Compose v2；InstaBox 发布端口必须使用不小于 1024 的宿主机端口，本项目默认使用 `18080` 映射到 Nginx 容器的 `80`。MySQL、Redis、MinIO 和后端端口默认只绑定到服务器本机。
2. 上传项目后复制环境模板：`cp .env.example .env`，并修改数据库密码、MinIO 密码、JWT_SECRET、QR_SECRET。生产环境不要使用模板中的默认值。
3. 确认 `DATA_DIR=/data/wjys`，创建目录并设置权限：`mkdir -p /data/wjys/{mysql,redis,minio,backups} && chmod 700 /data/wjys`。
4. 构建并启动：`docker compose --env-file .env -p wjys up -d --build`。
5. 查看状态：`docker compose --env-file .env -p wjys ps`；执行 `./deploy/scripts/health-check.sh http://127.0.0.1:18080` 验证前端和后端健康状态。
6. 在 InstaBox 控制台发布宿主机 `18080` 端口，访问平台生成的 HTTPS 地址；不要发布 80、8080、3306、6379、9000 或 9001。

Flyway 会在后端首次连接 MySQL 时自动执行 `backend/src/main/resources/db/migration` 下的迁移；对象存储使用 `pgsty/silo`（S3/MinIO API 兼容），服务启动后，后端会自动创建配置的 bucket。原 `minio/minio` 固定镜像在本次远端拉取时返回 `pull access denied`，因此已切换为可拉取的兼容镜像。

## 初始化账号与权限

V13 迁移会幂等初始化以下演示账号，初始密码统一为 `password`，首次登录后请立即修改：

| 账号 | 角色 | 主要范围 |
| --- | --- | --- |
| `admin` | 系统管理员 | 账号、角色、菜单、组织、字典、日志及全部业务 |
| `equip_admin` | 设备管理员 | 设备台账、巡检计划、报修受理派工、工单验收、维保审核 |
| `inspector` | 巡检员 | 巡检任务、巡检填报、异常查看和异常转报修 |
| `repairer` | 维修工 | 维修工单接单/完工、备件查看和工单领用 |
| `manager` | 企业管理者 | 工作台、设备/巡检/报修/维保/备件统计查看 |
| `guest` | 游客 | 仅工作台，只能查看被明确授权的 `dashboard:view` |

角色权限同时在前端菜单和后端接口生效：隐藏菜单不代表安全边界，直达 URL 或手工调用接口仍会由后端返回 403。字典是系统统一选项表，例如设备状态、巡检结果和工单状态；各业务页面使用同一组标签和值，避免状态文字和统计口径不一致。

## 更新与回滚

发布新版本前设置唯一的 `IMAGE_TAG` 并重新构建镜像。确认新版本健康后保留旧镜像；出现问题时执行：

Linux 服务器可执行 `./deploy/scripts/rollback.sh <旧镜像标签>`；Windows 管理机可使用 `deploy/scripts/rollback.ps1`。每次发布前建议执行 `./deploy/scripts/backup.sh` 生成 MySQL 压缩备份。

生产环境建议在服务器外层再配置 HTTPS、域名、备份和防火墙策略。APK/AAB 不属于 Compose 产物，需要使用 HBuilderX 的发行流程配置 AppID 和签名证书生成。
