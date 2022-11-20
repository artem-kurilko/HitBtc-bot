package com.project.service;

import com.project.model.Currencies;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private static final HttpClient client = HttpClient.newHttpClient();
    private static HttpRequest request;

    public static void placeOrder(boolean isBuy) throws URISyntaxException, IOException, InterruptedException {
        String side = isBuy ? "buy" : "sell";
        float price, quantity;

        if (isBuy) {
            price = parseFloat(String.valueOf(getAveragePrice() * BUY_PRICE_COEFFICIENT));
            quantity = getCurrencyBalance(Currencies.USDT) / 4;
        } else {
            float purchasePrice = parseFloat(getLastTradeHistory().getString("price"));
            price = purchasePrice * SELL_PRICE_COEFFICIENT;
            quantity = getLastTradeHistory().getFloat("quantity");
        }

        URIBuilder orderUri = new URIBuilder(HITBTC_ORDER_URL);
        orderUri.addParameter("symbol", CURRENCY_PAIR);
        orderUri.addParameter("side", side);
        orderUri.addParameter("price", String.valueOf(price));
        orderUri.addParameter("quantity", String.valueOf(quantity));

        request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(orderUri.build())
                .header("Authorization", getAuthHeader())
                .build();
        int statusCode = getResponse(request).statusCode();
        checkResponseStatusCode(statusCode);
        log.info("Placed order - side: {}, price: {}, quantity: {}", side, price, quantity);
    }

    public static void placeAdditionalBuyOrder() throws URISyntaxException, IOException, InterruptedException {
        JSONObject order = getLastActiveOrder();
        float quantity = order.getFloat("price") * order.getFloat("quantity");
        float price = parseFloat(String.valueOf(getAveragePrice() * BUY_PRICE_COEFFICIENT));
        String side = "buy";
        URIBuilder orderUri = new URIBuilder(HITBTC_ORDER_URL);
        orderUri.addParameter("symbol", CURRENCY_PAIR);
        orderUri.addParameter("side", side);
        orderUri.addParameter("price", String.valueOf(price));
        orderUri.addParameter("quantity", String.valueOf(quantity));

        request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(orderUri.build())
                .header("Authorization", getAuthHeader())
                .build();
        int statusCode = getResponse(request).statusCode();
        checkResponseStatusCode(statusCode);
        log.info("Placed additional buy order - price: {}, quantity: {}", price, quantity);
    }

    public static void cancelOrder(JSONObject order) throws IOException, InterruptedException {
        String clientOrderId = order.getString("client_order_id");
        request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(HITBTC_ORDER_URL + "/" + clientOrderId))
                .header("Authorization", getAuthHeader())
                .build();
        int statusCode = getResponse(request).statusCode();
        checkResponseStatusCode(statusCode);
        log.info("Cancel order with id " + clientOrderId);
    }

    public static JSONArray getBalance() throws IOException, InterruptedException {
        request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(HITBTC_BALANCE_URL))
                .header("Authorization", getAuthHeader())
                .build();
        return new JSONArray(getResponse(request).body());
    }

    private static float getCurrencyBalance(Currencies currency) throws IOException, InterruptedException {
        JSONArray responseBody = getBalance();
        for (int i = 0; i < responseBody.length(); i++) {
            JSONObject currencyBalance = responseBody.getJSONObject(i);
            if (currencyBalance.getString("currency").equals(currency.name()))
                return currencyBalance.getFloat("available");
        } return 0.0f;
    }

    public static JSONArray getActiveOrders() throws IOException, InterruptedException {
        request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(HITBTC_ORDER_URL))
                .header("Authorization", getAuthHeader())
                .build();
        HttpResponse<String> response = getResponse(request);
        checkResponseStatusCode(response.statusCode());
        return new JSONArray(response.body());
    }

    public static JSONObject getLastActiveOrder() throws IOException, InterruptedException {
        return getActiveOrders().getJSONObject(0);
    }

    public static JSONArray getTradesHistory() throws IOException, InterruptedException {
        request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(HITBTC_TRADE_URL))
                .header("Authorization", getAuthHeader())
                .build();
        HttpResponse<String> response = getResponse(request);
        checkResponseStatusCode(response.statusCode());
        return new JSONArray(response.body());
    }

    public static JSONObject getLastTradeHistory() throws IOException, InterruptedException {
        return getTradesHistory().getJSONObject(0);
    }

    public static float getAveragePrice() throws IOException, InterruptedException {
        String url = "https://api.binance.com/api/v3/avgPrice?symbol=" + CURRENCY_PAIR;
        request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = getResponse(request);
        checkResponseStatusCode(response.statusCode());
        String priceValue = new JSONObject(response.body()).getString("price");
        return parseFloat(priceValue);
    }

    public static void checkOrderLifeTime(JSONObject order) throws ParseException, IOException, InterruptedException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String time = order.getString("created_at");
        ZonedDateTime createdAt = ZonedDateTime.ofInstant(formatter.parse(time).toInstant(),
                ZoneId.systemDefault());
        ZonedDateTime expired = createdAt.plusMinutes(3);
        if (expired.equals(ZonedDateTime.now()) || expired.isBefore(ZonedDateTime.now())) {
            log.info("Cancel expired buy order: " + order.getString("client_order_id"));
            cancelOrder(order);
        }
    }

    private static HttpResponse<String> getResponse(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String getAuthHeader() {
        final String API_KEY = "48AEDmQHNHG8WP4-WQ0d83KCf0U-804J";
        final String SECRET_KEY = "1y4wmLtPB_ZaJpwrJ_o54GAvB7eUngEL";
        String valueToEncode = API_KEY + ":" + SECRET_KEY;
        return "Basic " + Base64.getEncoder()
                .encodeToString(valueToEncode.getBytes());
    }

    private static void checkResponseStatusCode(int statusCode) {
        if (statusCode != 200 && statusCode != 201) {
            log.info("Exception with status code " + statusCode);
            throw new RuntimeException();
        }
    }

}
