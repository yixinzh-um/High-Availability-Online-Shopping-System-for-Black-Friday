package com.zheng.seckill.service;

import com.alibaba.fastjson.JSON;
import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.pojo.Order;
import com.zheng.seckill.db.pojo.SeckillActivity;
import com.zheng.seckill.mq.RocketMQService;
import com.zheng.seckill.util.RedisService;
import com.zheng.seckill.util.SnowFlake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillActivityService {
    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;

    private SnowFlake snowFlake = new SnowFlake(1, 1);

    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        /**
         * 1. create order
         */
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();
        //generate order id by snowflake
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(seckillActivity.getId());
        order.setUserId(userId);
        order.setOrderAmount(seckillActivity.getSeckillPrice().longValue());

        /**
         * 2. send the message of creating order
         */
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));
        return order;
    }

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
