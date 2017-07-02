package net.serenitybdd.plugins.jira.zephyr.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HttpClient {
	
	/** HTTP Proxy details FOR FUTURE */
	private static final boolean USE_PROXY = false;
	private static final String PROXY_IP = "xxx.xxx.xxx.xxx";
	private static final int PROXY_PORT = 8080;
	private static final Proxy PROXY = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_IP, PROXY_PORT));
	
	private HttpClient(){}
	
	/**
	 * Send GET request to the specified URL
	 * 
	 * @param url
	 * @throws IOException
	 */
	public static JsonObject httpGetJSONObject(final String url, final String creds) throws IOException {
		return new JsonParser().parse(httpGetJSONString(url,creds)).getAsJsonObject();
	}

	/**
	 * Send GET request to the specified URL
	 * 
	 * @param url
	 * @throws IOException
	 */
	public static JsonArray httpGetJSONArray(final String url,final String creds) throws IOException {
		return new JsonParser().parse(httpGetJSONString(url,creds)).getAsJsonArray();
	}

	/**
	 * Get a string from a url.
	 * 
	 * @param url
	 *            the URL to perform the GET method on
	 * @return a String representing the body of the http response
	 * @throws IOException
	 */
	public static String httpGetJSONString(final String url, final String creds) throws IOException {
		final HttpURLConnection httpCon = createHttpCon(url, creds, "GET");
		final BufferedReader br = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));

		final StringBuilder httpResponse = new StringBuilder();
		String line;
		while (null != (line = br.readLine())) {
			httpResponse.append(line);
		}

		return httpResponse.toString();
	}

	/**
	 * Send a request with JSON content with the specified method
	 * 
	 * @param url
	 *            - the URL to send the request to
	 * @param obj
	 *            - the JSON content to send
	 * @param method
	 *            - e.g. PUT
	 * @throws IOException
	 */
	private static JsonObject sendRequest(final String url, final String creds, final JsonObject obj, final String method)
			throws IOException {
		final HttpURLConnection httpCon = createHttpCon(url, creds, method);

		if (null != obj && !"{}".equalsIgnoreCase(obj.toString())) {
			final OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
			out.write(obj.toString());
			out.close();
		}

		final BufferedReader rd = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
		final StringBuilder result = new StringBuilder();
		String line;
		while (null != (line = rd.readLine())) {
			result.append(line);
		}
		return new JsonParser().parse(result.toString()).getAsJsonObject();
	}

	private static JsonObject sendRequest(final String url, final String creds, final String jsonObj, final String method)
			throws IOException {
		final HttpURLConnection httpCon = createHttpCon(url, creds, method);

		if (null != jsonObj) {
			final OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
			out.write(jsonObj);
			out.close();
		}

		final BufferedReader rd = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
		final StringBuilder result = new StringBuilder();
		String line;
		while (null != (line = rd.readLine())) {
			result.append(line);
		}
		return new JsonParser().parse(result.toString()).getAsJsonObject();
	}

	/**
	 * Send PUT request to the specified URL
	 * 
	 * @param url
	 *            - the URL to send the request to
	 * @param obj
	 *            - the JSON content to send
	 * @throws IOException
	 */
	public static JsonObject put(final String url, final String creds, final JsonObject obj) throws IOException {
		return sendRequest(url, creds, obj, "PUT");
	}

	public static JsonObject put(final String url, final String creds, final String jsonObj) throws IOException {
		return sendRequest(url, creds, jsonObj, "PUT");
	}

	/**
	 * Send POST request to the specified URL
	 * 
	 * @param url
	 *            - the URL to send the request to
	 * @param obj
	 *            - the JSON content to send
	 * @throws IOException
	 */
	public static JsonObject post(final String url, final String creds, final JsonObject obj) throws IOException {
		return sendRequest(url, creds, obj, "POST");
	}

	public static JsonObject post(final String url, final String creds, final String jsonObj) throws IOException {
		return sendRequest(url, creds, jsonObj, "POST");
	}

	/**
	 * Send DELETE request to the specified URL
	 * 
	 * @param url
	 *            - the URL to send the request to
	 * @throws IOException
	 */
	public static JsonObject delete(final String url, final String creds) throws IOException {
		return sendRequest(url, creds, new JsonObject(), "DELETE");
	}

	public static JsonObject delete(final String url, final String creds, final String jsonObj) throws IOException {
		return sendRequest(url, creds, jsonObj, "DELETE");
	}

	/**
	 * Return a HttpURLConnection object for the specified URL and request
	 * method
	 * 
	 * @param url
	 *            the URL to connect to
	 * @param method
	 *            - e.g. GET
	 */
	private static HttpURLConnection createHttpCon(final String url, final String creds, final String method) throws IOException {
		final HttpURLConnection httpCon;
		if (USE_PROXY) {
			httpCon = (HttpURLConnection) new URL(url).openConnection(PROXY);
		} else {
			httpCon = (HttpURLConnection) new URL(url).openConnection();
		}

		httpCon.setDoOutput(true);
		httpCon.setRequestMethod(method);

		if (!creds.isEmpty()) {
			final String encoding = new Base64().encodeToString(creds.getBytes());
			httpCon.setRequestProperty("Authorization", "Basic " + encoding);
		}

		httpCon.setRequestProperty("Content-type", "application/json");

		return httpCon;
	}
}
