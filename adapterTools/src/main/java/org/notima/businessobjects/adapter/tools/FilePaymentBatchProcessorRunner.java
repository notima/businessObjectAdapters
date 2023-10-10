package org.notima.businessobjects.adapter.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;

public class FilePaymentBatchProcessorRunner {

	private PaymentBatch	paymentBatch;
	private PaymentBatchProcessOptions options;
	private PaymentBatchProcessResult result;

	private String	savePath;
	
	
	public FilePaymentBatchProcessorRunner(PaymentBatch batch) {
		paymentBatch = batch;
	}
	
	public PaymentBatchProcessOptions getOptions() {
		return options;
	}

	public void setOptions(PaymentBatchProcessOptions options) {
		this.options = options;
	}

	public PaymentBatchProcessResult getResult() throws IOException {
		if (result==null)
			createResult();
		return result;
	}

	public void setResult(PaymentBatchProcessResult result) {
		this.result = result;
	}
	
	
	private void createResult() throws IOException {
		
		createSavePath();
		savePaymentBatchToJsonFile();
		
	}
	
	private void savePaymentBatchToJsonFile() throws IOException {
		
		Gson gson = JsonUtil.buildGson();
		String fileName = savePath + paymentBatch.getBatchOwner().getTaxId() + ".json";
		File f = new File(fileName);
		FileWriter writer = new FileWriter(fileName);
		gson.toJson(paymentBatch, writer);
		writer.close();
		System.out.println("Wrote file " + f.getAbsolutePath());
		
	}
	
	private void createSavePath() {
		
		savePath = "";
		
		if (options!=null) {
			
			if (options.getOutputDirectory()!=null) {
				savePath += options.getOutputDirectory();
			}
			
			if (options.getOutputFilePrefix()!=null) {
				savePath = appendSeparator(savePath);
				savePath += options.getOutputFilePrefix();
			}
			
		}
		
		
	}
	
	private String appendSeparator(String path) {
		if (path==null || path.trim().length()==0) {
			path = File.separator;
			return path;
		}
		
		if (path.endsWith(File.separator))
			return path;
		
		return path + File.separator;
		
	}
	
}
