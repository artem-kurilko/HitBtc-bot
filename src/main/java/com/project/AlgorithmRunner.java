package com.project;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import static com.project.service.OrderServiceImpl.*;
import static java.lang.Float.parseFloat;

@Slf4j
public class AlgorithmRunner {

    public void runAlgorithm() throws Exception {
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
                log.info("No active orders. Place buy order.");
                placeOrder(true);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        AlgorithmRunner al = new AlgorithmRunner();
        al.checkOrderLifeTime(getActiveOrders().getJSONObject(0));
    }

    private void checkOrderLifeTime(JSONObject order) {
        String time = order.getString("createdAt");
        System.out.println(time);
    }
}
