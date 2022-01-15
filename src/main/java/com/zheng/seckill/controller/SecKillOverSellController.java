package com.zheng.seckill.controller;

import com.zheng.seckill.service.SecKillOverSellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SecKillOverSellController {
    @Autowired
    private SecKillOverSellService secKillOverSellService;

    /**
     * simple Processing snap requests
     * @param seckillActivityId
     * @return
     */

    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckill(@PathVariable long seckillActivityId) {
        return secKillOverSellService.processSeckill(seckillActivityId);
    }

}
