package org.nuaa.tomax.redismonitor;

import org.nuaa.tomax.redismonitor.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/21 21:49
 */
public class Starter {
    private static Logger logger = LoggerFactory.getLogger(Starter.class);
    public final static String MONITOR_CONFIG_PATH = "/monitor.conf";
    public static void main(String[] args) {
        String monitorConfigPath = args.length == 0 ? MONITOR_CONFIG_PATH : args[0];
        logger.info("redis monitor start, load config {}", monitorConfigPath);

        Context context = new Context(monitorConfigPath);

        //
        Runtime.getRuntime().addShutdownHook(new Thread(context::destroy));
    }
}
