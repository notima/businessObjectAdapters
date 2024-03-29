package org.notima.generic.adempiere.factory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Utility class to enable usage of a connection as a data source.
 * 
 * @author Daniel Tamm
 *
 */
public class ConnectionDataSource implements DataSource {
	
	private Connection conn;
	
	public ConnectionDataSource(Connection conn) {
		this.conn = conn;
	}
	
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return conn;
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return conn;
	}
	
}