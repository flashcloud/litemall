package org.linlinjava.litemall.core.util;

public class ResponseCode {
    public static final Integer TRADER_NAME_EXIST = 10000;
    public static final Integer TRADER_TAXID_EXIST = 10001;
    public static final Integer TRADER_REFERED_BY_USERS = 10002;
    public static final Integer PHONE_ERR = 10003;
    public static final Integer TRADER_HAS_ORDERS = 10004;
    public static final Integer TRADER_DEL_BY_OTHERS = 10005;
    public static final Integer TRADER_UPDATE_REJECT = 10006;
    public static final Integer TRADER_DEL_LAST = 10007;
    public static final Integer TRADER_SHARE_CODE_NOT_EXIST = 10008;
    public static final Integer TRADER_DEV_NOT_EXIST = TRADER_SHARE_CODE_NOT_EXIST; //软件开发商户不存在
}
