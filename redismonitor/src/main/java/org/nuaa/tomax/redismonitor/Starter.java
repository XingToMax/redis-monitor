package org.nuaa.tomax.redismonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }
}
