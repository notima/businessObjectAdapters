package org.notima.businessobjects.adapter.tools.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task lock manager that keeps one lock file per task in a directory, by default
 * {@code ${karaf.data}/task-locks}.
 *
 * A lock file records the process (JVM) that created it. Locks created by another process, ie
 * before Karaf was restarted or by a process that was killed, are stale: they don't lock the
 * task and are removed when found.
 */
public class FileTaskLockManager implements TaskLockManager {

	private static final Logger log = LoggerFactory.getLogger(FileTaskLockManager.class);

	public static final String LOCK_DIRECTORY_NAME = "task-locks";
	private static final String LOCK_SUFFIX = ".lock";

	private static final String P_LOCK_ID = "lockId";
	private static final String P_TASK_ID = "taskId";
	private static final String P_DATE = "date";
	private static final String P_META_DATA = "metaData";
	private static final String P_OWNER = "owner";

	private final File		lockDirectory;
	private final String	owner;
	private final AtomicLong	nextLockId = new AtomicLong(System.currentTimeMillis());

	/**
	 * Uses {@code ${karaf.data}/task-locks}, or the temp directory if not running in Karaf.
	 */
	public FileTaskLockManager() {
		this(defaultLockDirectory());
	}

	public FileTaskLockManager(File lockDirectory) {
		this(lockDirectory, currentProcessOwner());
	}

	/**
	 * @param lockDirectory	The directory for the lock files. Created when needed.
	 * @param owner			Identifies this process. Locks with another owner are stale.
	 */
	FileTaskLockManager(File lockDirectory, String owner) {
		this.lockDirectory = lockDirectory;
		this.owner = owner;
	}

	private static File defaultLockDirectory() {
		String karafData = System.getProperty("karaf.data");
		String base = karafData!=null ? karafData : System.getProperty("java.io.tmpdir");
		return new File(base, LOCK_DIRECTORY_NAME);
	}

	/**
	 * @return	Identifies the current process: its name (pid@host) and start time.
	 */
	private static String currentProcessOwner() {
		RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
		return rt.getName() + ":" + rt.getStartTime();
	}

	public File getLockDirectory() {
		return lockDirectory;
	}

	@Override
	public synchronized List<TaskLock> getLocks() {
		List<TaskLock> result = new ArrayList<TaskLock>();
		File[] files = lockDirectory.listFiles((dir, name) -> name.endsWith(LOCK_SUFFIX));
		if (files==null) return result;
		for (File f : files) {
			FileTaskLock lock = readValidLock(f);
			if (lock!=null) {
				result.add(lock);
			}
		}
		return result;
	}

	@Override
	public long lock(String id) {
		return lock(id, null);
	}

	/**
	 * Creates the task's lock file.
	 *
	 * @throws IllegalStateException	If the task is already locked.
	 */
	@Override
	public synchronized long lock(String id, String metaData) {

		if (!lockDirectory.isDirectory() && !lockDirectory.mkdirs()) {
			throw new IllegalStateException("Can't create lock directory " + lockDirectory);
		}
		File f = lockFile(id);
		// Remove a stale lock so it can be replaced
		readValidLock(f);

		long lockId = nextLockId.incrementAndGet();
		Properties props = new Properties();
		props.setProperty(P_LOCK_ID, Long.toString(lockId));
		props.setProperty(P_TASK_ID, id);
		props.setProperty(P_DATE, Long.toString(System.currentTimeMillis()));
		props.setProperty(P_OWNER, owner);
		if (metaData!=null) {
			props.setProperty(P_META_DATA, metaData);
		}
		// CREATE_NEW is atomic: fails if another run created the lock first
		try (OutputStream out = Files.newOutputStream(f.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			props.store(out, "Task lock");
		} catch (FileAlreadyExistsException e) {
			throw new IllegalStateException("Task " + id + " is already locked");
		} catch (IOException e) {
			throw new IllegalStateException("Can't create lock file " + f + ": " + e.getMessage(), e);
		}
		return lockId;
	}

	@Override
	public synchronized int unlockById(long id) {
		File[] files = lockDirectory.listFiles((dir, name) -> name.endsWith(LOCK_SUFFIX));
		if (files==null) return 0;
		int count = 0;
		for (File f : files) {
			Properties props = readProperties(f);
			if (props!=null && Long.toString(id).equals(props.getProperty(P_LOCK_ID))) {
				if (f.delete()) count++;
			}
		}
		return count;
	}

	@Override
	public synchronized int unlockByTaskId(String id) {
		File f = lockFile(id);
		return f.exists() && f.delete() ? 1 : 0;
	}

	@Override
	public synchronized boolean isTaskLocked(String id) {
		return readValidLock(lockFile(id))!=null;
	}

	@Override
	public synchronized void updateMetaData(long lockId, String metaData) {
		File[] files = lockDirectory.listFiles((dir, name) -> name.endsWith(LOCK_SUFFIX));
		if (files==null) return;
		for (File f : files) {
			Properties props = readProperties(f);
			if (props!=null && Long.toString(lockId).equals(props.getProperty(P_LOCK_ID))) {
				if (metaData!=null) {
					props.setProperty(P_META_DATA, metaData);
				} else {
					props.remove(P_META_DATA);
				}
				try (OutputStream out = Files.newOutputStream(f.toPath(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
					props.store(out, "Task lock");
				} catch (IOException e) {
					log.warn("Can't update lock file {}: {}", f, e.getMessage());
				}
			}
		}
	}

	/**
	 * Reads a lock file. A stale lock (created by another process) is removed.
	 *
	 * @return	The lock, or null if there's no valid lock.
	 */
	private FileTaskLock readValidLock(File f) {
		if (!f.exists()) return null;
		Properties props = readProperties(f);
		if (props==null) return null;
		if (!owner.equals(props.getProperty(P_OWNER))) {
			log.info("Removing stale task lock {} created by {}", f.getName(), props.getProperty(P_OWNER));
			f.delete();
			return null;
		}
		FileTaskLock lock = new FileTaskLock();
		try {
			lock.setLockId(Long.valueOf(props.getProperty(P_LOCK_ID)));
			lock.setDate(new Date(Long.parseLong(props.getProperty(P_DATE))));
		} catch (NumberFormatException e) {
			// Keep what can be read
		}
		lock.setTaskId(props.getProperty(P_TASK_ID));
		lock.setMetaData(props.getProperty(P_META_DATA));
		return lock;
	}

	private Properties readProperties(File f) {
		Properties props = new Properties();
		try (InputStream in = new FileInputStream(f)) {
			props.load(in);
			return props;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @return	The lock file of a task. The task id is made safe to use as file name.
	 */
	private File lockFile(String taskId) {
		String name = taskId==null ? "null" : taskId.replaceAll("[^a-zA-Z0-9._-]", "_");
		return new File(lockDirectory, name + LOCK_SUFFIX);
	}

	/**
	 * A task lock read from a lock file.
	 */
	public static class FileTaskLock implements TaskLock {

		private Long	lockId;
		private String	taskId;
		private Date	date;
		private String	metaData;

		@Override
		public Long getLockId() {
			return lockId;
		}
		@Override
		public void setLockId(Long lockId) {
			this.lockId = lockId;
		}
		@Override
		public String getTaskId() {
			return taskId;
		}
		@Override
		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}
		@Override
		public Date getDate() {
			return date;
		}
		@Override
		public void setDate(Date date) {
			this.date = date;
		}
		@Override
		public String getMetaData() {
			return metaData;
		}
		@Override
		public void setMetaData(String metaData) {
			this.metaData = metaData;
		}

	}

}
