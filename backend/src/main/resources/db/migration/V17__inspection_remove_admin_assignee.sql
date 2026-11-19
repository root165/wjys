-- System administrators monitor inspection work; they are not execution users.
-- Release only unfinished legacy demo tasks that were incorrectly assigned to
-- the admin account. Completed history remains unchanged.
update ins_task
set assignee_id = null, status = 0
where assignee_id = (select id from sys_user where username = 'admin')
  and plan_id in (select id from ins_plan where plan_code in ('PLAN-DAILY-001', 'PLAN-WEEKLY', 'PLAN-DALIY002'))
  and status in (0, 1, 3);
