create table if not exists eq_category (
    id bigint primary key auto_increment,
    parent_id bigint not null default 0,
    category_name varchar(100) not null,
    sort_no int not null default 0,
    status tinyint not null default 1,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

create table if not exists eq_equipment (
    id bigint primary key auto_increment,
    category_id bigint,
    equip_code varchar(64) not null,
    equip_name varchar(160) not null,
    model varchar(160),
    manufacturer varchar(160),
    location varchar(255),
    status tinyint not null default 0,
    responsible_user_id bigint,
    purchase_date date,
    warranty_expire date,
    description varchar(1000),
    deleted tinyint not null default 0,
    version int not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_eq_equipment_code unique (equip_code)
);

create table if not exists eq_param (
    id bigint primary key auto_increment,
    equip_id bigint not null,
    param_name varchar(100) not null,
    param_value varchar(160),
    unit varchar(32),
    sort_no int not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3)
);

create table if not exists eq_status_log (
    id bigint primary key auto_increment,
    equip_id bigint not null,
    from_status tinyint,
    to_status tinyint not null,
    source_type varchar(64),
    source_id bigint,
    reason varchar(500),
    created_time datetime(3) not null default current_timestamp(3)
);

insert into eq_category (id, parent_id, category_name, sort_no, status)
select 1, 0, '生产设备', 1, 1 where not exists (select 1 from eq_category where id = 1);
insert into eq_category (id, parent_id, category_name, sort_no, status)
select 2, 0, '动力设备', 2, 1 where not exists (select 1 from eq_category where id = 2);

insert into eq_equipment (id, category_id, equip_code, equip_name, model, manufacturer, location, status, deleted)
select 1, 1, 'EQ-001', '一号装配线', 'AL-2000', '维检制造', 'A栋一层', 0, 0
where not exists (select 1 from eq_equipment where id = 1);
insert into eq_equipment (id, category_id, equip_code, equip_name, model, manufacturer, location, status, deleted)
select 2, 2, 'EQ-002', '空压机组', 'AC-80', '云枢动力', '动力站房', 1, 0
where not exists (select 1 from eq_equipment where id = 2);
