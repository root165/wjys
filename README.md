# 维检云枢（WJYS）

维检云枢是一套面向企业设备运维场景的 Web 管理端、移动巡检端和后端服务，覆盖设备台账、巡检计划、现场巡检、异常报修、维修工单、保养管理、备件库存、消息和运营看板等业务链路。

## 在线演示

<https://18080-agqyw6mnvglefbxc2b3b3iymau.instabox.tech/>

演示环境使用 HTTPS 访问，接口通过 `/api` 由网关转发。演示账号和角色说明见 [`deploy/README.md`](deploy/README.md)；首次登录后应立即修改初始密码。

## 功能范围

- 系统基础：用户、角色、菜单、部门、字典、操作日志和数据权限。
- 设备管理：设备分类、设备台账、设备参数、二维码和状态履历。
- 巡检管理：巡检点位、检查项、计划、任务领取、检查值校验、异常判定和异常转报修。
- 维修闭环：报修受理、工单派发、接单、完工、验收、设备状态联动和操作记录。
- 保养管理：保养计划、记录提交、照片绑定、审核归档和异常转报修。
- 备件管理：备件台账、库存预警、条件扣减、消耗流水和工单金额汇总。
- 数据与消息：设备统计、趋势、Pareto、设备排行、Excel 导出和消息已读。
- 移动巡检：巡检员登录、任务领取、扫码核验、拍照、草稿、离线提交和网络恢复同步。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2、Spring Security、MyBatis-Plus、Flyway |
| 数据与基础设施 | MySQL 8、Redis 7、S3/MinIO 兼容对象存储、Docker Compose |
| Web 管理端 | Vue 3、Vite、Element Plus、Pinia、Vue Router、ECharts |
| 移动端 | UniApp Vue 3、UniApp Storage、应用私有文件目录 |
| 测试 | JUnit 5、Spring Test、Testcontainers、Vitest、Node Test |

## 目录结构

```text
wjys/
├─ backend/       Spring Boot 后端服务、Flyway 迁移和测试
├─ frontend/      Vue Web 管理端
├─ mobile/        UniApp 移动巡检端
├─ deploy/        Docker、Nginx、健康检查和回滚脚本
├─ docs/          需求、设计、实施计划和业务规则文档
├─ docker-compose.yml
├─ .env.example
└─ AGENTS.md      项目开发与交付规约
```

## 快速启动

### 方式一：Docker Compose 一键启动

环境要求：Docker Engine、Docker Compose v2。

Linux/macOS：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，至少替换数据库密码、MinIO 密码、`JWT_SECRET` 和 `QR_SECRET`，不要使用模板默认值。然后执行：

```bash
docker compose --env-file .env -p wjys up -d --build
```

默认 Web 入口：<http://localhost:18080/>

查看服务状态：

```bash
docker compose --env-file .env -p wjys ps
```

服务器部署、HTTPS、备份和回滚说明见 [`deploy/README.md`](deploy/README.md)。

### 方式二：本地分别启动

后端要求 Java 17 和 Maven：

```bash
cd backend
mvn test
mvn spring-boot:run
```

Web 管理端要求 Node.js 18.18+ 且小于 23：

```bash
cd frontend
npm install
npm run test
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`，`/api` 和 `/ws` 会代理到后端 `http://localhost:8080`。

移动端要求 Node.js 22.12+：

```bash
cd mobile
npm install
npm test
npm run dev:h5
```

真机调试前，将 `mobile/.env.example` 复制为 `mobile/.env.production`，把 `VITE_API_BASE_URL` 设置为手机可以访问的后端地址；不能使用 `localhost`。Android 正式 APK/AAB 需要在 HBuilderX 中通过“发行 → 原生 App-云打包”生成，`npm run build:app` 只生成 App 资源，不是安装包。

## 配置与安全

- 真实环境变量只放在未提交的 `.env` 或 `.env.*` 文件中，提交模板时使用 `.env.example`。
- Android/iOS 签名证书、私钥、APK/AAB 和数据库备份禁止提交到 Git。
- 生产环境必须使用 HTTPS，并在服务器外层配置域名、防火墙和备份策略。
- 文件服务使用对象键保存稳定元数据，访问地址应使用临时签名 URL。
- 移动端离线数据使用 UniApp Storage；照片保存到应用私有目录，业务绑定并确认上传成功后才能清理。

## 数据库迁移

数据库变更统一放在 `backend/src/main/resources/db/migration/`，由 Flyway 按版本顺序执行。禁止修改已经执行过的迁移文件；新增结构应创建新的版本脚本。

## 测试门禁

后端：

```bash
cd backend
mvn test
```

Web：

```bash
cd frontend
npm run test
npm run build
```

移动端：

```bash
cd mobile
npm test
npm run build:h5
npm run build:app
```

真实 MySQL、Redis、对象存储、Android APK、扫码、相机、弱网和杀进程场景还需要在对应环境中补充联调验证。

## 开发规范

详细的模块边界、状态机、权限、迁移、测试和 Git 交付要求见 [`AGENTS.md`](AGENTS.md) 与 [`docs/04-项目代码实施计划-维检云枢.md`](docs/04-项目代码实施计划-维检云枢.md)。
