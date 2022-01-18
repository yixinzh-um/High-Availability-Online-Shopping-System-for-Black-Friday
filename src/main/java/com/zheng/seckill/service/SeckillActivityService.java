package com.zheng.seckill.service;

import com.zheng.seckill.util.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillActivityService {
    @Autowired
    private RedisService redisService;

    /**
     * judge if there are still stocks available
     * @param activityId seckill activity id
     * @return boolean value
     */

    public boolean seckillStockValidator(long activityId) {
        String key = "stock: " + activityId;
        return redisService.stockDeductValidator(key);
    }

}
