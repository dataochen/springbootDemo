package org.egg.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @Description 获取IP最后"."后的数字
 */
public class IpUtil {
	private static String ip;
    private static String wholeIp;  //完整IP
	private static final Logger Log = LoggerFactory.getLogger(IpUtil.class);

	static {
		try {
			@SuppressWarnings("static-access")
			String _ip = getAddress().getLocalHost().getHostAddress();
			Integer temp = new Integer(_ip.substring(_ip.lastIndexOf(".") + 1));
			ip = String.format("%03d", temp);
            wholeIp = _ip;
		} catch (UnknownHostException e) {
			Log.error("获取本地IP异常", e);
		}
	}

	public static void main(String[] args) {
		System.out.println(IpUtil.getLocalIp());
		System.out.println(IpUtil.getWholeIp());
	}

	private static InetAddress getAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
					continue;
				}
				Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
				if (addresses.hasMoreElements()) {
					return addresses.nextElement();
				}
			}
		} catch (SocketException e) {
			Log.info("getAddress exception",e);
		}
		return null;
	}

	public static String getLocalIp() {
		return ip;
	}

    /**
     * 获得完整IP
     * @return
     */
    public static String getWholeIp() {
        return wholeIp;
    }
}
