package org.nuaa.tomax.redismonitor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

/**
 * @Author: ToMax
 * @Description:
 * @Date: Created in 2019/5/25 10:20
 */
public class RedisLock {
    private final static String LOCK_KEY = "redis-lock-key";
    private final static int LOCK_EXPIRE_TIME = 40;
    private final static SetParams LOCK_PARAMS = new SetParams().nx().ex(LOCK_EXPIRE_TIME);
    private final static String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private final static String LOCK_SUCCESS = "OK";
    private static final Long UNLOCK_SUCCESS = 1L;

    /**
     * use redis distributed lock realize
     * @param node node to lock
     * @param requestId request id
     * @return lock status
     */
    public static boolean lock(Jedis node, String requestId) {
        return LOCK_SUCCESS.equals(node.set(LOCK_KEY, requestId, LOCK_PARAMS));
    }

    public static boolean unlock(Jedis node, String requestId) {
        return UNLOCK_SUCCESS.equals(node.eval(UNLOCK_SCRIPT, Collections.singletonList(LOCK_KEY), Collections.singletonList(requestId)));
    }

    public static boolean isLocked(Jedis node) {
        return node.exists(LOCK_KEY);
    }
}
