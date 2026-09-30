package org.notima.businessobjects.adapter.tools.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestFileTaskLockManager {

	@TempDir
	Path dir;

	private static final String TASK = "payment-channel-match-report-556677-8899";

	@Test
	void lockAndUnlock() {
		FileTaskLockManager m = new FileTaskLockManager(dir.toFile(), "me");
		assertFalse(m.isTaskLocked(TASK));
		long id = m.lock(TASK);
		assertTrue(m.isTaskLocked(TASK));
		assertEquals(1, m.unlockById(id));
		assertFalse(m.isTaskLocked(TASK));
	}

	@Test
	void secondLockIsRefused() {
		FileTaskLockManager m = new FileTaskLockManager(dir.toFile(), "me");
		m.lock(TASK);
		assertThrows(IllegalStateException.class, () -> m.lock(TASK));
		// Another task isn't affected
		m.lock("other-task");
		assertEquals(2, m.getLocks().size());
	}

	@Test
	void lockFromAnotherProcessIsStale() {
		FileTaskLockManager before = new FileTaskLockManager(dir.toFile(), "old-karaf");
		before.lock(TASK);
		FileTaskLockManager now = new FileTaskLockManager(dir.toFile(), "new-karaf");
		assertFalse(now.isTaskLocked(TASK));
		assertTrue(now.getLocks().isEmpty());
		// The stale lock doesn't prevent a new lock
		now.lock(TASK);
		assertTrue(now.isTaskLocked(TASK));
	}

	@Test
	void metaDataAndListing() {
		FileTaskLockManager m = new FileTaskLockManager(dir.toFile(), "me");
		long id = m.lock(TASK, "starting");
		m.updateMetaData(id, "Matching channel ZaverSE");
		List<TaskLock> locks = m.getLocks();
		assertEquals(1, locks.size());
		assertEquals(TASK, locks.get(0).getTaskId());
		assertEquals(Long.valueOf(id), locks.get(0).getLockId());
		assertEquals("Matching channel ZaverSE", locks.get(0).getMetaData());
		assertTrue(locks.get(0).getDate()!=null);
	}

	@Test
	void unlockByTaskId() {
		FileTaskLockManager m = new FileTaskLockManager(dir.toFile(), "me");
		m.lock(TASK);
		assertEquals(1, m.unlockByTaskId(TASK));
		assertEquals(0, m.unlockByTaskId(TASK));
		assertFalse(m.isTaskLocked(TASK));
	}

	@Test
	void lockDirectoryIsCreatedAndTaskIdIsSafeAsFileName() {
		File lockDir = new File(dir.toFile(), "sub/task-locks");
		FileTaskLockManager m = new FileTaskLockManager(lockDir, "me");
		m.lock("a/b:c");
		assertTrue(new File(lockDir, "a_b_c.lock").exists());
		assertTrue(m.isTaskLocked("a/b:c"));
	}

}
