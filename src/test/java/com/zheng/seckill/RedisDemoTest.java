package com.zheng.seckill;

import com.zheng.seckill.util.RedisService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class RedisDemoTest {
    @Resource
    private RedisService redisService;

    @Order(1)
    @Test
    public void stockTest() {
        redisService.setValue("stock: 19",10L);
    }
    @Order(2)
    @Test
    public void getStockTest() {
        String stock = redisService.getValue("stock: 19");
        System.out.println(stock);
    }
    @Order(3)
    @Test
    public void stockDeductValidatorTest() {
        boolean result = redisService.stockDeductValidator(("stock: 19"));
        System.out.println("result " + result);
        String stock = redisService.getValue("stock: 19");
        System.out.println("stock: "+stock);
    }


}
