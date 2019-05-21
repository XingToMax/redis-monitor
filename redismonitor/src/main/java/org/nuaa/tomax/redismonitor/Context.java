package org.nuaa.tomax.redismonitor;

import java.util.List;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/21 22:05
 */
public class Context {
    private volatile List<Host> hosts;
    private volatile Host master;
    private final String monitorConfigPath;

    public Context(String monitorConfigPath) {
        this.monitorConfigPath = monitorConfigPath;
        init();
    }

    public void init() {

    }
}
