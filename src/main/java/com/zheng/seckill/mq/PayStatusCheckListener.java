package com.zheng.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.zheng.seckill.db.dao.OrderDao;
import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.pojo.Order;
import com.zheng.seckill.util.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "pay_check", consumerGroup = "pay_check_group")
public class PayStatusCheckListener implements RocketMQListener<MessageExt> {

    @Autowired
    SeckillActivityDao seckillActivityDao;

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    @Override
    public void onMessage (MessageExt messageExt) {

        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Received order payment status verification message: " + message);
        Order order = JSON.parseObject(message, Order.class);
        //1. Query the order
        Order orderInfo = orderDao.queryOrder(order.getOrderNo());
        if (orderInfo == null) {
            log.info("This order does not exist. Order number: " + order.getOrderNo());
            return;
        }

        //2. Check if payment of the order has been made
        if (orderInfo.getOrderStatus() != 2) {
            //3. Incomplete payment. Close the order.
            log.info("Incomplete payment. Close the order. Order number: " + orderInfo.getOrderNo());
            orderInfo.setOrderStatus(99);
            orderDao.updateOrder(orderInfo);
            //4. Revert stock in the database
            seckillActivityDao.revertStock(order.getSeckillActivityId());
            //4. Revert stock in the redis
            redisService.revertStock("stock: " + order.getSeckillActivityId());

        }

        //2. Deduct stock
        boolean lockStockResult = seckillActivityDao.lockStock(order.getSeckillActivityId());
        if (lockStockResult) {
            order.setOrderStatus(1);
        } else {
            order.setOrderStatus(0);
        }
        orderDao.insertOrder(order);
    }

}
