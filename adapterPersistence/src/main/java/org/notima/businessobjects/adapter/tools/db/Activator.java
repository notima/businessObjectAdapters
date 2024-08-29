package org.notima.businessobjects.adapter.tools.db;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.businessobjects.adapter.tools.task.TaskLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {@ProvideService(TaskLockManager.class)}
)
public class Activator extends BaseActivator {

	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() {
		
		TaskLockManager taskLockManager = new TaskLockManagerImpl();
		register(TaskLockManager.class, taskLockManager);
		log.info("Created task lock manager");
		
	}
	
}
