update ins_task
set assignee_id = (select id from sys_user where username = 'inspector')
where plan_id = (select id from ins_plan where plan_code = 'PLAN-DAILY-001')
  and status <> 2
  and exists (select 1 from sys_user where username = 'inspector');
