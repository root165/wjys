create table if not exists mt_plan (
    id bigint primary key auto_increment,
    plan_code varchar(64) not null,
    plan_name varchar(160) not null,
    equip_id bigint not null,
    maintain_type tinyint not null,
    cycle_type tinyint not null,
    cycle_value int not null,
    content varchar(500),
    assignee_id bigint,
    next_date date not null,
    status tinyint not null default 1,
    creator_id bigint not null,
    version int not null default 0,
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_mt_plan_code unique (plan_code)
);

create index idx_mt_plan_equip_status on mt_plan (equip_id, status);
create index idx_mt_plan_next_date on mt_plan (status, next_date);

create table if not exists mt_record (
    id bigint primary key auto_increment,
    record_code varchar(64) not null,
    plan_id bigint,
    equip_id bigint not null,
    maintain_date date not null,
    maintainer_id bigint not null,
    maintain_content varchar(1000) not null,
    maintain_result tinyint not null,
    cost decimal(18,2),
    downtime_minutes int,
    images varchar(1000),
    status tinyint not null default 0,
    audit_user bigint,
    audit_time datetime(3),
    remark varchar(500),
    created_time datetime(3) not null default current_timestamp(3),
    updated_time datetime(3) not null default current_timestamp(3),
    constraint uk_mt_record_code unique (record_code),
    constraint uk_mt_record_plan_date unique (plan_id, maintain_date)
);

create index idx_mt_record_equip_date on mt_record (equip_id, maintain_date);
create index idx_mt_record_status on mt_record (status, maintain_date);
