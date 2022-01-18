package com.zheng.seckill.controller;

import com.zheng.seckill.service.SeckillOverSellService;
import com.zheng.seckill.service.SeckillActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SecKillOverSellController {
    @Autowired
    private SeckillOverSellService secKillOverSellService;

    @Autowired
    private SeckillActivityService seckillActivityService;
    /**
     * simple Processing snap requests
     * @param seckillActivityId
     * @return
     */

    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckillCommodity(@PathVariable long seckillActivityId) {
        boolean stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
        return stockValidateResult ? "Congradulations, you have placed the order successfully" : "This item is out of stock";
    }

//    @ResponseBody
//    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckill(@PathVariable long seckillActivityId) {
        return secKillOverSellService.processSeckill(seckillActivityId);
    }
}
