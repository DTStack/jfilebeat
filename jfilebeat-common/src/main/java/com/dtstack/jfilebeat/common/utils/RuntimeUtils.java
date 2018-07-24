package com.dtstack.jfilebeat.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class RuntimeUtils {

	private static InetAddress addr;
	private static String localIps;

	public static Set<InetAddress> currentLocalAddresses() {
		Set<InetAddress> addrs = new HashSet<InetAddress>();
		Enumeration<NetworkInterface> ns = null;
		try {
			ns = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {

		}
		while (ns != null && ns.hasMoreElements()) {
			NetworkInterface n = ns.nextElement();
			Enumeration<InetAddress> is = n.getInetAddresses();
			while (is.hasMoreElements()) {
				InetAddress i = is.nextElement();
				if (!i.isLoopbackAddress() && !i.isLinkLocalAddress() && !i.isMulticastAddress()
						&& !isSpecialIp(i.getHostAddress()))
					addrs.add(i);
			}
		}
		return addrs;
	}

	public static String currentHostName() {
		try {
			if (addr == null) {
				addr = InetAddress.getLocalHost();
			}
			return addr.getHostName();
		} catch (Exception e) {

		}
		return "unknown";
	}

	public static String currentLocalIps() {
		if (localIps == null) {
			Set<InetAddress> addrs = currentLocalAddresses();
			if(addrs.isEmpty()) {
				return "";
			}
			List<String> ret = new ArrayList<String>();
			int k = 0;
			for (InetAddress addr : addrs) {
				String ar = addr.getHostAddress();
				if (!ret.contains(ar)) {
					if (k == 0) {
						localIps = ar;
					} else {
						localIps += "," + ar;
					}
				}
			}
		}

		return localIps;
	}

	private static boolean isSpecialIp(String ip) {
		if (ip.contains(":"))
			return true;
		if (ip.startsWith("127."))
			return true;
		if (ip.startsWith("169.254."))
			return true;
		if (ip.equals("255.255.255.255"))
			return true;
		return false;
	}

	public static String currentOS() {
		return System.getProperty("os.name");
	}

	public static void main(String[] args) {
		System.out.println(currentLocalAddresses());
		System.out.println(currentLocalIps());
		System.out.println(currentHostName());
		System.out.println(currentOS());
	}

}
