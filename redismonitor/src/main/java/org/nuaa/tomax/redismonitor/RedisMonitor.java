package org.nuaa.tomax.redismonitor;

import org.nuaa.tomax.redismonitor.util.NetworkUtil;
import org.nuaa.tomax.redismonitor.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/19 20:25
 */
public class RedisMonitor {
    private static Logger logger = LoggerFactory.getLogger(Context.class);
    /**
     * master role
     */
    private final static String MASTER = "master";
    /**
     * slave role
     */
    private final static String SLAVE = "slave";
    /**
     * default redis port
     */
    public final static int DEFAULT_REDIS_PORT = 6379;

    private final String node1Address;
    private final String node2Address;
    private final int node1Port;
    private final int node2Port;

    private volatile Jedis node1;
    private volatile Jedis node2;

    private String role1;
    private String role2;

    private final String requestId;

    private static volatile boolean run = false;

    public RedisMonitor(String node1Address, String node2Address, int node1Port, int node2Port) {
        this.node1Address = node1Address;
        this.node2Address = node2Address;
        this.node1Port = node1Port;
        this.node2Port = node2Port;
        this.requestId = UUID.randomUUID().toString();
    }

    public void start() {
        logger.info("monitor begin to start");
        run = true;
        init();
        try {
            service();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (node1 != null) {
            node1.close();
        }

        if (node2 != null) {
            node2.close();
        }
    }

    private void init() {
        try {
            // wait some seconds and work later to ensure redis is working
            TimeUnit.SECONDS.sleep(20);
            // connect to node 1
            node1 = new Jedis(node1Address, node1Port);
            // connect to node 2
            node2 = new Jedis(node2Address, node2Port);

            // get roles of nodes and record
            role1 = RedisUtil.getRole(node1);
            role2 = RedisUtil.getRole(node2);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        logger.info("monitor start success and request id is {}", requestId);
        logger.info("the role of node1 is {}", role1);
        logger.info("the role of node2 is {}", role2);
    }

    private void service() throws InterruptedException {
        // loop and check run signal
        while (run) {
            // init nodes status
            boolean node1Status = true;
            boolean node2Status = true;

            // check nodes status and stop until more than one node is not alive
            while (node1Status && node2Status) {
                TimeUnit.SECONDS.sleep(1);
                node1Status = RedisUtil.isAlive(node1);
                node2Status = RedisUtil.isAlive(node2);
            }
            // record node shutdown time
            long shutdownTime = System.currentTimeMillis();

            // node 1 is not alive
            if (!node1Status) {
                logger.info("node1 shutdown");

                // node2 was slave and node1 is shutdown, switch node2 to master
                if (SLAVE.equals(role2)) {
                    node2.slaveofNoOne();
                    logger.info("node2 switch to master");
                }

                // loop and check node1 status until node1 is connecting
                while (!node1Status) {
                    TimeUnit.SECONDS.sleep(1);
                    node1Status = NetworkUtil.isOpen(node1Address);
                }
                logger.info("node1 restart");

                // loop and make node1 slave of node2 until success
                while (true) {
                    try {
                        // reconnect node1
                        node1.close();
                        node1 = new Jedis(node1Address, node1Port);

                        // node1 is slave and break
                        if (SLAVE.equals(RedisUtil.getRole(node1))) {
                            break;
                        }

                        // use redis distributed lock
                        if (RedisLock.lock(node1, requestId)) {
                            // lock success, execute slave of command
                            logger.info("{} lock node1 success", requestId);
                            node1.slaveof(node2Address, node2Port);
                            break;
                        } else {
                            // not get the lock and wait the locked monitor release the lock
                            while (RedisLock.isLocked(node1)) {
                                TimeUnit.SECONDS.sleep(1);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        TimeUnit.SECONDS.sleep(5);
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
                }

                // get and record roles
                role1 = RedisUtil.getRole(node1);
                role2 = RedisUtil.getRole(node2);
                logger.info("the service restart of node1 cost {} seconds", (System.currentTimeMillis() - shutdownTime) / 1000);
                logger.info("the role of node1 is {}", role1);
                logger.info("the role of node2 is {}", role2);
            }

            // node 2 is not alive and the step is similar to node 1
            if (!node2Status) {
                logger.info("node2 shutdown");
                if (SLAVE.equals(role1)) {
                    node1.slaveofNoOne();
                    logger.info("node1 switch to master");
                }

                while (!node2Status) {
                    TimeUnit.SECONDS.sleep(1);
                    node2Status = NetworkUtil.isOpen(node2Address);
                }
                logger.info("node2 restart");

                while (true) {
                    try {
                        node2.close();
                        node2 = new Jedis(node2Address, node2Port);

                        if (SLAVE.equals(RedisUtil.getRole(node2))) {
                            break;
                        }
                        if (RedisLock.lock(node2, requestId)) {
                            logger.info("{} lock node2 success", requestId);
                            node2.slaveof(node1Address, node1Port);
                            break;
                        } else {
                            while (RedisLock.isLocked(node2)) {
                                TimeUnit.SECONDS.sleep(1);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        TimeUnit.SECONDS.sleep(5);
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
                }

                role1 = RedisUtil.getRole(node1);
                role2 = RedisUtil.getRole(node2);
                logger.info("the service restart of node2 cost {} seconds", (System.currentTimeMillis() - shutdownTime) / 1000);
                logger.info("the role of node1 is {}", role1);
                logger.info("the role of node2 is {}", role2);
            }
        }
    }

    public static void stop() {
        run = false;
    }

    public static void main(String[] args) throws InterruptedException {
        // read args
        if (args.length != 4 && args.length != 2) {
            throw new IllegalArgumentException("need 4 params(host1 port1 host2 port2) or 2 params (host1, host2 ) and use default port");
        }
        String host1 = null;
        String host2 = null;
        int port1 = 0;
        int port2 = 0;
        if (args.length == 2) {
            // TODO : host address check
            host1 = args[0];
            host2 = args[1];
        } else {
            try {
                host1 = args[0];
                port1 = Integer.parseInt(args[1]);
                host2 = args[2];
                port2 = Integer.parseInt(args[2]);
            } catch (Exception e) {
                throw new IllegalArgumentException("port should be int");
            }
        }

        // get monitor instance
        RedisMonitor monitor = new RedisMonitor(host1, host2, port1, port2);

        // start monitor
        new Thread(monitor::start, "monitor-thread").start();

        // add destroy hook
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::destroy));

        // TODO : add stop signal listener
    }
}
