package com.zheng.seckill.db.dao;

import com.zheng.seckill.db.pojo.Order;

public interface OrderDao {

    public void insertOrder(Order order);

    public Order queryOrder(String orderNo);

    public void updateOrder(Order order);
}
