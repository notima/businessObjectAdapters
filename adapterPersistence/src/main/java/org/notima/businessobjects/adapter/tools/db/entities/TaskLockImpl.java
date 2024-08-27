package org.notima.businessobjects.adapter.tools.db.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.notima.businessobjects.adapter.tools.task.TaskLock;

@Entity
public class TaskLockImpl implements TaskLock {

	@Id
	@GeneratedValue
	private Long lockId;
    private String taskId;
    private Date date = new Date();
    private String metaData;
    
    public Long getLockId() {
        return lockId;
    }
    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }
    public String getTaskId() {
        return taskId;
    }
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public String getMetaData() {
        return metaData;
    }
    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }	
	
}
