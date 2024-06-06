package org.notima.businessobjects.adapter.json.impl;

import org.notima.generic.businessobjects.BasicPaymentBatchChannel;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelList;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;

public class PaymentBatchChannelListImpl implements PaymentBatchChannelList {

	private List<PaymentBatchChannel>	channelList;
	
	private File channelListFile;
	
	private static Gson gson = JsonUtil.buildGson();

	public static PaymentBatchChannelListImpl createFromFile(File file) throws IOException {

		PaymentBatchChannelListImpl pbl = new PaymentBatchChannelListImpl();
		pbl.channelListFile = file;
		Type listType = new TypeToken<List<BasicPaymentBatchChannel>>() {}.getType();
		if (file.exists()) {
			try (FileReader fr = new FileReader(file)) {
				pbl.channelList = gson.fromJson(fr, listType);
			}
		} else {
			pbl.channelList = new ArrayList<PaymentBatchChannel>();
		}

		return pbl;
	}
	
	public void writeToFile() throws IOException {
		
		try ( FileWriter writer = new FileWriter(channelListFile)) {
			gson.toJson(channelList, writer);
		}
		
	}
	
	@Override
	public List<PaymentBatchChannel> getChannelList() {
		return channelList;
	}

	public File getChannelListFile() {
		return channelListFile;
	}

	@Override
	public List<PaymentBatchChannel> listChannelsForTenant(TaxSubjectIdentifier tenant) {
		List<PaymentBatchChannel> result = new ArrayList<PaymentBatchChannel>();
		if (tenant==null) return result;
		for (PaymentBatchChannel pbc : channelList) {
			if (pbc.getTenant().isEqual(tenant)) {
				result.add(pbc);
			}
		}
		return result;
	}
	
}
