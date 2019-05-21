package org.nuaa.tomax.redismonitor.util;

import redis.clients.jedis.Jedis;

import java.util.stream.Stream;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/19 20:50
 */
public class RedisUtil {
    public static String getRole(Jedis jedis) {
        if (jedis == null) {
            return null;
        }
        String info = jedis.info("replication");
        if (info == null || !info.contains("role")) {
            return null;
        }
        return info.split("\\n")[1].split(":")[1];
    }

    public static boolean isAlive(Jedis jedis) {
        try {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
