create table if not exists stat_equip_daily (
    id bigint primary key auto_increment,
    stat_date date not null,
    equip_id bigint not null,
    task_total int not null default 0,
    task_finish int not null default 0,
    abnormal_count int not null default 0,
    fault_count int not null default 0,
    downtime_minutes int not null default 0,
    mtbf_hours decimal(18,2) not null default 0,
    mttr_minutes decimal(18,2) not null default 0,
    health_score decimal(5,2) not null default 100,
    constraint uk_stat_equip_daily unique (stat_date, equip_id)
);

create index idx_stat_equip_daily_date on stat_equip_daily (stat_date);
create index idx_stat_equip_daily_equip on stat_equip_daily (equip_id, stat_date);
