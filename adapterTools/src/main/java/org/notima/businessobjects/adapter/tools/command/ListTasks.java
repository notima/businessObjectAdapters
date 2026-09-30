package org.notima.businessobjects.adapter.tools.command;

import java.text.SimpleDateFormat;
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

	private final SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Reference(optional = true)
	private TaskLockManager taskLockManager;
	
	@Reference 
	Session sess;
	
	@Override
	public Object execute() throws Exception {

		if (taskLockManager==null) {
			sess.getConsole().println("No tasklock manager found");
		} else {
			List<TaskLock> locks = taskLockManager.getLocks();
			if (locks.isEmpty()) {
				sess.getConsole().println("No task locks");
			}
			for (TaskLock l : locks) {
				sess.getConsole().println("Lock " + l.getLockId() + "  " + l.getTaskId() 
						+ (l.getDate()!=null ? "  since " + dfmt.format(l.getDate()) : "")
						+ (l.getMetaData()!=null ? "  " + l.getMetaData() : ""));
			}
		}
		
		return null;
	}

}
