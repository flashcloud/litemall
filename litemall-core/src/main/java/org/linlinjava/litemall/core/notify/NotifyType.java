package org.linlinjava.litemall.core.notify;

public enum NotifyType {
    SUBMIT_ORDER("submitOrder"),
    PAY_SUCCEED("paySucceed"),
    ORDER_MEMBER("orderMember"),
    SHIP("ship"),
    REFUND_REQ("refund"),
    REFUND_SUCCESS("refundSuccess"),
    CAPTCHA("captcha");

    private String type;

    NotifyType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
