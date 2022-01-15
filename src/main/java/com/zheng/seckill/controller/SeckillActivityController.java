package com.zheng.seckill.controller;

import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.dao.SeckillCommodityDao;
import com.zheng.seckill.db.pojo.SeckillActivity;
import com.zheng.seckill.db.pojo.SeckillCommodity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Controller
public class SeckillActivityController {

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;


    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity";
    }

}