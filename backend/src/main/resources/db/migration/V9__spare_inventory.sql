alter table wo_order add column spare_cost decimal(18,2) not null default 0;

create table if not exists wo_spare (
    id bigint primary key auto_increment,
    spare_code varchar(64) not null,
    spare_name varchar(160) not null,
    specification varchar(255),
    unit varchar(32) not null,
    stock_qty int not null default 0,
    warning_qty int not null default 0,
    unit_price decimal(18,2) not null default 0,
    status tinyint not null default 1,
    version int not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_wo_spare_code unique (spare_code)
);

create table if not exists wo_spare_record (
    id bigint primary key auto_increment,
    order_id bigint,
    spare_id bigint not null,
    source_record_id bigint,
    direction varchar(32) not null,
    quantity int not null,
    unit_price decimal(18,2) not null default 0,
    amount decimal(18,2) not null default 0,
    operator_id bigint,
    remark varchar(500),
    created_time datetime(3) not null default current_timestamp(3)
);

create index idx_wo_spare_record_order on wo_spare_record (order_id, created_time);
create index idx_wo_spare_record_spare on wo_spare_record (spare_id, created_time);
