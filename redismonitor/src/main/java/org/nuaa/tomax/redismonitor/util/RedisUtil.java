package org.nuaa.tomax.redismonitor.util;

import redis.clients.jedis.Jedis;

import java.util.stream.Stream;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/19 20:50
 */
public class RedisUtil {
    private final static String ROLE = "role";
    private final static String REPLICATION = "replication";
    private final static String PONG = "PONG";

    public static String getRole(Jedis jedis) {
        if (jedis == null) {
            return null;
        }
        String info = jedis.info(REPLICATION);
        if (info == null || !info.contains(ROLE)) {
            return null;
        }
        return info.split("\\n")[1].split(":")[1];
    }

    public static boolean isAlive(Jedis jedis) {
        if (jedis == null) {
            return false;
        }
        try {
            return PONG.equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
