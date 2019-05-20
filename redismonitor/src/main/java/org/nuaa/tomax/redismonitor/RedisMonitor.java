package org.nuaa.tomax.redismonitor;

import org.nuaa.tomax.redismonitor.util.NetworkUtil;
import org.nuaa.tomax.redismonitor.util.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/19 20:25
 */
public class RedisMonitor {
    private final String node1Address;
    private final String node2Address;
    private final int node1Port;
    private final int node2Port;

    private Jedis node1;
    private Jedis node2;

    private String role1;
    private String role2;

    public RedisMonitor(String node1Address, String node2Address, int node1Port, int node2Port) {
        this.node1Address = node1Address;
        this.node2Address = node2Address;
        this.node1Port = node1Port;
        this.node2Port = node2Port;

        init();
        try {
            service();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        // connect to node 1
        node1 = new Jedis(node1Address, node1Port);
        // connect to node 2
        node2 = new Jedis(node2Address, node2Port);

        role1 = RedisUtil.getRole(node1);

        role2 = RedisUtil.getRole(node2);
    }

    public void service() throws InterruptedException {
        while (true) {
            boolean node1Status = true;
            boolean node2Status = true;
            while (node1Status && node2Status) {
                TimeUnit.SECONDS.sleep(1);
                node1Status = NetworkUtil.isOpen(node1Address);
                node2Status = NetworkUtil.isOpen(node2Address);
            }

            if (!node1Status) {
                System.out.println("node1 shutdown");
                node2.slaveofNoOne();

                while (!node1Status) {
                    TimeUnit.SECONDS.sleep(1);
                    node1Status = NetworkUtil.isOpen(node1Address);
                }
                System.out.println("node1 restart");

                TimeUnit.SECONDS.sleep(5);
                node1 = new Jedis(node1Address, node1Port);
                while (true) {
                    try {
                        node1.slaveof(node2Address, node2Port);
                    } catch (Exception e) {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    break;
                }

                System.out.println(RedisUtil.getRole(node1));
                System.out.println(RedisUtil.getRole(node2));
                System.out.println();
            }

            if (!node2Status) {
                System.out.println("node2 shutdown");
                node1.slaveofNoOne();

                while (!node2Status) {
                    TimeUnit.SECONDS.sleep(1);
                    node2Status = NetworkUtil.isOpen(node2Address);
                }
                System.out.println("node2 restart");
                TimeUnit.SECONDS.sleep(5);
                node2 = new Jedis(node2Address, node2Port);
                while (true) {
                    try {
                        node2.slaveof(node1Address, node1Port);
                    } catch (Exception e) {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new RedisMonitor("192.168.163.132", "192.168.163.133", 6379, 6379);
    }
}
