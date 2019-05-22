package org.nuaa.tomax.redismonitor;

import org.nuaa.tomax.redismonitor.util.RedisUtil;
import redis.clients.jedis.Jedis;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/21 22:10
 */
public class Host {
    private String address;
    private int port;
    private Jedis jedis;

    public Host() {}

    public Host(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public Host(String addr) {
        String[] cells = addr.split(":");
        this.address = cells[0];
        this.port = Integer.parseInt(cells[1]);
    }

    public String connect() {
        jedis = new Jedis(address, port);
        if (RedisUtil.isAlive(jedis)) {
            return RedisUtil.getRole(jedis);
        }
        return null;
    }

    public String reconnect() {
        if (jedis != null) {
            jedis.close();
            jedis = null;
        }
        return connect();
    }

    public boolean isAlive() {
        return RedisUtil.isAlive(jedis);
    }

    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Jedis getJedis() {
        return jedis;
    }

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }
}
