package org.notima.businessobjects.adapter.financialinfo;

import java.io.IOException;
import java.util.Map;

/**
 * Fetches a document over HTTP. A seam for the tests, which serve saved responses.
 */
public interface HttpFetcher {

	/**
	 * @param url		The URL to GET.
	 * @param headers	Request headers; may be empty.
	 * @return	The body of a 2xx response.
	 * @throws IOException	If the request fails or the response isn't 2xx.
	 */
	String get(String url, Map<String, String> headers) throws IOException;

}
