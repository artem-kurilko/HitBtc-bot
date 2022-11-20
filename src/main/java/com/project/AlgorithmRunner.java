package com.project;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.project.service.OrderService.*;
import static java.lang.Float.parseFloat;

@Slf4j
public class AlgorithmRunner {

    public static void main(String[] args) throws Exception {
        log.info("Bot is running...");

        while (true) {
            JSONArray activeOrders = getActiveOrders();
            if (activeOrders.length()!=0) {
                try {
                    JSONObject actOrder = getLastActiveOrder();
                    if (actOrder.getString("side").equals("sell")) {
                        // check that price didn't drop >= 5%, if did then place additional buy order
                        float sellPrice = actOrder.getFloat("price");
                        if (sellPrice * 0.94 >= getAveragePrice())
                            placeAdditionalBuyOrder();
                    } else {
                        float cumQuantity = parseFloat(actOrder.getString("quantity_cumulative"));
                        if (cumQuantity == 0.0)
                            checkOrderLifeTime(actOrder);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            } else {
                log.info("No active orders. Place order.");
                try {
                    String side = getLastTradeHistory().getString("side");
                    boolean orderSide = side.equals("sell");
                    placeOrder(orderSide);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
            Thread.sleep(1000);
        }
    }

}
