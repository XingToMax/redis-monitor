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
    private final static String MASTER = "master";
    private final static String SLAVE = "slave";
    private final String node1Address;
    private final String node2Address;
    private final int node1Port;
    private final int node2Port;

    private Jedis node1;
    private Jedis node2;

    private String role1;
    private String role2;

    private final String requestId;

    public RedisMonitor(String node1Address, String node2Address, int node1Port, int node2Port) {
        this.node1Address = node1Address;
        this.node2Address = node2Address;
        this.node1Port = node1Port;
        this.node2Port = node2Port;
        logger.info("monitor begin to start");
        this.requestId = UUID.randomUUID().toString();
        init();
        try {
            service();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            // wait 5 seconds and work later
            TimeUnit.SECONDS.sleep(5);
            // connect to node 1
            node1 = new Jedis(node1Address, node1Port);
            // connect to node 2
            node2 = new Jedis(node2Address, node2Port);

            role1 = RedisUtil.getRole(node1);
            role2 = RedisUtil.getRole(node2);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        logger.info("monitor start success and request id is {}", requestId);
        logger.info("the role of node1 is {}", role1);
        logger.info("the role of node2 is {}", role2);
    }

    public void service() throws InterruptedException {
        while (true) {
            boolean node1Status = true;
            boolean node2Status = true;
            while (node1Status && node2Status) {
                TimeUnit.SECONDS.sleep(1);
                node1Status = RedisUtil.isAlive(node1);
                node2Status = RedisUtil.isAlive(node2);
            }
            long shutdownTime = System.currentTimeMillis();
            if (!node1Status) {
                logger.info("node1 shutdown");
                if (SLAVE.equals(role2)) {
                    node2.slaveofNoOne();
                    logger.info("node2 switch to master");
                }
                while (!node1Status) {
                    TimeUnit.SECONDS.sleep(1);
                    node1Status = NetworkUtil.isOpen(node1Address);
                }
                logger.info("node1 restart");

                TimeUnit.SECONDS.sleep(5);
                node1 = new Jedis(node1Address, node1Port);
                while (true) {
                    try {
                        if (MASTER.equals(RedisUtil.getRole(node1)) && RedisLock.lock(node1, requestId)) {
                            logger.info("{} lock node1 success", requestId);
                            node1.slaveof(node2Address, node2Port);
                            break;
                        } else {
                            while (RedisLock.isLocked(node1)) {
                                TimeUnit.SECONDS.sleep(1);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        TimeUnit.SECONDS.sleep(1);
                        logger.error(e.getMessage());
                    }
                }
                role1 = RedisUtil.getRole(node1);
                role2 = RedisUtil.getRole(node2);
                logger.info("the service restart of node1 cost {} seconds", (System.currentTimeMillis() - shutdownTime) / 1000);
                logger.info("the role of node1 is {}", role1);
                logger.info("the role of node2 is {}", role2);
            }

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
                TimeUnit.SECONDS.sleep(5);
                node2 = new Jedis(node2Address, node2Port);
                while (true) {
                    try {
                        if (MASTER.equals(RedisUtil.getRole(node2)) && RedisLock.lock(node2, requestId)) {
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
                        TimeUnit.SECONDS.sleep(1);
                        logger.error(e.getMessage());
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

    public static void main(String[] args) throws InterruptedException {
        new RedisMonitor("192.168.163.132", "192.168.163.133", 6379, 6379);
    }
}
