package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.task.TaskLockManager;

@Command(scope = "notima", name = "unlock-task", description = "Removes a task's lock, ie if a task was stopped without releasing it. See list-task-locks for task ids.")
@Service
public class UnlockTask implements Action {

	@Reference(optional = true)
	private TaskLockManager taskLockManager;
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "taskId", description = "The task id, ie payment-channel-match-report-556677-8899", required = true, multiValued = false)
	private String taskId;
	
	@Override
	public Object execute() throws Exception {

		if (taskLockManager==null) {
			sess.getConsole().println("No tasklock manager found");
			return null;
		}
		int count = taskLockManager.unlockByTaskId(taskId);
		sess.getConsole().println(count > 0 ? "Task " + taskId + " unlocked." : "Task " + taskId + " wasn't locked.");
		return null;
	}

}
