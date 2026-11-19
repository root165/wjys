insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 3, 2, '创建用户', null, null, 'system:user:create', 2, 1, 1
where not exists (select 1 from sys_menu where id = 3);

insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 4, 2, '变更用户状态', null, null, 'system:user:status', 2, 2, 1
where not exists (select 1 from sys_menu where id = 4);

insert into sys_role_menu (role_id, menu_id)
select 1, 3 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 3);
insert into sys_role_menu (role_id, menu_id)
select 1, 4 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 4);
