package org.notima.businessobjects.adapter.financialinfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * {@link HttpFetcher} on the JDK's {@link HttpClient}.
 */
public class JdkHttpFetcher implements HttpFetcher {

	private final HttpClient client;
	private final Duration timeout;

	public JdkHttpFetcher(Duration timeout) {
		this.timeout = timeout;
		this.client = HttpClient.newBuilder()
				.connectTimeout(timeout)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	@Override
	public String get(String url, Map<String, String> headers) throws IOException {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET();
		if (headers != null) headers.forEach(b::header);
		HttpResponse<String> response;
		try {
			response = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted: " + url, e);
		}
		int status = response.statusCode();
		if (status < 200 || status >= 300) {
			String hint = status == 429 ? " (too many requests: the source limits calls without a subscription key)" : "";
			throw new IOException("HTTP " + status + hint + ": " + url);
		}
		return response.body();
	}

}
