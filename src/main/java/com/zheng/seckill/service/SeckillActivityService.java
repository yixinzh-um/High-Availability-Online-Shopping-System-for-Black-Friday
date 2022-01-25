package com.zheng.seckill.service;

import com.alibaba.fastjson.JSON;
import com.zheng.seckill.db.dao.OrderDao;
import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.dao.SeckillCommodityDao;
import com.zheng.seckill.db.pojo.Order;
import com.zheng.seckill.db.pojo.SeckillActivity;
import com.zheng.seckill.db.pojo.SeckillCommodity;
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

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

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

    /** Push seckill infomation to Redis
     * @param seckillActivityId seckill activity id
     * @return
     */

    public void pushSeckillInfoToRedis(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        redisService.setValue("seckillActivity: " +seckillActivityId, JSON.toJSONString(seckillActivity));

        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        redisService.setValue("seckillCommodity: " + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }


    /** Process order Payment
     * @param orderNo order number
     * @return
     */

    public void payOrderProcess(String orderNo) throws Exception {
        log.info("Payment made. Order : " + orderNo);
        Order order = orderDao.queryOrder(orderNo);

        /**
         * 1. check if the order exists or not
         * 2. check if the order status is paid or not
         */

        // The order does not exist
        if (order == null) {
            log.error("The order corresponding to the order number does not exist: " + orderNo);
            return;
        }
        // The order exists, but its status is invalid (not paid)
        if (order.getOrderStatus() != 1) {
            log.error("Invalid order status: " + orderNo);
            return;
        }

        // The order exists with valid status
        order.setPayTime(new Date());
        // order status 0 : out of stock, invalid order
        //              1 : order created, wait for payment
        //              2 : payment completed

        order.setOrderStatus(2);
        orderDao.updateOrder(order);

        /**
         * 3. Send order payment success message
         */
        rocketMQService.sendMessage("pay_done", JSON.toJSONString(order));
    }
}
