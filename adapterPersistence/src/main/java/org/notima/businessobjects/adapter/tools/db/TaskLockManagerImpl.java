package org.notima.businessobjects.adapter.tools.db;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.notima.businessobjects.adapter.tools.db.entities.TaskLockImpl;
import org.notima.businessobjects.adapter.tools.task.TaskLock;
import org.notima.businessobjects.adapter.tools.task.TaskLockManager;

public class TaskLockManagerImpl implements TaskLockManager {
	
	@PersistenceContext(unitName = "adapterTools")
	private EntityManager em;

    @Override
    public List<TaskLock> getLocks() {
		String queryStr = "SELECT l FROM TaskLock l ";
        return em.createQuery(queryStr, TaskLock.class).getResultList();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public long lock(String id) {
        TaskLock lock = new TaskLockImpl();
        lock.setTaskId(id);
		em.persist(lock);
        return lock.getLockId();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public long lock(String id, String metaData) {
        TaskLock lock = new TaskLockImpl();
        lock.setTaskId(id);
        lock.setMetaData(metaData);
		em.persist(lock);
        return lock.getLockId();
    }

	@Transactional
    @Override
    public int unlockById(long id) {
        return em.createQuery("DELETE FROM TaskLock WHERE lockId = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

	@Transactional
    @Override
    public int unlockByTaskId(String id) {
        return em.createQuery("DELETE FROM TaskLock WHERE taskId = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public boolean isTaskLocked(String id) {
		String queryStr = "SELECT l FROM TaskLock l WHERE l.taskId = ?1";
        return em.createQuery(queryStr, TaskLock.class).setParameter(1, id).getResultList().size() > 0;
    }

	@Transactional
    @Override
    public void updateMetaData(long lockId, String metaData) {
        em.createQuery("UPDATE TaskLock SET metaData = :metaData WHERE lockId = :lockId")
                .setParameter("metaData", metaData)
                .setParameter("lockId", lockId)
                .executeUpdate();
    }
	
	
}
