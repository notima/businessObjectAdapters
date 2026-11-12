package org.notima.businessobjects.adapter.financialinfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serves saved responses by the part of the URL they're for, and records the calls. */
class StubHttp implements HttpFetcher {

	final Map<String, String> responses = new LinkedHashMap<String, String>();
	final List<String> calls = new ArrayList<String>();
	final List<Map<String, String>> headers = new ArrayList<Map<String, String>>();
	/** Fails every call, as if the source can't be reached. */
	boolean down;

	StubHttp serve(String urlPart, String fixture) throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/fixtures/" + fixture)) {
			responses.put(urlPart, new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
		return this;
	}

	@Override
	public String get(String url, Map<String, String> h) throws IOException {
		calls.add(url);
		headers.add(h);
		if (down) throw new IOException("HTTP 503: " + url);
		for (Map.Entry<String, String> e : responses.entrySet()) {
			if (url.contains(e.getKey())) return e.getValue();
		}
		throw new IOException("HTTP 404: " + url);
	}

	int callsTo(String urlPart) {
		int n = 0;
		for (String c : calls) if (c.contains(urlPart)) n++;
		return n;
	}
}
