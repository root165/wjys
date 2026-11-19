insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 6, 0, '巡检任务', '/inspection/tasks', 'InspectionTasksView', 'inspection:task:view', 1, 4, 1
where not exists (select 1 from sys_menu where id = 6);

insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 7, 0, '巡检计划', '/inspection/plans', 'InspectionPlansView', 'inspection:plan:view', 1, 5, 1
where not exists (select 1 from sys_menu where id = 7);

insert into sys_role_menu (role_id, menu_id)
select 1, 6 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 6);
insert into sys_role_menu (role_id, menu_id)
select 1, 7 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 7);
