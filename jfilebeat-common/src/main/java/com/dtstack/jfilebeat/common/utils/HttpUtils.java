package com.dtstack.jfilebeat.common.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtils {
	
	public static String sendPut(String url, String body, int timeout) throws IOException {
		HttpURLConnection con = null;
		String result = null;
		DataOutputStream out = null;
		BufferedReader in = null;

		try {
			URL realUrl = new URL(url);
			con = (HttpURLConnection) realUrl.openConnection();
			if (body != null) {
				con.setDoInput(true);
				con.setDoOutput(true);
				con.setRequestMethod("PUT");
				con.setRequestProperty("Content-Type", "application/json");
				out = new DataOutputStream(con.getOutputStream());
				out.write(body.getBytes());
				out.flush();
				out.close();
			}

			con.connect();

			in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			String temp = null;
			StringBuilder sb = new StringBuilder();
			while ((temp = in.readLine()) != null) {
				sb.append(temp).append(" ");
			}
			result = sb.toString();
		} finally {
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
		}

		return result;
	}

	public static String sendGet(String url, int timeout) throws IOException {
		HttpURLConnection con = null;
		String result = null;
		DataOutputStream out = null;
		BufferedReader in = null;

		try {
			URL realUrl = new URL(url);
			con = (HttpURLConnection) realUrl.openConnection();
			con.setInstanceFollowRedirects(false);
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");

			con.connect();

			if (con.getResponseCode() == 302) {
				// 如果会重定向，保存302重定向地址，以及Cookies,然后重新发送请求(模拟请求)
				String location = con.getHeaderField("Location");
				return sendGet(location, timeout);
			}
			
			in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			String temp = null;
			StringBuilder sb = new StringBuilder();
			while ((temp = in.readLine()) != null) {
				sb.append(temp);
			}
			result = sb.toString();
		} finally {
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
		}

		return result;
	}

	public static String sendPost(String url, String body, int timeout) throws IOException {
		HttpURLConnection con = null;
		String result = null;
		DataOutputStream out = null;
		BufferedReader in = null;

		try {
			URL realUrl = new URL(url);
			con = (HttpURLConnection) realUrl.openConnection();
			if (body != null) {
				con.setInstanceFollowRedirects(false);
				con.setDoInput(true);
				con.setDoOutput(true);
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/json");

				out = new DataOutputStream(con.getOutputStream());
				out.write(body.getBytes());
				out.flush();
				out.close();
			}

			con.connect();

			if (con.getResponseCode() == 302) {
				// 如果会重定向，保存302重定向地址，以及Cookies,然后重新发送请求(模拟请求)
				String location = con.getHeaderField("Location");

				return sendPost(location, body, timeout);
			}

			in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			String temp = null;
			StringBuilder sb = new StringBuilder();
			while ((temp = in.readLine()) != null) {
				sb.append(temp);
			}
			result = sb.toString();
		} finally {
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
		}

		return result;
	}


}
