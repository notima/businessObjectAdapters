package org.notima.businessobjects.adapter.jasperreports.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXB;

import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;

public class DunningEntryXmlDataSource {

	public static final String DUNNING_XML_FILE = "DUNNING_XML";
	
	public static Collection<DunningEntry<?,?>> getDunningEntries() throws Exception {
		
		List<DunningEntry<?,?>> entries = new ArrayList<DunningEntry<?,?>>();

		String xmlFile = System.getenv(DUNNING_XML_FILE);
		if (xmlFile==null) {
			xmlFile = System.getProperty(DUNNING_XML_FILE);
		}
		File inFile = null;
		if (xmlFile==null) {
			throw new Exception("Environment variable " + DUNNING_XML_FILE + " is not set.");
		} else {
			inFile = new File(xmlFile);
			if (!inFile.canRead()) {
				throw new Exception(xmlFile + " can't be read.");
			}
		}
		
		DunningRun<?,?> result = JAXB.unmarshal(inFile, DunningRun.class);
		
		entries.addAll(result.getEntries());
			
		return entries;
		
	}
	
	
}
