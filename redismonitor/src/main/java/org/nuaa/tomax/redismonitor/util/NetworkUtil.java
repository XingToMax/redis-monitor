package org.nuaa.tomax.redismonitor.util;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/19 20:40
 */
public class NetworkUtil {
    private final static int TIMEOUT = 500;
    /**
     * check host is open or not
     * @param ip host ip
     * @return open state
     */
    public static boolean isOpen(String ip) {
        try {
            return InetAddress.getByName(ip).isReachable(TIMEOUT);
        } catch (IOException e) {
            System.out.println(ip + " not open");
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(isOpen("192.168.163.132"));
    }
}
