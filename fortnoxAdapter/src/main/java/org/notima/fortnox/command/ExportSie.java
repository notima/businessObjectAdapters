package org.notima.fortnox.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "export-sie", description = "Exports SIE-file for given client")
@Service
public class ExportSie extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Option(name = "--fromdate", description = "Use this date to determine what year to export. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDate;
	
	@Option(name = "--sieType", description ="Type of sie file. 1,2 or 4 are supported. 4 is default.", required = false, multiValued = false)
	private Integer	sieType;
	
	@Option(name = "-of", description = "Outfile. If unspecified, stdout is used", required = false, multiValued = false)
	@Completion(FileCompleter.class)
	private String outFile;
	
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		if (sieType==null) {
			sieType = 4;
		}
		
		Date useDate = null;
		if (fromDate!=null) {
			useDate = FortnoxClient3.s_dfmt.parse(fromDate);
		}
		
		int yearId = fc.getFinancialYear(useDate).getId();
		
		ByteBuffer sieContent = fc.retrieveSieFile(sieType, yearId);
		
		PrintStream os = System.out;
		
		File destinationFile = new File(outFile);
		os = new PrintStream(new FileOutputStream(destinationFile));
		
		os.write(sieContent.array());
		
		if (os!=System.out) {
			System.out.println("SIE file saved to " + destinationFile.getAbsolutePath());
		}

		os.close();
		
		return null;
	}

	
	
	
	
}
