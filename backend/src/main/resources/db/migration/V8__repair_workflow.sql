alter table wo_order add column dispatch_remark varchar(500);
alter table wo_order add column expect_finish_time datetime(3);
alter table wo_order add column finish_reason varchar(1000);
alter table wo_order add column repair_plan varchar(1000);
alter table wo_order add column finish_result varchar(1000);
alter table wo_order add column downtime_minutes int;
alter table wo_order add column accept_images varchar(1000);
alter table wo_order add column reject_reason varchar(500);
alter table wo_order add column finished_time datetime(3);
alter table wo_order add column accepted_time datetime(3);
alter table wo_order add column version int not null default 0;

create index idx_wo_order_repair_id on wo_order (repair_id);
create unique index uk_wo_order_repair_id on wo_order (repair_id);
