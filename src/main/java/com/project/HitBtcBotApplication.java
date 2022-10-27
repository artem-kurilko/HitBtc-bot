package com.project;

import com.project.service.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HitBtcBotApplication {

    public static void main(String[] args) throws Exception {
        log.info("Started bot...");
        OrderServiceImpl orderService = new OrderServiceImpl();
        orderService.getBalance();
    }
}
