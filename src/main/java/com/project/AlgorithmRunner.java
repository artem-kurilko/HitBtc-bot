package com.project;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

import static com.project.service.OrderServiceImpl.*;
import static java.lang.Float.parseFloat;

@Slf4j
public class AlgorithmRunner {
    private static final int ORDER_LIFETIME_IN_SECONDS = 180;

    public static void main(String[] args) throws Exception {
        log.info("Bot is running...");

        while (true) {
            JSONArray activeOrders = getActiveOrders();
            if (activeOrders.length()!=0) {
                JSONObject actOrder = activeOrders.getJSONObject(0);

                if (actOrder.getString("side").equals("sell")) {
                    // check that price didn't drop >= 5%, if did then place additional buy order
                    float price = parseFloat(actOrder.getString("price"));
                    placeAdditionalBuyOrder();
                } else {
                    float cumQuantity = parseFloat(actOrder.getString("cumQuantity"));
                    if (cumQuantity==0.0)
                        checkOrderLifeTime(actOrder);
                }
            } else {
                String side = getLastTradeHistory().getString("side");

                log.info("No active orders. Place buy order.");
                placeOrder(true);
            }
        }
    }

    private static void checkOrderLifeTime(JSONObject order) throws ParseException, IOException, InterruptedException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String time = order.getString("created_at");
        System.out.println(formatter.parse(time));
        Instant createdAt = formatter.parse(time).toInstant();
        Instant expired = createdAt.plusSeconds(ORDER_LIFETIME_IN_SECONDS);
        if (expired.equals(Instant.now()) || expired.isAfter(Instant.now())) {
            log.info("Expired buy order: " + order.getString("client_order_id"));
            cancelOrder(order);
        }
    }
}
