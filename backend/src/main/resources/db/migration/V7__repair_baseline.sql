create table if not exists wo_repair (
    id bigint primary key auto_increment,
    equip_id bigint not null,
    source_type varchar(64) not null,
    source_id bigint not null,
    description varchar(1000) not null,
    reporter_id bigint,
    status tinyint not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_wo_repair_source unique (source_type, source_id)
);

create table if not exists wo_order (
    id bigint primary key auto_increment,
    repair_id bigint not null,
    order_no varchar(64) not null,
    assignee_id bigint,
    status tinyint not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_wo_order_no unique (order_no)
);

create table if not exists wo_order_log (
    id bigint primary key auto_increment,
    order_id bigint not null,
    action varchar(64) not null,
    operator_id bigint,
    remark varchar(500),
    created_time datetime(3) not null default current_timestamp(3)
);
