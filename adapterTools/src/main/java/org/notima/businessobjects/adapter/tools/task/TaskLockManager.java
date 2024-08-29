package org.notima.businessobjects.adapter.tools.task;

import java.util.List;

public interface TaskLockManager {
    public List<TaskLock> getLocks();
    public long lock(String id);
    public long lock(String id, String metaData);
    public int unlockById(long id);
    public int unlockByTaskId(String id);
    public boolean isTaskLocked(String id);
    public void updateMetaData(long lockId, String metaData);
}
