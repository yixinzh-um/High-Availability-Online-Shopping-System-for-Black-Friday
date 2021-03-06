package com.zheng.seckill;

import com.zheng.seckill.db.dao.OrderDao;
import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.mapper.SeckillActivityMapper;
import com.zheng.seckill.db.pojo.Order;
import com.zheng.seckill.db.pojo.SeckillActivity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class DaoTest {
    @Resource
    private SeckillActivityMapper seckillActivityMapper;
    @Autowired
    private SeckillActivityDao seckillActivityDao;
    @Test
    public void SeckillActivityTest() {
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName("测试");
        seckillActivity.setCommodityId(999L);
        seckillActivity.setTotalStock(100L);
        seckillActivity.setSeckillPrice(new BigDecimal(99));
        seckillActivity.setActivityStatus(16);
        seckillActivity.setOldPrice(new BigDecimal(99));
        seckillActivity.setAvailableStock(100);
        seckillActivity.setLockStock(0L);
        seckillActivityMapper.insert(seckillActivity);
        System.out.println("====>>>>" + seckillActivityMapper.selectByPrimaryKey(1L));
    }

    @Test
    public void setSeckillActivityQuery() {
        List<SeckillActivity> seckillActivitys = seckillActivityDao.querySeckillActivitysByStatus(0);
        System.out.println(seckillActivitys.size());
        seckillActivitys.stream().forEach(seckillActivity -> System.out.println(seckillActivity.toString()));
    }

    @Test
    public void seckillActivityMapperUpdate() {
        System.out.println(seckillActivityDao.querySeckillActivityById(Long.parseLong("9")));
        seckillActivityMapper.lockStock(Long.valueOf("9"));
        System.out.println(seckillActivityDao.querySeckillActivityById(Long.parseLong("9")));
    }

    @Test
    public void seckillActivityDaoUpdate() {
        System.out.println(seckillActivityDao.querySeckillActivityById(Long.parseLong("9")));
        seckillActivityDao.lockStock(Long.valueOf("9"));
        System.out.println(seckillActivityDao.querySeckillActivityById(Long.parseLong("9")));
    }

    @Autowired
    OrderDao orderDao;

    @Test
    public void orderDaoTest() {
        Order order = orderDao.queryOrder("524743559841189888");
        System.out.println(order);
    }

}