package com.project.service;

import com.project.model.Currencies;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import static com.project.model.HitBtcAPI.*;
import static java.lang.Float.parseFloat;

//FIXME average price may not be taking correctly, try to take hitbtc avg
@Slf4j
public class OrderService {
    private static final String CURRENCY_PAIR = "BTCUSDT";
    private static final float BUY_PRICE_COEFFICIENT = 0.99f;
    private static final float SELL_PRICE_COEFFICIENT = 1.01f;
    private static final OkHttpClient client = new OkHttpClient();
    private static Request request;

    public static void placeOrder(boolean isBuy) throws IOException {
        String side = isBuy ? "buy" : "sell";
        float price, quantity;
        if (isBuy) {
            price = parseFloat(String.valueOf(getAveragePrice() * BUY_PRICE_COEFFICIENT));
            quantity = (getCurrencyBalance(Currencies.USDT) / 4) / price;
        } else {
            float purchasePrice = parseFloat(getLastTradeHistory().getString("price"));
            price = purchasePrice * SELL_PRICE_COEFFICIENT;
            quantity = getLastTradeHistory().getFloat("quantity");
        }
        createOrder(side, price, quantity);
        log.info("Placed order - side: {}, price: {}, quantity: {}", side, price, quantity);
    }

    public static void placeAdditionalBuyOrder() throws IOException {
        float quantity = getLastActiveOrder().getFloat("quantity");
        float price = parseFloat(String.valueOf(getAveragePrice() * BUY_PRICE_COEFFICIENT));
        String side = "buy";
        createOrder(side, price, quantity);
        log.info("Placed additional buy order - price: {}, quantity: {}", price, quantity);
    }

    private static void createOrder(String side, float price, float quantity) throws IOException {
        HttpUrl.Builder orderUri = HttpUrl.parse(HITBTC_ORDER_URL).newBuilder();
        String orderUrl = orderUri.build().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("symbol", CURRENCY_PAIR)
                .add("side", side)
                .add("price", String.valueOf(price))
                .add("quantity", String.valueOf(quantity))
                .build();

        request = new Request.Builder()
                .url(orderUrl)
                .post(formBody)
                .header("Authorization", getAuthHeader())
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        checkResponseStatusCode(response);
        response.close();
    }

    public static void cancelOrder(JSONObject order) throws IOException {
        String clientOrderId = order.getString("client_order_id");
        request = new Request.Builder()
                .url(HITBTC_ORDER_URL + "/" + clientOrderId)
                .delete()
                .header("Authorization", getAuthHeader())
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        checkResponseStatusCode(response);
        response.close();
        log.info("Cancel order with id " + clientOrderId);
    }

    public static JSONArray getBalance() throws IOException {
        request = new Request.Builder()
                .url(HITBTC_BALANCE_URL)
                .get()
                .header("Authorization", getAuthHeader())
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        checkResponseStatusCode(response);
        JSONArray balance = new JSONArray(response.body().string());
        response.close();
        return balance;
    }

    private static float getCurrencyBalance(Currencies currency) throws IOException {
        JSONArray responseBody = getBalance();
        for (int i = 0; i < responseBody.length(); i++) {
            JSONObject currencyBalance = responseBody.getJSONObject(i);
            if (currencyBalance.getString("currency").equals(currency.name()))
                return currencyBalance.getFloat("available");
        } return 0.0f;
    }

    public static JSONArray getActiveOrders() throws IOException {
        request = new Request.Builder()
                .url(HITBTC_ORDER_URL)
                .get()
                .header("Authorization", getAuthHeader())
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        checkResponseStatusCode(response);
        JSONArray activeOrders = new JSONArray(response.body().string());
        response.close();
        return activeOrders;
    }

    public static JSONObject getLastActiveOrder() throws IOException {
        JSONArray actOrders = getActiveOrders();
        int orderNumber = actOrders.length()-1;
        return actOrders.getJSONObject(orderNumber);
    }

    public static JSONArray getTradesHistory() throws IOException {
        request = new Request.Builder()
                .url(HITBTC_TRADE_URL)
                .get()
                .header("Authorization", getAuthHeader())
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        checkResponseStatusCode(response);
        JSONArray tradesHistory = new JSONArray(response.body().string());
        response.close();
        return tradesHistory;
    }

    public static JSONObject getLastTradeHistory() throws IOException {
        return getTradesHistory().getJSONObject(0);
    }

    public static float getAveragePrice() throws IOException {
        String url = "https://api.binance.com/api/v3/avgPrice?symbol=" + CURRENCY_PAIR;
        request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        checkResponseStatusCode(response);

        String priceValue = new JSONObject(response.body().string()).getString("price");
        response.close();
        return parseFloat(priceValue);
    }

    public static void checkOrderLifeTime(JSONObject order) throws ParseException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String time = order.getString("created_at");
        ZonedDateTime createdAt = ZonedDateTime.ofInstant(formatter.parse(time).toInstant(),
                ZoneId.systemDefault()).plusHours(2); //fix that api send utc instead utc+2 time
        ZonedDateTime expired = createdAt.plusMinutes(3);
        if (expired.equals(ZonedDateTime.now()) || expired.isBefore(ZonedDateTime.now())) {
            log.info("Cancel expired buy order.");
            cancelOrder(order);
        }
    }

    private static String getAuthHeader() {
        final String API_KEY = "48AEDmQHNHG8WP4-WQ0d83KCf0U-804J";
        final String SECRET_KEY = "1y4wmLtPB_ZaJpwrJ_o54GAvB7eUngEL";
        String valueToEncode = API_KEY + ":" + SECRET_KEY;
        return "Basic " + Base64.getEncoder()
                .encodeToString(valueToEncode.getBytes());
    }

    private static void checkResponseStatusCode(Response response) {
        int statusCode = response.code();
        if (statusCode != 200 && statusCode != 201) {
            log.info("Exception code: {}, message: {}", statusCode, response.body());
            throw new RuntimeException();
        }
    }

}
