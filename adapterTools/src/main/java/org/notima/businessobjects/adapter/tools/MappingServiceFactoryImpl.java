package org.notima.businessobjects.adapter.tools;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Reference;

public class MappingServiceFactoryImpl implements MappingServiceFactory {

	@Reference
	ConfigurationAdmin configAdmin;
	
	private Map<String, Map<String, MappingServiceInstanceFactory>> sourceToTargetMappers = 
			new TreeMap<String, Map<String, MappingServiceInstanceFactory>>();
	
	private BundleContext ctx;
	
	public void setBundleContext(BundleContext c) {
		ctx = c;
	}
	
	@Override
	public MappingServiceInstanceFactory getMappingServiceFor(String sourceSystem, String targetSystem,
			String typeOfObject) {

		try {
			resetSourceServices(sourceSystem);
		} catch (Exception ee) {
			ee.printStackTrace(System.err);
		}
		
		Map<String, MappingServiceInstanceFactory> sourceMappers = sourceToTargetMappers.get(sourceSystem);
		if (sourceMappers==null) return null;

		MappingServiceInstanceFactory mapper = sourceMappers.get(targetSystem);
		return mapper;
		
	}
	
	
	private void resetSourceServices(String sourceSystem) throws InvalidSyntaxException {

		String filter = "SystemName=" + sourceSystem;
		
		Collection<ServiceReference<MappingServiceInstanceFactory>> mrefs = 
				ctx.getServiceReferences(MappingServiceInstanceFactory.class, filter);
		
		if (mrefs!=null) {
			MappingServiceInstanceFactory mf;
			for (ServiceReference<MappingServiceInstanceFactory> m : mrefs) {
				mf = ctx.getService(m);
				addMappingServiceToMap(mf);
			}
		}
		
	}
	
	private void addMappingServiceToMap(MappingServiceInstanceFactory ff) {
		
		Map<String, MappingServiceInstanceFactory> targetMap = sourceToTargetMappers.get(ff.getSourceSystemName());
		if (targetMap==null) {
			targetMap = new TreeMap<String, MappingServiceInstanceFactory>();
		}
		
		targetMap.put(ff.getTargetSystemName(), ff);
		sourceToTargetMappers.put(ff.getSourceSystemName(), targetMap);			
		
	}
	

}
