----------------------------------------------------------------
-- [async_tasks] Table
--
Create or replace FUNCTION Insertasync_tasks(v_action_type INTEGER,
	v_result INTEGER,
	v_status INTEGER,
	v_task_id UUID,
	v_action_parameters text,
	v_action_params_class varchar(256),
	v_step_id UUID,
	v_command_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO async_tasks(action_type, result, status, task_id, action_parameters,action_params_class, step_id, command_id)
	VALUES(v_action_type, v_result, v_status, v_task_id, v_action_parameters,v_action_params_class, v_step_id, v_command_id);
END; $procedure$
LANGUAGE plpgsql;    


Create or replace FUNCTION Updateasync_tasks(v_action_type INTEGER,
	v_result INTEGER,
	v_status INTEGER,
	v_task_id UUID,
	v_action_parameters text,
	v_action_params_class varchar(256),
	v_step_id UUID,
	v_command_id UUID)
RETURNS VOID

	--The [async_tasks] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE async_tasks
      SET action_type = v_action_type,
          result = v_result,
          status = v_status,
          action_parameters = v_action_parameters,
          action_params_class = v_action_params_class,
          step_id = v_step_id,
          command_id = v_command_id
      WHERE task_id = v_task_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION Deleteasync_tasks(v_task_id UUID)
RETURNS integer
   AS $procedure$
DECLARE
deleted_rows int;
BEGIN
	
   DELETE FROM async_tasks
   WHERE task_id = v_task_id;
   GET DIAGNOSTICS deleted_rows = ROW_COUNT;
   RETURN deleted_rows;
    
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetAllFromasync_tasks() RETURNS SETOF async_tasks
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM async_tasks;

END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION Getasync_tasksBytask_id(v_task_id UUID) RETURNS SETOF async_tasks
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM async_tasks
   WHERE task_id = v_task_id;

END; $procedure$
LANGUAGE plpgsql;
