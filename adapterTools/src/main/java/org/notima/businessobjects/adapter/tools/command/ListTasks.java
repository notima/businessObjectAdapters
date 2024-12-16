package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.task.TaskLock;
import org.notima.businessobjects.adapter.tools.task.TaskLockManager;

@Command(scope = "notima", name = "list-task-locks", description = "Lists on-going tasks")
@Service
public class ListTasks implements Action {

//	@Reference (depends on persistence which dependency we're not ready for yet)
	private TaskLockManager taskLockManager;
	
	@Reference 
	Session sess;
	
	@Override
	public Object execute() throws Exception {

		if (taskLockManager==null) {
			sess.getConsole().println("No tasklock manager found");
		} else {
			List<TaskLock> locks = taskLockManager.getLocks();
			for (TaskLock l : locks) {
				sess.getConsole().println("Lock " + l.getLockId());
			}
		}
		
		return null;
	}

}
