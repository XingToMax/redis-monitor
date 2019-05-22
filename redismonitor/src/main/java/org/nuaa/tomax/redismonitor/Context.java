package org.nuaa.tomax.redismonitor;

import org.nuaa.tomax.redismonitor.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/21 22:05
 */
public class Context {

    private static Logger logger = LoggerFactory.getLogger(Context.class);

    private volatile List<Host> hosts;
    private volatile List<Host> aliveHosts;

    private volatile Host master;
    private final String monitorConfigPath;
    private final Properties config;

    public Context(String monitorConfigPath) {
        this.monitorConfigPath = monitorConfigPath;
        config = PropertyUtil.loadProperties(monitorConfigPath);
        hosts = new ArrayList<>(8);
        aliveHosts = new ArrayList<>(8);
        init();
    }

    public void init() {
        logger.info("init context");
        List<String> list = getAddressList();
        for (String in : list) {
            hosts.add(new Host(in));
        }

        logger.info("begin to connect");
        int connectCount = 0;
        for (Host host : hosts) {
            String result = host.connect();
            if (result == null) {
                if ("master".equals(result)) {
                    master = host;
                }
                logger.info("connect to {}:{}({}) success", host.getAddress(), host.getPort(), result);
                connectCount++;
                aliveHosts.add(host);
            } else {
                logger.info("connect to {}:{} fail");
            }
        }
        logger.info("{} / {} hosts connect success", connectCount, hosts.size());
        if (master == null) {
            // TODO : choose the master
            logger.info("choose {} as master", "");
        }

        if (connectCount == 0) {
            // TODO : connect num is not enough , exit
        }

        // TODO : schedule and ping each host
    }
    
    public void destroy() {
        logger.info("exit.");
        System.exit(0);
    }

    private List<String> getAddressList() {
        String hosts = PropertyUtil.getString(config, "hosts");
        hosts = hosts.substring(1, hosts.length());
        return Arrays.asList(hosts.split("\\s+"));
    }
}
