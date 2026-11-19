-- Demo foundation data for the five operational roles plus a least-privilege visitor.
-- Every insert is idempotent so a redeploy never duplicates course/demo data.

insert into sys_role (role_code, role_name, data_scope, status)
select 'equipment_admin', '设备管理员', 1, 1 where not exists (select 1 from sys_role where role_code = 'equipment_admin');
insert into sys_role (role_code, role_name, data_scope, status)
select 'inspector', '巡检员', 3, 1 where not exists (select 1 from sys_role where role_code = 'inspector');
insert into sys_role (role_code, role_name, data_scope, status)
select 'repairer', '维修工', 3, 1 where not exists (select 1 from sys_role where role_code = 'repairer');
insert into sys_role (role_code, role_name, data_scope, status)
select 'enterprise_manager', '企业管理者', 1, 1 where not exists (select 1 from sys_role where role_code = 'enterprise_manager');
insert into sys_role (role_code, role_name, data_scope, status)
select 'visitor', '游客', 3, 1 where not exists (select 1 from sys_role where role_code = 'visitor');

insert into sys_dept (parent_id, dept_name, sort_no, status)
select 1, '设备管理部', 2, 1 where not exists (select 1 from sys_dept where dept_name = '设备管理部');
insert into sys_dept (parent_id, dept_name, sort_no, status)
select 1, '巡检班组', 3, 1 where not exists (select 1 from sys_dept where dept_name = '巡检班组');
insert into sys_dept (parent_id, dept_name, sort_no, status)
select 1, '维修班组', 4, 1 where not exists (select 1 from sys_dept where dept_name = '维修班组');
insert into sys_dept (parent_id, dept_name, sort_no, status)
select 1, '经营管理部', 5, 1 where not exists (select 1 from sys_dept where dept_name = '经营管理部');

insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '系统配置', '/system/settings', 'SystemSettingsView', 'system:config:view', 1, 3, 1 where not exists (select 1 from sys_menu where permission_code = 'system:config:view');
insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '巡检配置', '/inspection/config', 'InspectionConfigView', 'inspection:config:view', 1, 6, 1 where not exists (select 1 from sys_menu where permission_code = 'inspection:config:view');
insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '异常管理', '/inspection/abnormals', 'AbnormalView', 'inspection:abnormal:view', 1, 8, 1 where not exists (select 1 from sys_menu where permission_code = 'inspection:abnormal:view');
insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '备件台账', '/spares', 'SpareView', 'spare:view', 1, 9, 1 where not exists (select 1 from sys_menu where permission_code = 'spare:view');
insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '维修工单', '/work-orders', 'WorkOrdersView', 'repair:view', 1, 10, 1 where not exists (select 1 from sys_menu where permission_code = 'repair:view');
insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '维保管理', '/maintenance', 'MaintenanceView', 'maintenance:view', 1, 11, 1 where not exists (select 1 from sys_menu where permission_code = 'maintenance:view');
insert into sys_menu (parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 0, '消息中心', '/messages', 'MessageCenterView', 'message:view', 1, 12, 1 where not exists (select 1 from sys_menu where permission_code = 'message:view');

insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '设备维护', 'equipment:write', 2, 20, 1 where not exists (select 1 from sys_menu where permission_code = 'equipment:write');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '设备状态操作', 'equipment:status', 2, 21, 1 where not exists (select 1 from sys_menu where permission_code = 'equipment:status');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '巡检计划维护', 'inspection:plan:write', 2, 22, 1 where not exists (select 1 from sys_menu where permission_code = 'inspection:plan:write');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '巡检配置维护', 'inspection:config:write', 2, 23, 1 where not exists (select 1 from sys_menu where permission_code = 'inspection:config:write');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '巡检任务操作', 'inspection:task:operate', 2, 24, 1 where not exists (select 1 from sys_menu where permission_code = 'inspection:task:operate');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '异常转报修', 'repair:create', 2, 25, 1 where not exists (select 1 from sys_menu where permission_code = 'repair:create');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '报修管理', 'repair:manage', 2, 26, 1 where not exists (select 1 from sys_menu where permission_code = 'repair:manage');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '维修作业', 'repair:operate', 2, 27, 1 where not exists (select 1 from sys_menu where permission_code = 'repair:operate');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '工单验收', 'repair:accept', 2, 28, 1 where not exists (select 1 from sys_menu where permission_code = 'repair:accept');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '备件维护', 'spare:write', 2, 29, 1 where not exists (select 1 from sys_menu where permission_code = 'spare:write');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '工单备件领用', 'spare:consume', 2, 30, 1 where not exists (select 1 from sys_menu where permission_code = 'spare:consume');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '维保记录维护', 'maintenance:write', 2, 31, 1 where not exists (select 1 from sys_menu where permission_code = 'maintenance:write');
insert into sys_menu (parent_id, menu_name, permission_code, menu_type, sort_no, status)
select 0, '维保记录审核', 'maintenance:audit', 2, 32, 1 where not exists (select 1 from sys_menu where permission_code = 'maintenance:audit');

-- Admin receives every current menu, including operation permissions.
insert into sys_role_menu (role_id, menu_id)
select r.id, m.id from sys_role r cross join sys_menu m
where r.role_code = 'system_admin' and not exists (select 1 from sys_role_menu x where x.role_id = r.id and x.menu_id = m.id);

-- Each business role receives only the route and operation permissions needed for its work.
insert into sys_role_menu (role_id, menu_id)
select r.id, m.id from sys_role r join sys_menu m on m.permission_code in ('dashboard:view','equipment:view','equipment:write','equipment:status','inspection:plan:view','inspection:plan:write','repair:view','repair:manage','repair:accept','maintenance:view','maintenance:audit','message:view')
where r.role_code = 'equipment_admin' and not exists (select 1 from sys_role_menu x where x.role_id = r.id and x.menu_id = m.id);
insert into sys_role_menu (role_id, menu_id)
select r.id, m.id from sys_role r join sys_menu m on m.permission_code in ('dashboard:view','inspection:task:view','inspection:task:operate','inspection:abnormal:view','repair:create','message:view')
where r.role_code = 'inspector' and not exists (select 1 from sys_role_menu x where x.role_id = r.id and x.menu_id = m.id);
insert into sys_role_menu (role_id, menu_id)
select r.id, m.id from sys_role r join sys_menu m on m.permission_code in ('dashboard:view','repair:view','repair:operate','repair:accept','spare:view','spare:consume','message:view')
where r.role_code = 'repairer' and not exists (select 1 from sys_role_menu x where x.role_id = r.id and x.menu_id = m.id);
insert into sys_role_menu (role_id, menu_id)
select r.id, m.id from sys_role r join sys_menu m on m.permission_code in ('dashboard:view','equipment:view','inspection:plan:view','inspection:task:view','inspection:abnormal:view','repair:view','maintenance:view','spare:view','message:view')
where r.role_code = 'enterprise_manager' and not exists (select 1 from sys_role_menu x where x.role_id = r.id and x.menu_id = m.id);
insert into sys_role_menu (role_id, menu_id)
select r.id, m.id from sys_role r join sys_menu m on m.permission_code = 'dashboard:view'
where r.role_code = 'visitor' and not exists (select 1 from sys_role_menu x where x.role_id = r.id and x.menu_id = m.id);

-- All demo accounts use password for classroom demonstration; change it after deployment.
insert into sys_user (username, password, nickname, dept_id, status, token_version)
select 'equip_admin', '$2a$10$SZhePwuQ79Yle0rYFsg3y.Fzhe9NNct8MbxR3UMG04LNwxVixNS6m', '设备管理员', d.id, 1, 0 from sys_dept d
where d.dept_name = '设备管理部' and not exists (select 1 from sys_user where username = 'equip_admin');
insert into sys_user (username, password, nickname, dept_id, status, token_version)
select 'inspector', '$2a$10$SZhePwuQ79Yle0rYFsg3y.Fzhe9NNct8MbxR3UMG04LNwxVixNS6m', '巡检员', d.id, 1, 0 from sys_dept d
where d.dept_name = '巡检班组' and not exists (select 1 from sys_user where username = 'inspector');
insert into sys_user (username, password, nickname, dept_id, status, token_version)
select 'repairer', '$2a$10$SZhePwuQ79Yle0rYFsg3y.Fzhe9NNct8MbxR3UMG04LNwxVixNS6m', '维修工', d.id, 1, 0 from sys_dept d
where d.dept_name = '维修班组' and not exists (select 1 from sys_user where username = 'repairer');
insert into sys_user (username, password, nickname, dept_id, status, token_version)
select 'manager', '$2a$10$SZhePwuQ79Yle0rYFsg3y.Fzhe9NNct8MbxR3UMG04LNwxVixNS6m', '企业管理者', d.id, 1, 0 from sys_dept d
where d.dept_name = '经营管理部' and not exists (select 1 from sys_user where username = 'manager');
insert into sys_user (username, password, nickname, dept_id, status, token_version)
select 'guest', '$2a$10$SZhePwuQ79Yle0rYFsg3y.Fzhe9NNct8MbxR3UMG04LNwxVixNS6m', '游客', 1, 1, 0 where not exists (select 1 from sys_user where username = 'guest');

insert into sys_user_role (user_id, role_id)
select u.id, r.id from sys_user u cross join sys_role r where u.username = 'equip_admin' and r.role_code = 'equipment_admin' and not exists (select 1 from sys_user_role x where x.user_id = u.id and x.role_id = r.id);
insert into sys_user_role (user_id, role_id)
select u.id, r.id from sys_user u cross join sys_role r where u.username = 'inspector' and r.role_code = 'inspector' and not exists (select 1 from sys_user_role x where x.user_id = u.id and x.role_id = r.id);
insert into sys_user_role (user_id, role_id)
select u.id, r.id from sys_user u cross join sys_role r where u.username = 'repairer' and r.role_code = 'repairer' and not exists (select 1 from sys_user_role x where x.user_id = u.id and x.role_id = r.id);
insert into sys_user_role (user_id, role_id)
select u.id, r.id from sys_user u cross join sys_role r where u.username = 'manager' and r.role_code = 'enterprise_manager' and not exists (select 1 from sys_user_role x where x.user_id = u.id and x.role_id = r.id);
insert into sys_user_role (user_id, role_id)
select u.id, r.id from sys_user u cross join sys_role r where u.username = 'guest' and r.role_code = 'visitor' and not exists (select 1 from sys_user_role x where x.user_id = u.id and x.role_id = r.id);

insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'equipment_status', '正常', '0', 1, 1 where not exists (select 1 from sys_dict where dict_type = 'equipment_status' and dict_value = '0');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'equipment_status', '运行中', '1', 2, 1 where not exists (select 1 from sys_dict where dict_type = 'equipment_status' and dict_value = '1');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'equipment_status', '维修中', '2', 3, 1 where not exists (select 1 from sys_dict where dict_type = 'equipment_status' and dict_value = '2');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'inspection_result', '正常', '1', 1, 1 where not exists (select 1 from sys_dict where dict_type = 'inspection_result' and dict_value = '1');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'inspection_result', '异常', '0', 2, 1 where not exists (select 1 from sys_dict where dict_type = 'inspection_result' and dict_value = '0');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'work_order_status', '待派工', '0', 1, 1 where not exists (select 1 from sys_dict where dict_type = 'work_order_status' and dict_value = '0');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'work_order_status', '维修中', '2', 2, 1 where not exists (select 1 from sys_dict where dict_type = 'work_order_status' and dict_value = '2');
insert into sys_dict (dict_type, dict_label, dict_value, sort_no, status)
select 'work_order_status', '待验收', '3', 3, 1 where not exists (select 1 from sys_dict where dict_type = 'work_order_status' and dict_value = '3');

insert into wo_spare (spare_code, spare_name, specification, unit, stock_qty, warning_qty, unit_price, status)
select 'SP-BRG-001', '深沟球轴承', '6205-2RS', '件', 24, 5, 48.50, 1 where not exists (select 1 from wo_spare where spare_code = 'SP-BRG-001');
insert into wo_spare (spare_code, spare_name, specification, unit, stock_qty, warning_qty, unit_price, status)
select 'SP-FUS-001', '快速熔断器', '10A / 500V', '个', 50, 10, 6.80, 1 where not exists (select 1 from wo_spare where spare_code = 'SP-FUS-001');
insert into wo_spare (spare_code, spare_name, specification, unit, stock_qty, warning_qty, unit_price, status)
select 'SP-OIL-001', '设备润滑油', 'ISO VG68', '升', 12, 3, 128.00, 1 where not exists (select 1 from wo_spare where spare_code = 'SP-OIL-001');

insert into ins_point (equip_id, point_code, point_name, location, status)
select 2, 'P-EQ002-001', '空压机润滑系统', '动力站房西侧', 1 where not exists (select 1 from ins_point where point_code = 'P-EQ002-001');
insert into ins_item (point_id, item_code, item_name, check_method, value_type, min_value, max_value, unit, required_photo, status)
select p.id, 'ITEM-PRESS-001', '储气罐压力', '读取压力表', 'NUMBER', 0.6, 0.9, 'MPa', 0, 1 from ins_point p where p.point_code = 'P-EQ002-001' and not exists (select 1 from ins_item where item_code = 'ITEM-PRESS-001');
