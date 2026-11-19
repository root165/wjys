alter table ins_plan add column assignment_mode varchar(32) not null default 'SELF_CLAIM';
alter table ins_plan add column assignment_dept_id bigint;

-- Existing demo plans are intentionally converted to the safe default. A
-- pending task remains claimable by an inspector instead of being owned by
-- the administrator account.
update ins_plan
set assignment_mode = 'SELF_CLAIM', responsible_user_id = null, assignment_dept_id = null
where plan_code = 'PLAN-DAILY-001';

update ins_task
set assignee_id = null, status = 0
where plan_id = (select id from ins_plan where plan_code = 'PLAN-DAILY-001')
  and status <> 2
  and assignee_id = (select id from sys_user where username = 'admin');
