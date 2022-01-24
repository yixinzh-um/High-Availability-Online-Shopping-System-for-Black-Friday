package com.zheng.seckill.service;

import com.alibaba.fastjson.JSON;
import com.zheng.seckill.db.dao.OrderDao;
import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.pojo.Order;
import com.zheng.seckill.db.pojo.SeckillActivity;
import com.zheng.seckill.mq.RocketMQService;
import com.zheng.seckill.util.RedisService;
import com.zheng.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {
    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;

    @Autowired
    private OrderDao orderDao;

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
        /**
         * 3.Send order payment status verification message
         * messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m
         30m 1h 2h
         */
        rocketMQService.sendDelayMessage("pay_check", JSON.toJSONString(order), 3);
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

    /** Process order Payment
     * @param orderNo order number
     * @return
     */

    public void payOrderProcess(String orderNo) {
        log.info("Payment made. Order : " + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        boolean deductStockResult = seckillActivityDao.deductStock(order.getSeckillActivityId());
        if (deductStockResult) {
            order.setPayTime(new Date());
            order.setOrderStatus(2);
            orderDao.updateOrder(order);
        }
    }
}
