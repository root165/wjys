insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 5, 0, '设备台账', '/equipment', 'EquipmentView', 'equipment:view', 1, 3, 1
where not exists (select 1 from sys_menu where id = 5);

insert into sys_role_menu (role_id, menu_id)
select 1, 5 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 5);
