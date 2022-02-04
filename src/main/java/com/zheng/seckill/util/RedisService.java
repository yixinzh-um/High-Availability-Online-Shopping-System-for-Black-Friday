package com.zheng.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private JedisPool jedisPool;

    public void setValue(String key, Long value) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.set(key, value.toString());
        jedisClient.close();
    }

    public void setValue(String key, String value) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.set(key, value.toString());
        jedisClient.close();
    }

    public String getValue(String key) {
        Jedis jedisClient = jedisPool.getResource();
        String value = jedisClient.get(key);
        jedisClient.close();
        return value;
    }

    /**
     * Stock deduction validation in cache
     *
     * @param key
     * @return boolean value
     * @throws Exception
     */
    public boolean stockDeductValidator(String key) {
        try (Jedis jedisClient = jedisPool.getResource()) {
            String script = "if redis.call('exists',KEYS[1]) == 1 then\n" +
                    " local stock = tonumber(redis.call('get', KEYS[1]))\n" +
                    " if( stock <=0 ) then\n" +
                    " return -1\n" +
                    " end;\n" +
                    " redis.call('decr',KEYS[1]);\n" +
                    " return stock - 1;\n" +
                    " end;\n" +
                    " return -1;";
            Long stock = (Long) jedisClient.eval(script,
                    Collections.singletonList(key), Collections.emptyList());
            System.out.println(stock);
            if (stock < 0) {
                System.out.println("out of stock");
                return false;
            } else {
                System.out.println("Congratulations! You have successfully placed the order");
            }
            return true;
        } catch (Throwable throwable) {
            System.out.println("Failed to deduct the stock：" + throwable.toString());
            return false;
        }
    }

    /**
     * Redis stock rollback for payment timeout
     */
    public void revertStock(String key) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.incr(key);
        jedisClient.close();
    }


    public boolean isInLimitMember(long seckillActivityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        boolean sismember = jedisClient.sismember("seckillActivity_users: " + seckillActivityId, String.valueOf(userId));
        jedisClient.close();
        log.info("userId: {}, activityId: {}, already in purchase list: {}", userId, seckillActivityId, sismember);
        return sismember;
    }

    public void addLimitMember(long seckillActivityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.sadd("seckillActivity_users: " + seckillActivityId, String.valueOf(userId));
        jedisClient.close();
    }

    public void removeLimitMember(Long seckillActivityId, Long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.srem("seckillActivity_users: " + seckillActivityId, String.valueOf(userId));
        jedisClient.close();
    }

    /**
     * Get the distributed lock
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */

    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime) {
        Jedis jedisClient = jedisPool.getResource();
        //params - NX|XX,
        // NX -- Only set the key if it does not already exist.
        // XX -- Only set the key if it already exist.
        // EX|PX, expire time units:
        // EX = seconds;
        // PX = milliseconds
        String result = jedisClient.set(lockKey, requestId, "NX", "PX", expireTime);
        jedisClient.close();
        if ("OK".equals(result)) {
            return true;
        }
        return false;
    }
    /**
     * Release the distributed lock
     *
     * @param lockKey
     * @param requestId
     * @return is released or not
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis jedisClient = jedisPool.getResource();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        Long result = (Long) jedisClient.eval(script,
                Collections.singletonList(lockKey), Collections.singletonList(requestId));
        jedisClient.close();
        if (result == 1L) {
            return true;
        }
        return false;
    }

}