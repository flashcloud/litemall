package org.linlinjava.litemall.db.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TraderOrderGoodsVo {
    private Integer id;
    private Integer parentOrderId;
    private Integer orderId;
    private String orderSn;
    private Short orderStatus;
    private String orderStatusText;
    private Integer traderId;
    private LocalDateTime payTime;
    private String companyName;
    private String companyNickname;
    private String taxid;
    private String goodsName;
    private String keywords;
    private String goodsTypeName;
    private String[] specifications;
    private String serial;
    private String boundSerial;
    private BigDecimal price;
    private Short maxClientsCount;
    private Short maxRegisterUsersCount;
    private Integer[] hasRegisterUserIds;
    private LocalDateTime expDateTime;
    private Byte payType;
    private String payTypeName;
    private String payVoucherUrl;
    private String invoiceUrl;
    private String buyer;

    @Override
    public String toString() {
        return "TraderOrderGoodsVo{" +
                "parentOrderId=" + parentOrderId +
                ", orderId=" + orderId +
                ", traderId='" + traderId + '\'' +
                ", companyName=" + companyName +
                ", goodsName=" + goodsName +
                ", specifications=" + specifications.toString() +
                ", price=" + price +
                '}';
    }
}
