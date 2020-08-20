package org.notima.generic.adempiere;

import java.sql.Connection;
import java.util.ArrayList;

import org.notima.generic.businessobjects.DunningRun;

public class AdempiereDunningRun {


	public static DunningRun load(int dunningRunId, Connection conn) throws Exception {
		
		DunningRun dun = new DunningRun();
		
		ArrayList<AdempiereDunningEntry> tempEntries = AdempiereDunningEntry.loadAllForDunningRun(dunningRunId, conn);
		for(AdempiereDunningEntry entry : tempEntries){
			dun.addDunningEntry(AdempiereDunningEntry.convert(entry, conn));
		}
		
		return dun;
	}
	
	public AdempiereDunningRun(){};
	
}
