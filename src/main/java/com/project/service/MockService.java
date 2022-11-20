package com.project.service;

import com.project.model.Currencies;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Slf4j
public class MockService {
    private static float usdtBalance = 10;
    private static float btcBalance = 0.0004f;

    public static void placeOrder(boolean isBuy) throws URISyntaxException, IOException, InterruptedException {
        String side = isBuy ? "buy" : "sell";
        log.info("Placed " + side + " order");
    }

    public static void placeAdditionalBuyOrder() throws URISyntaxException, IOException, InterruptedException {
        log.info("Placed additional buy order");
    }

    public static void cancelOrder(JSONObject order) throws IOException, InterruptedException {
        log.info("Cancel order " + order);
    }

    public static void checkOrderLifeTime(JSONObject order) throws ParseException, IOException, InterruptedException {
        log.info("Check order life time " + order.getString("created_at"));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String time = "2022-11-20T03:19:47.321Z";
        ZonedDateTime createdAt = ZonedDateTime.ofInstant(formatter.parse(time).toInstant(),
                ZoneId.systemDefault());
        ZonedDateTime expired = createdAt.plusMinutes(3);
        if (expired.equals(ZonedDateTime.now()) || expired.isBefore(ZonedDateTime.now()))
            log.info("Cancel expired buy order: " + order.getString("client_order_id"));
        else log.info("Not cancel.");
    }

}
