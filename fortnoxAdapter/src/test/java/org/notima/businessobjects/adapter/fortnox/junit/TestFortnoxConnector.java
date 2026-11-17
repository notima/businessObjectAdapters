package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.notima.businessobjects.adapter.fortnox.FortnoxConnector;
import org.notima.generic.businessobjects.connector.ConnectorSetting;
import org.notima.generic.businessobjects.connector.ConnectorStatus;

/**
 * Settings and status of the Fortnox connector, without calling Fortnox.
 */
public class TestFortnoxConnector {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File dir;

	@Before
	public void setUp() throws Exception {
		dir = new File(tmp.getRoot(), "fortnox");
	}

	@Test
	public void testNotConfigured() {
		FortnoxConnector c = new FortnoxConnector();
		c.setConfigDirectory(dir);
		assertTrue(dir.isDirectory());
		assertFalse(c.isConfigured());
		assertEquals(ConnectorStatus.State.NOT_CONFIGURED, c.getStatus("556000-0000").getState());
	}

	@Test
	public void testSaveAndReadSettings() throws Exception {
		FortnoxConnector c = new FortnoxConnector();
		c.setConfigDirectory(dir);

		Map<String, String> values = new HashMap<String, String>();
		values.put(FortnoxConnector.KEY_CLIENT_ID, " myClientId ");
		values.put(FortnoxConnector.KEY_CLIENT_SECRET, "mySecret ");
		values.put(FortnoxConnector.KEY_PORT, "8010");
		values.put(FortnoxConnector.KEY_SCOPE, "bookkeeping, Customer");
		c.saveSettings(values);

		File f = new File(dir, "integration.json");
		assertTrue(f.isFile());
		try {
			assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(f.toPath())));
		} catch (UnsupportedOperationException e) {
			// Not a POSIX file system
		}

		// Read by a new instance
		FortnoxConnector c2 = new FortnoxConnector();
		c2.setConfigDirectory(dir);
		assertTrue(c2.isConfigured());
		Map<String, String> read = new HashMap<String, String>();
		for (ConnectorSetting s : c2.getSettings(Locale.ENGLISH)) {
			read.put(s.getKey(), s.getValue());
		}
		assertEquals("myClientId", read.get(FortnoxConnector.KEY_CLIENT_ID));
		assertEquals("mySecret", read.get(FortnoxConnector.KEY_CLIENT_SECRET));
		assertEquals("8010", read.get(FortnoxConnector.KEY_PORT));
		assertTrue(read.get(FortnoxConnector.KEY_SCOPE).contains("bookkeeping"));
		assertTrue(read.get(FortnoxConnector.KEY_SCOPE).contains("customer"));
		assertTrue(c2.getSetupInstructions(Locale.ENGLISH).contains("http://localhost:8010/login"));

		// No stored authorization
		assertEquals(ConnectorStatus.State.NOT_CONNECTED, c2.getStatus("556000-0000").getState());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidScope() throws Exception {
		FortnoxConnector c = new FortnoxConnector();
		c.setConfigDirectory(dir);
		Map<String, String> values = new HashMap<String, String>();
		values.put(FortnoxConnector.KEY_SCOPE, "bookkeeping,orders");
		c.saveSettings(values);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPort() throws Exception {
		FortnoxConnector c = new FortnoxConnector();
		c.setConfigDirectory(dir);
		Map<String, String> values = new HashMap<String, String>();
		values.put(FortnoxConnector.KEY_PORT, "80");
		c.saveSettings(values);
	}

}
