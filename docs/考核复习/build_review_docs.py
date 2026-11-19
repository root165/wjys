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
        "out": "郑亮-维检云枢-M05维保与M06备件-考核复习.docx",
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
    },
    {
        "name": "高浩然",
        "source": "02-高浩然-M02设备与M04维修.md",
        "out": "高浩然-维检云枢-M02设备与M04维修-考核复习.docx",
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
    },
    {
        "name": "张万健",
        "source": "03-张万健-M01M03M07M08与横切设计.md",
        "out": "张万健-维检云枢-M01M03M07M08与横切设计-考核复习.docx",
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
    for i, value in enumerate(rows[0]):
        cell = hdr.cells[i]; cell.text = ''
        p = cell.paragraphs[0]; r = p.add_run(value); font(r, size=8.7, bold=True, color=(255,255,255))
        set_cell_shading(cell, '1F4E78'); set_cell_margins(cell); cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
    for idx, row in enumerate(rows[1:]):
        cells = tbl.add_row().cells
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

def add_code_section(doc, codes):
    doc.add_page_break(); doc.add_paragraph('关键代码支撑与讲解', style='Heading 1')
    doc.add_paragraph('以下为仓库真实 Service 代码节选。考核中不要逐行背诵，应说明：输入如何校验、什么条件更新、写了哪些表、事务如何保证一致性、失败如何返回。')
    for title, path, markers in codes:
        doc.add_paragraph(title, style='Heading 2')
        p=doc.add_paragraph(); r=p.add_run(f'代码位置：{path}'); font(r, size=9, color=(80,80,80))
        for marker in markers:
            doc.add_paragraph(marker, style='Heading 3')
            code=extract_method(Path(path), marker)
            p=doc.add_paragraph(); p.paragraph_format.left_indent=Inches(.12); p.paragraph_format.right_indent=Inches(.08)
            for n, line in enumerate(code.splitlines()):
                r=p.add_run(f'{n+1:>2}  {line}\n'); font(r, name='Consolas', size=7.4)

def build(spec):
    doc=Document(); style_doc(doc)
    add_title(doc, f'维检云枢 {spec["name"]} 后期考核复习', '系统实现重点版')
    doc.add_paragraph('一 重点考核答案', style='Heading 1')
    add_table(doc, [['考核点', '可直接作答的答案']] + [[a,b] for a,b in spec['summary']])
    doc.add_paragraph('二 个人负责模块完整复习', style='Heading 1')
    md=(Path(__file__).resolve().parent/spec['source']).read_text(encoding='utf-8')
    add_markdown(doc, md)
    add_code_section(doc, spec['codes'])
    doc.save(OUT/spec['out'])

for spec in SPECS:
    build(spec)
    print(OUT/spec['out'])
