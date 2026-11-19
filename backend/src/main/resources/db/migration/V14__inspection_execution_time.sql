alter table ins_plan add column execute_time time not null default '09:00:00';

update ins_plan
set responsible_user_id = (select id from sys_user where username = 'inspector')
where plan_code = 'PLAN-DAILY-001'
  and exists (select 1 from sys_user where username = 'inspector');
