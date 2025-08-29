package org.linlinjava.litemall.db.exception;

public class MaxTwoMemberOrderException extends Exception {
    public MaxTwoMemberOrderException() {
        super("已达到用户最多只能有两个会员订单的限制");
    }
    public MaxTwoMemberOrderException(String msg) {
        super(msg);
    }    
}
