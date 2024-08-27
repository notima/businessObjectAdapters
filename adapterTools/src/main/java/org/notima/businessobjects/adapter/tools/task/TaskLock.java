package org.notima.businessobjects.adapter.tools.task;

import java.util.Date;

public interface TaskLock {

    public Long getLockId();
    public void setLockId(Long lockId);
    public String getTaskId();
    public void setTaskId(String taskId);
    public Date getDate();
    public void setDate(Date date);
    public String getMetaData();
    public void setMetaData(String metaData);
	
}
