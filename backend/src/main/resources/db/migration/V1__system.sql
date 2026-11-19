create table if not exists sys_dept (
    id bigint primary key auto_increment,
    parent_id bigint not null default 0,
    dept_name varchar(100) not null,
    sort_no int not null default 0,
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

create table if not exists sys_user (
    id bigint primary key auto_increment,
    username varchar(64) not null,
    password varchar(100) not null,
    nickname varchar(64) not null,
    phone varchar(32),
    email varchar(128),
    dept_id bigint,
    status tinyint not null default 1,
    token_version int not null default 0,
    last_login_time datetime(3),
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_sys_user_username unique (username)
);

create table if not exists sys_role (
    id bigint primary key auto_increment,
    role_code varchar(64) not null,
    role_name varchar(100) not null,
    data_scope tinyint not null default 1,
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_sys_role_code unique (role_code)
);

create table if not exists sys_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id)
);

create table if not exists sys_menu (
    id bigint primary key auto_increment,
    parent_id bigint not null default 0,
    menu_name varchar(100) not null,
    route_path varchar(160),
    component varchar(200),
    permission_code varchar(160),
    menu_type tinyint not null default 1,
    sort_no int not null default 0,
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

create table if not exists sys_role_menu (
    role_id bigint not null,
    menu_id bigint not null,
    primary key (role_id, menu_id)
);

create table if not exists sys_dict (
    id bigint primary key auto_increment,
    dict_type varchar(64) not null,
    dict_label varchar(100) not null,
    dict_value varchar(100) not null,
    sort_no int not null default 0,
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

create table if not exists sys_oper_log (
    id bigint primary key auto_increment,
    user_id bigint,
    username varchar(64),
    operation varchar(160) not null,
    request_method varchar(16),
    request_uri varchar(255),
    success tinyint not null default 1,
    trace_id varchar(64),
    created_time datetime(3) not null default current_timestamp(3)
);

create table if not exists sys_file (
    id bigint primary key auto_increment,
    object_key varchar(255) not null,
    upload_token varchar(100),
    original_name varchar(255),
    checksum varchar(128),
    bind_status tinyint not null default 0,
    biz_type varchar(64),
    biz_id bigint,
    expire_time datetime(3),
    created_time datetime(3) not null default current_timestamp(3)
);

create table if not exists sys_message (
    id bigint primary key auto_increment,
    user_id bigint not null,
    message_type varchar(64) not null,
    title varchar(160) not null,
    content varchar(1000),
    biz_type varchar(64),
    biz_id bigint,
    read_flag tinyint not null default 0,
    push_status tinyint not null default 0,
    retry_count int not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    read_time datetime(3)
);

create index idx_sys_message_user_read_time on sys_message (user_id, read_flag, created_time);

insert into sys_dept (id, parent_id, dept_name, sort_no, status)
select 1, 0, '运维中心', 1, 1
where not exists (select 1 from sys_dept where id = 1);

insert into sys_role (id, role_code, role_name, data_scope, status)
select 1, 'system_admin', '系统管理员', 5, 1
where not exists (select 1 from sys_role where id = 1);

insert into sys_user (id, username, password, nickname, dept_id, status, token_version)
select 1, 'admin', '$2a$10$SZhePwuQ79Yle0rYFsg3y.Fzhe9NNct8MbxR3UMG04LNwxVixNS6m', '系统管理员', 1, 1, 0
where not exists (select 1 from sys_user where id = 1);

insert into sys_user_role (user_id, role_id)
select 1, 1
where not exists (select 1 from sys_user_role where user_id = 1 and role_id = 1);

insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 1, 0, '工作台', '/', 'DashboardView', 'dashboard:view', 1, 1, 1
where not exists (select 1 from sys_menu where id = 1);

insert into sys_menu (id, parent_id, menu_name, route_path, component, permission_code, menu_type, sort_no, status)
select 2, 0, '系统管理', '/system/users', 'SystemUsersView', 'system:user:list', 1, 2, 1
where not exists (select 1 from sys_menu where id = 2);

insert into sys_role_menu (role_id, menu_id)
select 1, 1 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 1);
insert into sys_role_menu (role_id, menu_id)
select 1, 2 where not exists (select 1 from sys_role_menu where role_id = 1 and menu_id = 2);
