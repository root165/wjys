from pathlib import Path
import re
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / "Word复习资料"
OUT.mkdir(exist_ok=True)

SPECS = [
    {
        "name": "郑亮",
        "source": "01-郑亮-M05维保与M06备件.md",
        "out": "郑亮-维检云枢-M05维保与M06备件-考核复习-定稿.docx",
        "summary": [
            ("接口与输入输出设计", "维保接口的输入是计划/记录 DTO 与审核结论，输出为计划或记录 VO；备件领用以工单 ID 加多条领用明细为输入，输出是流水和更新后的工单成本。必须先说字段校验，再说写入的表和返回值。"),
            ("数据库设计", "M05 以 mt_plan 和 mt_record 分离规则与一次执行事实，(plan_id, maintain_date) 防止同计划同日重复。M06 用 wo_spare 保存当前库存，用 wo_spare_record 保存不可删除的正反向流水，金额字段为 decimal(18,2)。"),
            ("数据处理与关键代码", "consume 的关键是 WHERE stock_qty >= use_qty 的条件扣减；影响行数不是 1 立即报 409。扣库存、写流水、汇总 wo_order.spare_cost 在一个事务。审核通过时记录归档与计划 next_date 推进也在一个事务。"),
            ("业务流程", "维保计划 -> 提交记录 -> 审核归档或退回重提 -> 异常转报修；工单 -> 领用备件 -> 库存流水/成本汇总 -> 反向流水撤销。"),
        ],
        "codes": [
            ("M05 维保提交与审核服务", "backend/src/main/java/com/wjys/module/maintenance/service/MaintenanceRecordService.java", ["public MaintenanceRecordVO submit", "public MaintenanceRecordVO audit", "public MaintenanceRecordVO resubmit"]),
            ("M06 库存原子扣减与流水服务", "backend/src/main/java/com/wjys/module/spare/service/SpareConsumptionService.java", ["public List<SpareRecordVO> consume", "public SpareRecordVO reverse"]),
        ],
        "contracts": [
            ["创建维保计划", "POST /api/maintenance/plans", "SaveMaintenancePlanRequest：planCode(必填且≤64)、planName(必填≤160)、equipId(必填)、maintainType(0-3)、cycleType(0-3)、cycleValue(≥1)、content(≤500)、assigneeId、nextDate(必填)", "写 mt_plan；校验设备存在和计划编码唯一；返回 ApiResponse<MaintenancePlanVO>"],
            ["更新/启停计划", "PUT /api/maintenance/plans/{id}；PUT /api/maintenance/plans/{id}/status", "前者同保存 DTO；后者 ChangeMaintenancePlanStatusRequest 的 status", "更新计划或状态；不删除历史记录；返回 MaintenancePlanVO"],
            ["提交维保记录", "POST /api/maintenance/records", "SaveMaintenanceRecordRequest：planId(可空)、equipId、maintainDate、maintainContent(10-1000)、maintainResult(0/1)、cost(≥0)、downtimeMinutes(≥0)、fileIds(非空)、remark(≤500)", "写 mt_record、绑定 sys_file、设备转保养中；返回 MaintenanceRecordVO"],
            ["审核/异常转报修", "PUT /api/maintenance/records/{id}/audit；POST /api/maintenance/records/{id}/to-repair", "审核体 AuditMaintenanceRecordRequest：pass(必填)、remark(≤500)；转报修仅路径 id", "审核通过归档并推进 mt_plan.next_date；转报修调用 M04 RepairService，返回 RepairVO"],
            ["创建/编辑备件", "POST /api/spares；PUT /api/spares/{id}", "SaveSpareRequest：spareCode、spareName、specification、unit、stockQty(≥0)、warningQty(≥0)、unitPrice(≥0)、status", "写 wo_spare；编码唯一；返回 SpareVO"],
            ["库存调整", "PUT /api/spares/{id}/stock", "AdjustStockRequest：delta(必填，可正可负)、remark(必填)", "更新当前库存并保留操作审计；返回 SpareVO"],
            ["领用/撤销", "POST /api/work-orders/{orderId}/spares/consume；POST /api/work-orders/spare-records/{recordId}/reverse", "领用体 ConsumeSparesRequest.items 非空；每项 SpareUseRequest：spareId、quantity(正数)、remark。撤销体可选 remark", "条件扣库存、写 wo_spare_record、汇总 wo_order.spare_cost；撤销写反向流水而非删除"],
        ],
        "calls": [
            ["M01 -> M05/M06", "Controller 的 @PreAuthorize 和 WjysPrincipal 提供 maintenance/spare 权限与当前操作人；M05/M06 不自行实现认证。"],
            ["M05 -> M02", "MaintenanceRecordService.submit 调用 equipmentService.getActive 和 EquipmentStatusService.changeStatus，设备状态及 eq_status_log 由 M02 唯一维护。"],
            ["M05 -> M04", "MaintenanceRecordService.toRepair 调用 RepairService.createFromMaintenance(recordId,equipId,description,reporterId)，以 MAINTENANCE_RECORD + recordId 防重复。"],
            ["M04 -> M06", "维修工单 ID 是 SpareConsumptionService.consume 的输入；M06 校验工单状态后写流水并更新 wo_order.spare_cost，M04 不直接扣库存。"],
            ["M05/M06 -> M07", "M07 的 DashboardQueryMapper 只读维保、工单、库存事实做聚合；M07 不回写 mt_* 或 wo_spare*。"],
            ["M08 -> M05", "移动端的草稿/同步队列经服务端公开接口提交，照片绑定和重试仍复用同一规则，不产生第二套维保状态机。"],
        ],
        "tables": [
            ["mt_plan", "plan_code(唯一)、equip_id、maintain_type、cycle_type/cycle_value、assignee_id、next_date、status、version", "保养规则；next_date 是审核通过后推进的下次执行日期，version 服务并发控制"],
            ["mt_record", "record_code(唯一)、plan_id、equip_id、maintain_date、maintainer_id、maintain_content、maintain_result、cost、downtime_minutes、images、status、audit_user/audit_time", "一次维保执行事实；(plan_id,maintain_date) 唯一；审核后归档只读"],
            ["wo_spare", "spare_code(唯一)、stock_qty、warning_qty、unit_price、status、version", "当前可用库存和预警阈值；单价为 decimal(18,2)"],
            ["wo_spare_record", "order_id、spare_id、source_record_id、direction、quantity、unit_price、amount、operator_id、remark", "不可删除的正/反向库存流水；source_record_id 指向被撤销的原流水"],
        ],
        "flows": [
            ["维保提交与审核", "校验设备和计划 -> 查 (plan_id,maintain_date) 去重 -> 插入 mt_record -> fileService.bindAll -> M02 状态服务转保养中 -> 审核条件更新 status=0 -> 通过则推进 next_date/恢复状态，退回则 status=2。", "M05 维保提交与审核服务"],
            ["工单领用备件", "校验 wo_order 存在且允许领用 -> 校验 wo_spare 启用和 quantity>0 -> 带 stock_qty>=quantity 的条件扣减 -> 写 wo_spare_record -> 汇总有效流水到 wo_order.spare_cost。任一步异常回滚。", "M06 库存原子扣减与流水服务"],
        ],
        "qas": [
            ["为什么归档记录不能改，退回记录却能重提？", "归档是审核确认后的正式设备履历，允许改会破坏审计可信度，因此 status=1 只读；退回 status=2 表示审核未通过，允许经过修正后转回待审核 status=0。代码通过 audit/resubmit 的状态判断和条件更新执行该边界。"],
            ["审核推进 nextDate 失败怎样保证不乱？", "审核记录、计划 next_date 推进、设备状态恢复在 @Transactional 中；任一步抛异常会回滚，既不会出现“已归档但下次日期没推进”，也不会出现“日期推进但记录仍待审核”。"],
            ["为什么不能 DELETE wo_spare_record？", "该表是审计流水，删除会丢失已领用、撤销人和金额历史。撤销时以 source_record_id 指回原流水，写相反 direction 的记录并回补库存，保留完整账实变化。"],
            ["请从领用接口讲到 409。", "Controller 收 orderId 和 items，Service 先验证工单/备件/正数量；SQL 以 stock_qty>=quantity 条件更新。库存不足或并发抢先扣减时影响行数不是 1，抛 BusinessException(HttpStatus.CONFLICT)，统一异常返回 409；成功后在同一事务写流水和成本。"],
            ["增加采购入库模块要改什么？", "新增 Flyway 迁移建立采购单、入库单或库存流水类型表；新增采购/入库 DTO、VO、Controller、Service 和权限菜单；入库使用事务增加库存并写可审计流水。旧迁移和已发生的领用流水不改写。"],
        ],
    },
    {
        "name": "高浩然",
        "source": "02-高浩然-M02设备与M04维修.md",
        "out": "高浩然-维检云枢-M02设备与M04维修-考核复习-定稿.docx",
        "summary": [
            ("接口与输入输出设计", "设备新增/编辑输入 SaveEquipmentRequest，输出 EquipmentListVO；状态变更输入 status、sourceType、sourceId、reason，输出是设备状态与履历。工单派工、完工、验收分别使用独立 DTO，避免一个万能请求体绕过状态机。"),
            ("数据库设计", "eq_equipment 用 equip_code 唯一、deleted 逻辑删除、version 并发控制；eq_param 与 eq_status_log 分离扩展参数和状态审计。wo_repair 与 wo_order 分离申报和执行，repair_id 唯一使一报修仅有一工单。"),
            ("数据处理与关键代码", "状态变更的 UPDATE 带旧 status 条件并递增 version；受理、派工、接单、完工、验收均用状态条件更新。验收把工单、报修、来源异常、设备状态的改变放进同一事务。"),
            ("业务流程", "设备建档 -> 巡检/维保发现异常 -> 报修 -> 受理生成工单 -> 派工 -> 接单 -> 完工 -> 验收或退回 -> 设备状态恢复与统计。"),
        ],
        "codes": [
            ("M02 设备状态条件更新和履历", "backend/src/main/java/com/wjys/module/equipment/service/EquipmentStatusService.java", ["public void changeStatus"]),
            ("M04 报修受理与来源唯一控制", "backend/src/main/java/com/wjys/module/repair/service/RepairService.java", ["public RepairVO createFromAbnormal", "public WorkOrderVO accept"]),
            ("M04 工单派工验收状态机", "backend/src/main/java/com/wjys/module/repair/service/WorkOrderService.java", ["public WorkOrderVO dispatch", "public WorkOrderVO accept"]),
        ],
        "contracts": [
            ["设备分页/详情", "GET /api/equipment/equipments；GET /api/equipment/equipments/{id}", "列表参数 keyword、categoryId、status、pageNum(默认1)、pageSize(默认20)；详情仅路径 id", "列表返回 PageResult<EquipmentListVO>；详情返回 EquipmentDetailVO，含参数与状态履历"],
            ["创建设备/编辑", "POST /api/equipment/equipments；PUT /api/equipment/equipments/{id}", "SaveEquipmentRequest：categoryId、equipCode(必填)、equipName(必填)、model、manufacturer、location、responsibleUserId、purchaseDate、warrantyExpire、description", "分类启用校验、equipCode 唯一；写 eq_equipment；返回 EquipmentListVO"],
            ["状态/参数/删除", "PUT /api/equipment/equipments/{id}/status；PUT /api/equipment/equipments/{id}/params/{paramId}；DELETE /api/equipment/equipments/{id}", "状态体 ChangeEquipmentStatusRequest：status、sourceType、sourceId、reason；参数体 SaveParamRequest；删除仅路径 id", "状态条件更新并写 eq_status_log；参数归属校验；仅报废且无活跃任务/工单时逻辑删除"],
            ["异常转报修/受理", "POST /api/repairs/from-abnormal/{abnormalId}；PUT /api/repairs/{id}/accept", "仅路径 abnormalId 或 repair id；当前用户从 WjysPrincipal 获取", "写 wo_repair，来源唯一；受理创建 wo_order、报修 0->1，返回 RepairVO/WorkOrderVO"],
            ["派工/接单/完工", "PUT /api/work-orders/{id}/dispatch、start、finish", "DispatchRequest：assigneeId(必填)、remark、expectFinishTime；start 无体；FinishRequest：finishReason、repairPlan、finishResult(均必填)、downtimeMinutes(≥0)、fileIds", "状态 0->1->2；写派工/完工字段和 wo_order_log，返回 WorkOrderVO"],
            ["验收/退回", "PUT /api/work-orders/{id}/accept；PUT /api/work-orders/{id}/reject", "accept 可选 AcceptRequest.remark；reject 为 RejectRequest.reason(必填)", "验收关闭工单/报修/来源异常、调用 M02 恢复设备；退回记录原因并回到维修处理"],
        ],
        "calls": [
            ["M01 -> M02/M04", "EquipmentController、RepairController、WorkOrderController 用 @PreAuthorize 校验 equipment/repair 权限；WjysPrincipal 传递操作人 ID。"],
            ["M03 -> M04", "AbnormalService.toRepair 调用 M04 的公开 RepairService.createFromAbnormal；M04 以 (source_type,source_id) 唯一约束确保同异常只建一张报修。"],
            ["M05 -> M04", "MaintenanceRecordService.toRepair 调用 RepairService.createFromMaintenance；M04 复用相同来源唯一策略。"],
            ["M04 -> M02", "WorkOrderService.dispatch/accept 通过 EquipmentStatusService.changeStatus 改维修中/恢复状态，绝不直接更新 eq_equipment.status。"],
            ["M06 <-> M04", "M06 以 wo_order.id 领用备件、写 wo_order.spare_cost；M04 只提供工单状态和成本承载，不维护库存。"],
            ["M04 -> M07/M08", "验收后的故障和停机事实供 M07 聚合；M08 的接单/完工操作复用 M04 Service 的权限与状态机。"],
        ],
        "tables": [
            ["eq_equipment", "category_id、equip_code(唯一)、equip_name、status、responsible_user_id、deleted、version", "设备主档；deleted 逻辑删除；version 与旧状态条件更新共同防并发"],
            ["eq_param", "equip_id、param_name、param_value、unit、sort_no", "可扩展设备技术参数，避免为不同设备类别反复加列"],
            ["eq_status_log", "equip_id、from_status、to_status、source_type、source_id、reason", "记录 M03/M04/M05 等来源引起的状态变化，形成审计履历"],
            ["wo_repair / wo_order / wo_order_log", "报修来源(source_type,source_id 唯一)；工单 repair_id 唯一、assignee_id、status、finish_*、downtime_minutes、spare_cost；日志 action/operator_id", "申报、执行、时间线三层分离，保证一来源一报修、一报修一工单和全过程可追责"],
        ],
        "flows": [
            ["设备状态变更", "解析枚举 -> 查询活动设备 -> WHERE id AND old status AND deleted=0 条件更新并 version+1 -> 影响行数非1返回409 -> 插入 eq_status_log。", "M02 设备状态条件更新和履历"],
            ["异常到工单验收", "M03/M05 异常 -> 创建 wo_repair -> 受理创建 wo_order -> 派工并转设备维修中 -> 指定维修人接单 -> 完工写原因/方案/停机 -> 验收关闭工单/报修/异常、恢复设备。", "M04 报修受理与来源唯一控制"],
            ["派工和验收状态机", "派工校验当前工单状态和维修人 -> 记录日志/设备状态；验收校验待验收状态 -> 关闭关联单据并刷新设备/统计；任一步失败事务回滚。", "M04 工单派工验收状态机"],
        ],
        "qas": [
            ["为什么设备状态不能由工单表直接改？", "设备状态是跨巡检、维修、维保共享的权威数据。若 M04 直接 SQL 更新会漏写 eq_status_log，也无法统一做并发和报废状态校验。因此只能调用 M02 EquipmentStatusService。"],
            ["为什么报修和工单要分表？", "报修表示故障申报，含来源和描述；工单表示受理后的执行过程，含维修人、方案、停机、验收和成本。两者状态、参与者、生命周期不同，分表避免大量空字段并使一报修一工单约束清晰。"],
            ["两人同时受理同一报修会怎样？", "两人都可能先读到待受理，但更新报修时以 status=0 为条件。第一个成功变为已派单；第二个影响行数为0并收到409，所以不会生成两张工单。"],
            ["验收为什么要事务？", "验收同时涉及 wo_order、wo_repair、ins_abnormal、eq_equipment/eq_status_log 及统计触发。没有事务会出现工单已完成但设备仍维修中等半完成状态；@Transactional 保证原子提交。"],
            ["增加外部维修供应商如何改？", "M01 新增供应商角色/权限和数据范围；新增 supplier 主表及 order_supplier 关系或 wo_order.supplier_id；M04 增派工和供应商待办接口，限制只能看自己被派工单；用 Flyway 新增迁移并补越权/禁用/重复派工测试。"],
        ],
    },
    {
        "name": "张万健",
        "source": "03-张万健-M01M03M07M08与横切设计.md",
        "out": "张万健-维检云枢-M01M03M07M08与横切设计-考核复习-定稿.docx",
        "summary": [
            ("接口与输入输出设计", "认证接口输入登录/刷新 DTO，输出 TokenPair 和身份权限；巡检提交输入 submitNo、客户端时间、离线标志和任务项数组，输出 TaskSubmitVO；移动同步输入 cursor 或批次队列，输出增量快照或逐项 confirmed/conflict/rejected/retry。"),
            ("数据库设计", "sys_user 的 token_version 用于使旧登录态失效；ins_task 的 (plan_id, plan_time) 和 submit_no 是生成与提交的双重唯一保障；ins_task_item 存检查规则快照；stat_equip_daily 按 (stat_date, equip_id) 聚合。"),
            ("数据处理与关键代码", "生成任务使用 Redis token 锁加数据库唯一约束；提交服务端复算阈值、校验必拍、写异常、写提交哈希并整体事务；看板 refresh 日聚合 upsert 后失效缓存；移动 push 单条复用巡检提交服务。"),
            ("业务流程", "登录与权限加载 -> 计划生成任务快照 -> 现场领取/离线填写 -> 上传绑定/幂等 push -> 异常转维修 -> 日统计、缓存和导出。"),
        ],
        "codes": [
            ("M01 登录令牌处理", "backend/src/main/java/com/wjys/module/system/service/AuthService.java", ["public TokenPairVO login", "public TokenPairVO refresh"]),
            ("M03 任务生成和幂等提交", "backend/src/main/java/com/wjys/module/inspection/service/TaskGenerateService.java", ["public InsTask generate"]),
            ("M03 服务端校验和异常写入", "backend/src/main/java/com/wjys/module/inspection/service/TaskSubmitService.java", ["public TaskSubmitVO submit"]),
            ("M07 日聚合与缓存失效", "backend/src/main/java/com/wjys/module/dashboard/service/DashboardService.java", ["public void refresh"]),
            ("M08 批量同步复用业务服务", "backend/src/main/java/com/wjys/module/mobile/service/MobileSyncService.java", ["public MobileSyncPushVO push"]),
        ],
        "contracts": [
            ["登录/刷新", "POST /api/auth/login；POST /api/auth/refresh", "LoginRequest：username、password；RefreshTokenRequest：refreshToken", "校验 BCrypt、用户启用和 tokenVersion；返回 TokenPairVO；刷新时撤销旧 jti 并签发新 Token"],
            ["巡检计划", "POST /api/inspection/plans；PUT /api/inspection/plans/{id}", "CreatePlanRequest/UpdatePlanRequest：planCode、planName、cycleType、cycleValue、startDate、endDate、executeTime、assignmentMode、responsibleUserId、assignmentDeptId、pointIds 等", "校验周期、点位、派发对象；写 ins_plan/ins_plan_point；返回 PlanVO"],
            ["生成/领取", "POST /api/inspection/plans/{id}/generate；PUT /api/inspection/tasks/{id}/receive", "生成体 GenerateTaskRequest：planTime；领取仅路径 taskId", "写 ins_task 和 ins_task_item 快照；领取条件更新 0->1，返回 TaskVO"],
            ["提交巡检", "POST /api/inspection/tasks/{id}/submit", "TaskSubmitRequest：submitNo、clientSubmitTime、offlineFlag、items；SubmitItemRequest：taskItemId、checkResult、checkValue、remark、checkTime、fileIds", "校验执行人/完整项/必拍/扫码/幂等；写任务项、异常、提交哈希、文件绑定；返回 TaskSubmitVO"],
            ["移动同步", "GET /api/mobile/sync/pull；POST /api/mobile/sync/push；PUT /api/mobile/messages/{id}/read", "pull：cursor、lastSyncTime、modules；push：含固定 submitNo 的批次项目；read 仅路径消息 ID", "pull 返回快照增量和 cursor；push 每条 confirmed/conflict/rejected/retry，单条失败不阻断其它项"],
            ["看板/导出", "GET /api/dashboard/overview、trend、pareto、ranking；导出接口", "分类、开始/结束日期、维度、topN 等查询参数", "返回 Dashboard*VO；日期范围最长365天，缓存10分钟，导出为 XLSX"],
        ],
        "calls": [
            ["M01 -> 全模块", "AuthService/JWT 提供身份；Controller 的 @PreAuthorize 执行接口授权；RoleService 的菜单关系驱动 Web 路由和按钮展示。"],
            ["M03 -> M02", "TaskGenerateService 读取设备/点位并将设备编码、阈值等写入 ins_task_item 快照；状态改变仍经 M02 Service。"],
            ["M03 -> M04", "AbnormalService.toRepair 调用 RepairService.createFromAbnormal，异常来源唯一避免离线重试重复建报修。"],
            ["M08 -> M03/M04/M05", "MobileSyncService.push 按队列逐条调用既有提交 Service；移动端不直接写业务表、不复制状态机。"],
            ["M07 <- M02~M06", "DashboardQueryMapper 只读设备、巡检、工单、备件等事实；DashboardService.refresh upsert stat_equip_daily 后失效缓存。"],
            ["Redis/消息横切", "任务生成锁用随机 token 释放校验所有权；统计使用 Redis/内存 TTL；消息在业务动作后持久化到 sys_message。"],
        ],
        "tables": [
            ["sys_user / sys_role / 关联表", "username、password、status、token_version；role_code、data_scope；user_role/role_menu 多对多", "认证、RBAC、数据范围；token_version 让禁用/改密后的旧 Token 失效"],
            ["ins_plan / ins_task", "计划周期/execute_time/assignment_mode/pointIds；任务 plan_id、plan_time、assignee_id、status、submit_no、submit_hash", "计划是规则，任务是执行实例；(plan_id,plan_time) 防重复任务，submit_no 防重复提交"],
            ["ins_task_item / ins_abnormal", "任务项保存设备、点位、阈值、单位、必拍、填写值与 fileIds 快照；异常关联 task/task_item/equip/point", "历史不受配置变更影响；异常是调用 M04 报修的业务来源"],
            ["stat_equip_daily / sys_message", "(stat_date,equip_id) 唯一，任务/异常/故障/停机/MTBF/MTTR/健康分；消息 user_id/biz_type/biz_id/read_flag", "日聚合提升看板查询效率；消息支持移动端增量拉取和已读状态"],
        ],
        "flows": [
            ["认证与授权", "登录查用户 -> BCrypt 比较 -> 检查 status -> 记录最后登录时间 -> 以 tokenVersion 签发 Token；刷新时比较 tokenVersion 并撤销旧 jti。", "M01 登录令牌处理"],
            ["生成任务快照", "校验计划日期/周期 -> Redis token 锁 -> 数据库 (plan_id,plan_time) 唯一兜底 -> 写 ins_task -> 遍历点位/检查项写 ins_task_item 快照。", "M03 任务生成和幂等提交"],
            ["巡检提交", "校验任务状态/执行人/submitNo -> 重算阈值和必拍 -> 更新 ins_task_item -> 写 ins_abnormal -> 更新任务提交哈希与统计 -> 绑定文件；整个事务失败回滚。", "M03 服务端校验和异常写入"],
            ["看板聚合与移动同步", "按设备按日查询业务事实 -> upsert stat_equip_daily -> 删除 dashboard 缓存前缀；移动 push 保持 submitNo，逐条映射 confirmed/conflict/rejected/retry。", "M07 日聚合与缓存失效"],
        ],
        "qas": [
            ["为什么 Redis 锁后仍需要数据库唯一键？", "Redis 锁可能因超时、进程崩溃或网络异常失效；数据库 uk_ins_task_plan_time 是最终一致性底线。两者是性能层去重和数据层兜底的双保险。"],
            ["为什么离线重试不能换 submitNo？", "服务端用 submitNo 和 submitHash 识别同一业务提交。换号会被当作新提交，可能重复更新任务、重复异常和重复报修；固定号才能安全重试。"],
            ["为什么看板要只读和日聚合？", "业务单据的写入规则属于原模块，看板不能反向篡改；日聚合将高成本的多表统计转换成按设备按日读取，口径可固化并适合缓存。"],
            ["为什么 M08 不能复制一套巡检规则？", "若移动端另实现阈值、状态和幂等规则，Web 与 App 会产生不同结果。M08 只管存储、队列、照片和网络，提交复用服务端 TaskSubmitService 才保证唯一业务权威。"],
            ["巡检提交的完整异常处理是什么？", "参数或枚举问题返回400；未登录401；非巡检员/非执行人403；任务不存在404；重复 submitNo、状态不允许、并发领取、同号不同载荷返回409；网络/5xx 由移动端保留队列退避重试。"],
        ],
    },
]

def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:fill'), fill)
    tc_pr.append(shd)

def set_cell_margins(cell, top=90, start=90, bottom=90, end=90):
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcMar = tcPr.first_child_found_in('w:tcMar')
    if tcMar is None:
        tcMar = OxmlElement('w:tcMar')
        tcPr.append(tcMar)
    for m, v in [('top', top), ('start', start), ('bottom', bottom), ('end', end)]:
        node = tcMar.find(qn(f'w:{m}'))
        if node is None:
            node = OxmlElement(f'w:{m}')
            tcMar.append(node)
        node.set(qn('w:w'), str(v)); node.set(qn('w:type'), 'dxa')

def set_repeat_table_header(row):
    trPr = row._tr.get_or_add_trPr()
    tblHeader = OxmlElement('w:tblHeader'); tblHeader.set(qn('w:val'), 'true'); trPr.append(tblHeader)

def prevent_row_split(row):
    trPr = row._tr.get_or_add_trPr()
    node = OxmlElement('w:cantSplit')
    trPr.append(node)

def font(run, name='Microsoft YaHei', size=10.5, bold=False, color=None):
    run.font.name = name; run._element.rPr.rFonts.set(qn('w:eastAsia'), name)
    run.font.size = Pt(size); run.bold = bold
    if color: run.font.color.rgb = RGBColor(*color)

def style_doc(doc):
    sec = doc.sections[0]
    sec.page_width = Inches(8.5); sec.page_height = Inches(11)
    sec.top_margin = Inches(.68); sec.bottom_margin = Inches(.65)
    sec.left_margin = Inches(.68); sec.right_margin = Inches(.68)
    styles = doc.styles
    styles['Normal'].font.name = 'Microsoft YaHei'; styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), 'Microsoft YaHei')
    styles['Normal'].font.size = Pt(10.5)
    styles['Normal'].paragraph_format.space_after = Pt(5)
    styles['Normal'].paragraph_format.line_spacing = 1.22
    for style_name, size in [('Title', 20), ('Heading 1', 15), ('Heading 2', 12.5), ('Heading 3', 11)]:
        s = styles[style_name]; s.font.name = 'Microsoft YaHei'; s._element.rPr.rFonts.set(qn('w:eastAsia'), 'Microsoft YaHei'); s.font.size = Pt(size); s.font.bold = True; s.font.color.rgb = RGBColor(0,0,0)
    # python-docx 默认 Title 样式可能携带蓝色下边框；复习资料使用纯文字标题。
    title_ppr = styles['Title']._element.get_or_add_pPr()
    title_border = title_ppr.find(qn('w:pBdr'))
    if title_border is not None:
        title_ppr.remove(title_border)
    footer = sec.footer.paragraphs[0]; footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = footer.add_run('维检云枢 后期考核复习资料'); font(r, size=8, color=(90,90,90))

def add_title(doc, text, subtitle):
    p = doc.add_paragraph(style='Title'); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text); font(r, size=20, bold=True)
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(subtitle); font(r, size=10, color=(80,80,80))
    doc.add_paragraph('本资料按“答案、字段、表设计、代码、流程、异常”组织。答题时先给结论，再结合实际类名、接口和表名说明实现。')

def add_table(doc, rows):
    if not rows: return
    tbl = doc.add_table(rows=1, cols=len(rows[0])); tbl.alignment = WD_TABLE_ALIGNMENT.CENTER; tbl.style = 'Table Grid'
    hdr = tbl.rows[0]
    set_repeat_table_header(hdr)
    prevent_row_split(hdr)
    for i, value in enumerate(rows[0]):
        cell = hdr.cells[i]; cell.text = ''
        p = cell.paragraphs[0]; r = p.add_run(value); font(r, size=8.7, bold=True, color=(255,255,255))
        set_cell_shading(cell, '1F4E78'); set_cell_margins(cell); cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
    for idx, row in enumerate(rows[1:]):
        new_row = tbl.add_row(); prevent_row_split(new_row); cells = new_row.cells
        for i, value in enumerate(row):
            cell = cells[i]; cell.text = ''
            p = cell.paragraphs[0]; r = p.add_run(value); font(r, size=8.4)
            set_cell_margins(cell); cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            if idx % 2 == 1: set_cell_shading(cell, 'F2F6FA')
    doc.add_paragraph()

def parse_table(lines):
    rows=[]
    for line in lines:
        if re.match(r'^\|\s*:?-+', line): continue
        cells=[re.sub(r'`', '', x.strip()) for x in line.strip().strip('|').split('|')]
        rows.append(cells)
    return rows

def add_markdown(doc, md):
    lines = md.splitlines(); i=0
    while i < len(lines):
        line=lines[i]
        if not line.strip(): i += 1; continue
        if line.startswith('```'):
            buf=[]; i += 1
            while i < len(lines) and not lines[i].startswith('```'):
                buf.append(lines[i]); i += 1
            p=doc.add_paragraph(); p.paragraph_format.left_indent=Inches(.18); p.paragraph_format.right_indent=Inches(.18)
            for idx, b in enumerate(buf):
                r=p.add_run(b + ('\n' if idx < len(buf)-1 else '')); font(r, name='Consolas', size=8.2)
            i += 1; continue
        if line.startswith('|'):
            tbl=[]
            while i < len(lines) and lines[i].startswith('|'):
                tbl.append(lines[i]); i += 1
            add_table(doc, parse_table(tbl)); continue
        m=re.match(r'^(#{1,3})\s+(.*)', line)
        if m:
            level=len(m.group(1)); text=re.sub(r'`', '', m.group(2))
            doc.add_paragraph(text, style=f'Heading {level}'); i += 1; continue
        if line.startswith('- '):
            p=doc.add_paragraph(style='List Bullet'); p.add_run(re.sub(r'`', '', line[2:])); i += 1; continue
        if re.match(r'^\d+\. ', line):
            p=doc.add_paragraph(style='List Number'); p.add_run(re.sub(r'`', '', re.sub(r'^\d+\. ', '', line))); i += 1; continue
        p=doc.add_paragraph();
        text=re.sub(r'\*\*(.*?)\*\*', r'\1', re.sub(r'`', '', line))
        p.add_run(text); i += 1

def extract_method(path, marker, max_lines=70):
    text=(ROOT/path).read_text(encoding='utf-8')
    idx=text.find(marker)
    if idx < 0: return f'未找到代码标记：{marker}'
    start=text.rfind('\n', 0, idx)
    tail=text[start+1:].splitlines()
    out=[]; depth=0; opened=False
    for line in tail:
        out.append(line)
        if '{' in line: depth += line.count('{'); opened=True
        if '}' in line: depth -= line.count('}')
        if opened and depth <= 0: break
        if len(out)>=max_lines: out.append('    // 代码节选到此，完整实现见源文件'); break
    return '\n'.join(out)

def add_code(doc, title, path, markers):
    doc.add_paragraph(title, style='Heading 3')
    p=doc.add_paragraph(); r=p.add_run(f'真实代码位置：{path}'); font(r, size=9, color=(80,80,80))
    # 每个关键流程仅展示一个方法节选。流程文字已覆盖完整状态机；代码只证明
    # 输入校验、条件更新/写入和异常返回，避免把整段 Service 复制进复习材料。
    for marker in markers[:1]:
        doc.add_paragraph(marker, style='Heading 3')
        lines=extract_method(Path(path), marker, max_lines=140).splitlines()
        if len(lines) > 14:
            # 代码仅作流程证据：保留入口校验和收口的条件更新/返回，
            # 逐项字段赋值、循环明细等由紧邻的流程文字说明。
            lines=lines[:8] + ['        // ... 逐项校验、写入明细与关联记录（已省略） ...'] + lines[-5:]
        code='\n'.join(lines)
        p=doc.add_paragraph(); p.paragraph_format.left_indent=Inches(.12); p.paragraph_format.right_indent=Inches(.08)
        for n, line in enumerate(code.splitlines()):
            r=p.add_run(f'{n+1:>2}  {line}\n'); font(r, name='Consolas', size=7.4)

def add_audited_implementation(doc, spec):
    doc.add_page_break()
    doc.add_paragraph('B 个人负责模块完整复习资料', style='Heading 1')
    doc.add_paragraph(f'{spec["name"]}。本区只讲本人负责模块的实现：表、接口、调用方向、核心业务流程和异常处理。每个负责模块的流程完整保留；代码仅保留两段短节选，用来证明关键校验、条件更新和事务收口。')
    doc.add_paragraph('B.1 本人模块定位 作用与重要性', style='Heading 2')
    add_table(doc, [['考核点', '可直接作答的答案']] + [[a,b] for a,b in spec['summary']])
    doc.add_paragraph('B.2 个人负责模块的关键数据库设计', style='Heading 2')
    add_table(doc, [['表', '关键字段和约束', '设计作用与答题解释']] + spec['tables'])
    doc.add_paragraph('B.3 接口与输入输出设计能力', style='Heading 2')
    add_table(doc, [['能力', '实际路径与方法', '输入字段及校验', '输出效果与写入']] + spec['contracts'])
    doc.add_paragraph('回答模板：Controller 用 @Valid 校验 DTO、用 @PreAuthorize 校验权限、从 WjysPrincipal 取操作人；Service 负责状态机与事务；成功统一封装为 ApiResponse<VO>，非法状态、重复提交或并发失败返回 409。')
    doc.add_paragraph('B.4 本人模块与其他模块的调用方向', style='Heading 2')
    add_table(doc, [['调用方向', '实际调用关系和职责边界']] + spec['calls'])
    doc.add_paragraph('B.5 本人模块核心业务流程 数据处理与关键代码', style='Heading 2')
    code_map={title:(path,markers) for title,path,markers in spec['codes']}
    key_code_titles = {'M05 维保提交与审核服务', 'M06 库存原子扣减与流水服务', 'M02 设备状态条件更新和履历', 'M04 工单派工验收状态机', 'M03 任务生成和幂等提交', 'M03 服务端校验和异常写入'}
    for flow, steps, code_title in spec['flows']:
        doc.add_paragraph(flow, style='Heading 3')
        p=doc.add_paragraph(); r=p.add_run('流程答案：'); font(r, bold=True); p.add_run(steps)
        path,markers=code_map[code_title]
        if code_title in key_code_titles:
            add_code(doc, f'关键代码证据：{code_title}', path, markers)
        else:
            p=doc.add_paragraph(); r=p.add_run('实现定位：'); font(r, bold=True); p.add_run(f'{path}；本材料只保留两段关键业务代码，避免代码块淹没复习重点。')
    doc.add_paragraph('B.6 面试式自测题 标准答案', style='Heading 2')
    add_table(doc, [['题目', '标准回答']] + spec['qas'])

def add_public_system_section(doc):
    doc.add_paragraph('A 全项目公共复习区 本页内置 考前先过一遍', style='Heading 1')
    doc.add_paragraph('这是每位成员都应掌握的公共答案，置于个人模块之前且不依赖其它文档。它回答项目背景、用户、模块、全局闭环、架构、公共表设计和异常边界；后续 B 区再深入本人模块的实现。')
    doc.add_paragraph('A.1 项目背景 目标与面向用户', style='Heading 2')
    doc.add_paragraph('维检云枢面向企业设备运维场景。传统台账、纸质巡检、故障报修和备件领用相互脱节，常见问题是设备状态不一致、异常没有闭环、维修过程不可追溯、库存账实不清。系统以设备为业务主线，把台账、巡检、报修维修、维保、备件和统计接成可审计闭环。')
    add_table(doc, [['用户角色', '主要需求', '系统能力与权限边界'],
        ['系统管理员', '维护账号、角色、菜单、部门、字典与日志', 'M01；管理系统配置，不领取和提交巡检任务'],
        ['设备管理员', '维护设备台账、配置巡检、受理派工、验收与维保审核', 'M02/M03/M04/M05；可管理本权限范围内业务'],
        ['巡检员', '在现场领取任务、扫码、填写检查项、上传照片和离线提交', 'M03/M08；只能执行分配或领取的任务'],
        ['维修工', '接单、登记维修过程、完工和备件消耗', 'M04/M06；不能越权验收或管理台账'],
        ['企业管理者', '查看设备健康、故障趋势、排行和预警', 'M07；以只读分析为主']])
    doc.add_paragraph('A.2 功能需求 模块边界与分工', style='Heading 2')
    add_table(doc, [['模块', '功能需求和关键产物', '负责人', '直接依赖或被调用关系'],
        ['M01 系统与基础数据', '登录、刷新、用户、角色、菜单、部门、字典、操作日志、数据范围；sys_*', '张万健', '向全部模块提供身份、权限和操作人'],
        ['M02 设备档案', '分类、设备、参数、二维码、状态履历；eq_*', '高浩然', '被 M03/M04/M05 调用；设备状态唯一入口'],
        ['M03 巡检', '点位、检查项、计划、任务快照、领取、提交、异常；ins_*', '张万健', '读 M02；异常调用 M04；M08 复用其提交规则'],
        ['M04 报修维修', '报修、受理、派工、接单、完工、验收、日志；wo_repair/wo_order', '高浩然', '接收 M03/M05 异常；调用 M02 改状态；向 M06 提供工单'],
        ['M05 维保', '保养计划、记录、审核归档、异常转报修；mt_*', '郑亮', '调用 M02 状态服务和 M04 报修服务'],
        ['M06 备件', '备件台账、预警、库存调整、工单领用和流水；wo_spare*', '郑亮', '以 M04 工单为领用对象，更新工单备件成本'],
        ['M07 看板', '日统计、总览、趋势、Pareto、排行、导出；stat_equip_daily', '张万健', '只读聚合 M02-M06，不能反写业务'],
        ['M08 移动协同', '任务快照、草稿、离线队列、照片、pull/push、消息', '张万健', '调用 M03/M04/M05 公开 Service，不复制业务规则']])
    doc.add_paragraph('A.3 全局业务闭环与调用方向', style='Heading 2')
    doc.add_paragraph('流程一 巡检到维修：M01 鉴权 -> M03 计划按执行时刻生成任务快照 -> 巡检员领取 -> TaskSubmitService 校验完整项、扫码、必拍和阈值 -> 写 ins_abnormal -> AbnormalService 调用 M04 RepairService.createFromAbnormal -> 受理创建 wo_order -> M04 派工/接单/完工/验收 -> M04 调用 M02 EquipmentStatusService 恢复设备 -> M07 聚合故障与停机事实。')
    doc.add_paragraph('流程二 维保到归档：M05 提交 mt_record 和文件绑定 -> 调用 M02 状态服务进入保养中 -> 审核通过推进 mt_plan.next_date 并归档只读，退回后可重提 -> 发现异常时 M05 调用 M04 RepairService.createFromMaintenance。')
    doc.add_paragraph('流程三 工单到备件成本：M04 工单进入允许领用状态 -> M06 SpareConsumptionService 校验工单、备件和数量 -> SQL 条件扣减 wo_spare.stock_qty -> 写 wo_spare_record -> 汇总写 wo_order.spare_cost；撤销通过反向流水回补，不删除历史。')
    doc.add_paragraph('流程四 移动离线：M08 pull 获取任务/消息快照和 cursor -> 页面写草稿和固定 submitNo 队列 -> 照片先留私有目录 -> 联网上传绑定 fileId -> push 逐条复用 M03 提交服务 -> confirmed 清理、409 冲突交人工、网络/5xx 退避。')
    doc.add_paragraph('A.4 公共架构 数据库与异常处理能力', style='Heading 2')
    add_table(doc, [['考察点', '系统实现与可直接回答'],
        ['分层架构', '用户层为 Vue Web 与 UniApp；接入层为 Controller、JWT、@Valid、@PreAuthorize、ApiResponse；业务层为 Service、状态机和 @Transactional；数据层为 Mapper、MySQL/Flyway、Redis/内存 TTL、对象文件元数据。DTO 负责请求校验，VO 负责输出，Entity 不直接做接口入出参。'],
        ['核心数据库设计', 'eq_equipment 以 equip_code 唯一、status/deleted/version 管生命周期；ins_task 用 (plan_id,plan_time) 与 submit_no 双唯一并配 ins_task_item 快照；wo_repair 以 (source_type,source_id) 唯一，wo_order 以 repair_id 唯一；mt_record 用 (plan_id,maintain_date) 唯一；wo_spare_record 为不可删除流水；stat_equip_daily 用 (stat_date,equip_id) 唯一日聚合。'],
        ['并发与事务', '状态流转带旧状态条件，库存扣减带 stock_qty>=quantity 条件；影响行数非1返回409。巡检提交、验收、维保审核、备件领用都以 @Transactional 包住跨表写入，失败整体回滚。'],
        ['接口输入输出', '统一 /api、JSON camelCase、ApiResponse(code/message/data/traceId)。列表为 pageNum/pageSize 与 PageResult。400 参数错误，401 未认证，403 无权限，404 不存在，409 幂等冲突/唯一冲突/非法状态/并发冲突。'],
        ['安全与可靠性', 'BCrypt、JWT 刷新轮换、tokenVersion 失效旧 Token、RBAC 加数据范围、操作日志；Redis token 锁加数据库唯一键双保险；文件先上传再绑定，移动端重试沿用 submitNo。']])

def build(spec):
    doc=Document(); style_doc(doc)
    add_title(doc, f'维检云枢 {spec["name"]} 后期考核复习', '系统实现重点版')
    add_public_system_section(doc)
    add_audited_implementation(doc, spec)
    doc.save(OUT/spec['out'])

for spec in SPECS:
    build(spec)
    print(OUT/spec['out'])
