package org.notima.businessobjects.adapter.tools.task;

import java.io.IOException;
import java.io.PrintStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Task {

	protected Logger log = LoggerFactory.getLogger(Task.class);

	private long lockId = 0;
	
	protected PrintStream outStream;
	protected InputListener inputListener;

	protected boolean testRun;
	
	
	public Task(PrintStream outStream) {
		this.outStream = outStream;
	}
	
	public Task(PrintStream outStream, InputListener inputListener) {
		this.outStream = outStream;
		this.inputListener = inputListener;
	}

	void setTestRun(boolean test){
		testRun = test;
	}

	public abstract String getTaskId();
	
	protected void output(String msg) {
		if (outStream!=null) 
			outStream.printf(getOutputFormat() + "\n", msg);
		else
			System.out.printf(getOutputFormat() + "\n", msg);
	}
	
	/**
	 * Formats a string and prints it to the designated output.
	 * 
	 * @param msg	The label / message
	 * @param args	The arguments to format.
	 * @return		The formatted string so it can be used for more purposes.
	 */
	protected String outputf(String msg, Object... args) {
		String formattedMsg = String.format(msg, args); 
		output(formattedMsg);
		return formattedMsg;
	}
	
	protected <S> Object getServiceReference(Class<S> clazz) {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		if (bundle != null) {
			BundleContext ctx = bundle.getBundleContext();
			ServiceReference<S> reference = ctx
					.getServiceReference(clazz);
			if (reference != null)
				return ctx.getService(reference);
		}
		return null;
    }
	
	protected TaskLockManager getTaskLockManager() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		BundleContext ctx = bundle.getBundleContext();
		ServiceReference<TaskLockManager> reference = ctx.getServiceReference(TaskLockManager.class);
		return (TaskLockManager) ctx.getService(reference);
	}
	
	public Object execute() throws Exception {
		TaskLockManager taskLockManager = getTaskLockManager();
		if(taskLockManager.isTaskLocked(getTaskId())){
			Exception e = new Exception("Attempted to execute a locked task (" + getTaskId() + ")");
			log.error("Attempted to execute a locked task", e);
			throw e;
		}
		lockId = taskLockManager.lock(getTaskId());
		Object ret = null;
		try {
			ret = onExecute();
		} finally {
			taskLockManager.unlockById(getLockId());
		}
		return ret;
	}

	Object testExecute() throws Exception {
		testRun = true;
		return onExecute();
	}
	
	/**
	 * Implement in derived classes to provide instructions to be performed by the task
	 * @return
	 * @throws Exception
	 */
	protected abstract Object onExecute() throws Exception;

	/**
	 * Use this to provide information about what the task is doing so that it is
	 * clear why the task is currently locked.
	 * @param metaData
	 */
	protected void updateLockMetaData(String metaData) {
		if(!testRun)
			getTaskLockManager().updateMetaData(getLockId(), metaData);
		else 
			System.out.printf("New Lock meta data: %s\n", metaData);
	}

	/**
	 * Create a format for log messages.
	 * Used to add general information to log messages.
	 * 
	 * @return
	 * A format string containing one (and only one) string format specifier (%s).
	 * Trailing new lines are not needed!
	 */
	protected String getOutputFormat() {
		return String.format("%s: ", getTaskId()) + "%s";
	}

	/**
	 * @return The lock id of the task lock that is currently locking this task.
	 * This uniquely identifies the lock created by this task as opposed to the
	 * task id.
	 */
	protected long getLockId(){
		return lockId;
	}
	
	/**
	 * If a task requires input, Implement this interface to provide the input when 
	 * prompted by the task.
	 * 
	 * For a task that is started by a shell command, the implementation can prompt
	 * the user to enter the input through the shell.
	 * 
	 * For a task that is not started by a shell command (for example, tasks started
	 * by a scheduler) the implementation should simply return the appropriate input.
	 */
	public interface InputListener{
		String onInputPrompt(String prompt, Character mask) throws IOException;
	}
	
	
}
