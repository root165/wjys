create table if not exists ins_point (
    id bigint primary key auto_increment,
    equip_id bigint not null,
    point_code varchar(64) not null,
    point_name varchar(160) not null,
    location varchar(255),
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_ins_point_code unique (point_code)
);

create table if not exists ins_item (
    id bigint primary key auto_increment,
    point_id bigint not null,
    item_code varchar(64) not null,
    item_name varchar(160) not null,
    check_method varchar(255),
    value_type varchar(32) not null default 'TEXT',
    min_value decimal(18,6),
    max_value decimal(18,6),
    unit varchar(32),
    options_json varchar(2000),
    required_photo tinyint not null default 0,
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_ins_item_code unique (item_code)
);

create table if not exists ins_plan (
    id bigint primary key auto_increment,
    plan_code varchar(64) not null,
    plan_name varchar(160) not null,
    cycle_type varchar(32) not null,
    cycle_value varchar(64),
    start_date date not null,
    end_date date,
    responsible_user_id bigint,
    status tinyint not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_ins_plan_code unique (plan_code)
);

create table if not exists ins_plan_point (
    plan_id bigint not null,
    point_id bigint not null,
    primary key (plan_id, point_id)
);

create table if not exists ins_task (
    id bigint primary key auto_increment,
    task_no varchar(64) not null,
    plan_id bigint not null,
    plan_time date not null,
    assignee_id bigint,
    status tinyint not null default 0,
    submit_no varchar(64),
    submit_hash varchar(128),
    client_submit_time datetime(3),
    offline_flag tinyint not null default 0,
    finish_time datetime(3),
    total_items int not null default 0,
    abnormal_count int not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_ins_task_no unique (task_no),
    constraint uk_ins_task_plan_time unique (plan_id, plan_time),
    constraint uk_ins_task_submit_no unique (submit_no)
);

create table if not exists ins_task_item (
    id bigint primary key auto_increment,
    task_id bigint not null,
    point_id bigint not null,
    item_id bigint not null,
    equip_id bigint not null,
    equip_code varchar(64) not null,
    point_code varchar(64) not null,
    item_code varchar(64) not null,
    item_name varchar(160) not null,
    value_type varchar(32) not null,
    check_method varchar(255),
    unit varchar(32),
    min_value decimal(18,6),
    max_value decimal(18,6),
    options_json varchar(2000),
    required_photo tinyint not null default 0,
    check_result tinyint,
    check_value varchar(255),
    remark varchar(500),
    photo_file_ids varchar(1000),
    check_time datetime(3),
    abnormal_reason varchar(500),
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

create table if not exists ins_abnormal (
    id bigint primary key auto_increment,
    task_id bigint not null,
    task_item_id bigint not null,
    equip_id bigint not null,
    point_id bigint not null,
    severity tinyint not null default 1,
    description varchar(1000) not null,
    status tinyint not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

insert into ins_point (id, equip_id, point_code, point_name, location, status)
select 1, 1, 'P-EQ001-001', '主轴轴承', '一号装配线东侧', 1
where not exists (select 1 from ins_point where id = 1);

insert into ins_item (id, point_id, item_code, item_name, check_method, value_type, min_value, max_value, unit, required_photo, status)
select 1, 1, 'ITEM-TEMP-001', '主轴轴承温度', '红外测温', 'NUMBER', 0, 80, '℃', 0, 1
where not exists (select 1 from ins_item where id = 1);

insert into ins_plan (id, plan_code, plan_name, cycle_type, cycle_value, start_date, responsible_user_id, status)
select 1, 'PLAN-DAILY-001', '装配线日常巡检', 'DAILY', '1', current_date, 1, 1
where not exists (select 1 from ins_plan where id = 1);

insert into ins_plan_point (plan_id, point_id)
select 1, 1 where not exists (select 1 from ins_plan_point where plan_id = 1 and point_id = 1);
