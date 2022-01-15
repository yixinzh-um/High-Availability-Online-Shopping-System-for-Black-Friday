package com.zheng.seckill.service;

import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.pojo.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecKillOverSellService {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    public String processSeckill(long activityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(activityId);
        long availableStock = seckillActivity.getAvailableStock();
        String result;
        if (availableStock > 0) {
            result = "Congratulations, you have placed the order successfully.";
            System.out.println(result);
            availableStock = availableStock - 1;
            seckillActivity.setAvailableStock((new Integer("" + availableStock)));
            seckillActivityDao.updateSeckillActivity(seckillActivity);
        } else {
            result = "Sorry, this item is no longer available.";
            System.out.println(result);
        }
        return result;
    }
}
