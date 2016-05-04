package edu.ucsf.rbvi.bindingDbApp.internal.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;

public class HttpUtils {
	public static Document getXML(String url, DocumentBuilder builder) {
		Document ann = null;

		// Set up our connection
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(url);

		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		CloseableHttpResponse response1 = null;
		try {
			// request.setEntity(new UrlEncodedFormEntity(nvps));
			response1 = client.execute(request);
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			/*
			if (entity1.getContentLength() == 0 || entityStream.available() == 0) {
				System.out.println("No content! ContentLength="+entity1.getContentLength()+
												   "available="+entityStream.available());
				return null;
			}
			*/

			ann = builder.parse(entityStream);

			// and ensure it is fully consumed
			EntityUtils.consume(entity1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response1.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return ann;
	}
}
