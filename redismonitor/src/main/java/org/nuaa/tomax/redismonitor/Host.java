package org.nuaa.tomax.redismonitor;

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

    public Host() {

    }

    public Host(String address, int port) {
        this.address = address;
        this.port = port;
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
