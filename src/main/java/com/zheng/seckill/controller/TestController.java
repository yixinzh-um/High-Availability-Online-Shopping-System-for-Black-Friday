package com.zheng.seckill.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Controller
public class TestController {

    @ResponseBody
    @RequestMapping("hello")
    public String hello() {
        String result;
        //resource name
        try (Entry entry = SphU.entry("HelloResource")) {
            //protected service logic
            result = "Hello Sentinel";
            return result;

        } catch (BlockException e) {
            // The resource access is blocked, limited or downgraded
            log.error(e.toString());
            result = "System is busy, please try again later";
            return result;
        }
    }

    /**
     * Define current limiting rules
     * 1. Create a collection of current limiting rules
     * 2. Create current limiting rules
     * 3. Put the current limiting rules in the collection
     * 4. Load current limiting rules
     * @PostConstruct Executed after the constructor of the current class is executed
     */

    @PostConstruct
    public void seckillsFlow() {
        log.info("rules");
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("seckills");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);

        FlowRule rule2 = new FlowRule();
        rule2.setResource("HelloResource");
        rule2.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule2.setCount(2);

        FlowRule rule3 = new FlowRule();
        rule2.setResource("seckillBuy");
        rule2.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule2.setCount(100);

        rules.add(rule);
        rules.add(rule2);
        rules.add(rule3);

        FlowRuleManager.loadRules(rules);
    }
}
