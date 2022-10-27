package com.project.model;

public interface HitBtcAPI {
    String HITBTC_BASIC_URL = "https://api.hitbtc.com",
    HITBTC_ORDER_URL = HITBTC_BASIC_URL + "/api/3/spot/order",
    HITBTC_BALANCE_URL = HITBTC_BASIC_URL + "/api/3/spot/balance",
    HITBTC_TRADE_URL = HITBTC_BASIC_URL + "/api/3/spot/history/trade";
}
