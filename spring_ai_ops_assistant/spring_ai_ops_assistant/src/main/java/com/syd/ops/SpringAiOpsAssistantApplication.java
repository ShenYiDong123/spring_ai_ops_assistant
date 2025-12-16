package com.syd.ops;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SpringAiOpsAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiOpsAssistantApplication.class, args);
    }

}
