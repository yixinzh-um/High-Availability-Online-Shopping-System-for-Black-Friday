package com.zheng.seckill.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.zheng.seckill.db.dao.OrderDao;
import com.zheng.seckill.db.dao.SeckillActivityDao;
import com.zheng.seckill.db.dao.SeckillCommodityDao;
import com.zheng.seckill.db.pojo.Order;
import com.zheng.seckill.db.pojo.SeckillActivity;
import com.zheng.seckill.db.pojo.SeckillCommodity;
import com.zheng.seckill.service.SeckillActivityService;
import com.zheng.seckill.util.RedisService;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class SeckillActivityController {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    private SeckillActivityService seckillActivityService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private RedisService redisService;

    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
        @RequestParam("name") String name,
        @RequestParam("commodityId") long commodityId,
        @RequestParam("seckillPrice") BigDecimal seckillPrice,
        @RequestParam("oldPrice") BigDecimal oldPrice,
        @RequestParam("seckillNumber") long seckillNumber,
        @RequestParam("startTime") String startTime,
        @RequestParam("endTime") String endTime,
        Map<String, Object> resultMap
    ) throws ParseException {
        startTime = startTime.substring(0, 10) + startTime.substring(11);
        endTime = endTime.substring(0, 10) + endTime.substring(11);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(oldPrice);
        seckillActivity.setTotalStock(seckillNumber);
        seckillActivity.setAvailableStock(new Integer("" + seckillNumber));
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));
        seckillActivityDao.inertSeckillActivity(seckillActivity);
        resultMap.put("seckillActivity", seckillActivity);
        return "add_success";
    }


    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity";
    }

    @RequestMapping("/seckills")
    public String activityList(Map<String, Object> resultMap) {
        try(Entry entry = SphU.entry("seckills")) {
            List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
            resultMap.put("seckillActivities", seckillActivities);
            return "seckill_activity";
        } catch (BlockException e) {
            log.error("Querying the list of seckill activities is limited. " + e.toString());
            return "wait";
        }
    }



    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(Map<String, Object> resultMap, @PathVariable long seckillActivityId) {
        SeckillActivity seckillActivity;
        SeckillCommodity seckillCommodity;

        String seckillActivityInfo = redisService.getValue("seckillActivity: " + seckillActivityId);
        if (StringUtils.isNotEmpty(seckillActivityInfo)) {
            log.info("Redis cache data: " + seckillActivityInfo);
            seckillActivity = JSON.parseObject(seckillActivityInfo, SeckillActivity.class);
        } else {
            seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        }

        String seckillCommodityInfo = redisService.getValue("seckillCommodity: " + seckillActivity.getCommodityId());
        if (StringUtils.isNotEmpty(seckillCommodityInfo)) {
            log.info("Redis cache data: " + seckillCommodityInfo);
            seckillCommodity = JSON.parseObject(seckillCommodityInfo, SeckillCommodity.class);
        } else {
            seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        }

        resultMap.put("seckillActivity", seckillActivity);
        resultMap.put("seckillCommodity", seckillCommodity);
        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
        resultMap.put("oldPrice", seckillActivity.getOldPrice());
        resultMap.put("commodityId", seckillActivity.getCommodityId());
        resultMap.put("commodityName", seckillCommodity.getCommodityName());
        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());
        return "seckill_item";
    }

    /**
     * deal with seckill order request,
     * @param userId
     * @param seckillActivityId
     * @return
     */
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(@PathVariable long userId, @PathVariable long seckillActivityId) {
        boolean stockValidateResult = false;


        ModelAndView modelAndView = new ModelAndView();
        try (Entry entry = SphU.entry("seckillBuy")){
            // check whether the user is in the purchased list
            if (redisService.isInLimitMember(seckillActivityId, userId)) {
                // notify the user already in the purchased list, return the result
                modelAndView.addObject("resultInfo", "Sorry, you are already in the purchased list");
                modelAndView.setViewName("seckill_result");
                return modelAndView;
            }

            //Verify whether the seckill can be performed
            stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
            if (stockValidateResult) {
                Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                modelAndView.addObject("resultInfo",
                        "Seckill successful, order creating, order ID: " + order.getOrderNo());
                modelAndView.addObject("orderNo", order.getOrderNo());
                redisService.addLimitMember(seckillActivityId, userId);
            } else {
                modelAndView.addObject("resultInfo", "Sorry, this item is out of stock");
            }
        } catch (FlowException e) {
            log.error("No hurries. Please try again later. " + e.toString());
            modelAndView.setViewName("wait");
            return modelAndView;
        } catch (Exception e) {
            log.error("There is something wrong with seckill system, " + e.toString());
            modelAndView.addObject("resultInfo", "Failed to seckill");
        }
        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    /**
     * query the order
     * @param orderNo
     * @return
     */
    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView orderQuery(@PathVariable String orderNo) {
        log.info("Query the order， order Number: " + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();

        if (order != null) {
            modelAndView.setViewName("order");
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.setViewName("order_wait");
        }
        return modelAndView;
    }

    /**
     * make the payment for the order
     * @param orderNo
     * @return
     */
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(@PathVariable String orderNo) throws Exception {
        seckillActivityService.payOrderProcess(orderNo);
        return "redirect:/seckill/orderQuery/" + orderNo;
    }

    @ResponseBody
    @RequestMapping("seckill/getSystemTime")
    public String getSystemTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format((new Date()));
        return date;
    }
}