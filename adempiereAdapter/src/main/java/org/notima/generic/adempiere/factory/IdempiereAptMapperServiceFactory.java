package org.notima.generic.adempiere.factory;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.notima.generic.ifacebusinessobjects.MappingService;
import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;

public class IdempiereAptMapperServiceFactory implements MappingServiceInstanceFactory {

	private static Logger logger = Logger.getLogger(IdempiereAptMapperServiceFactory.class.getName());
	
	private int			adClientId;
	private int			adOrgId;
	
	private DataSource ds;
	
	public int getAdClientId() {
		return adClientId;
	}

	public void setAdClientId(int adClientId) {
		this.adClientId = adClientId;
	}

	public int getAdOrgId() {
		return adOrgId;
	}

	public void setAdOrgId(int adOrgId) {
		this.adOrgId = adOrgId;
	}

	public DataSource getDs() {
		return ds;
	}

	public void setDs(DataSource ds) throws SQLException {
		this.ds = ds;
		if (ds==null || !(ds instanceof DataSource)) {
			logger.warning("No datasource supplied");
			if (ds!=null) {
				logger.warning("Don't know what to do with a " + ds.getClass().getCanonicalName());
			}
			return;
		}
		
	}

	@Override
	public String getSourceSystemName() {
		return Activator.SYSTEM_NAME;
	}

	@Override
	public String getTargetSystemName() {
		return "Generic";
	}

	@Override
	public MappingService getMappingService() {
		return new IDempiereAptMapper(ds, adClientId, adOrgId);
	}

}
