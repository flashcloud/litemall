package org.linlinjava.litemall.db.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TraderOrderGoodsVo {
    private Integer id;
    private Integer orderId;
    private Integer traderId;
    private LocalDateTime payTime;
    private String companyName;
    private String companyNickname;
    private String taxid;
    private String goodsName;
    private String[] specifications;
    private String serial;
    private String boundSerial;
    private BigDecimal price;
    private Short maxClientsCount;
    private Short maxRegisterUsersCount;
    private Integer[] hasRegisterUserIds;

    @Override
    public String toString() {
        return "TraderOrderGoodsVo{" +
                "orderId=" + orderId +
                ", traderId='" + traderId + '\'' +
                ", companyName=" + companyName +
                ", goodsName=" + goodsName +
                ", specifications=" + specifications.toString() +
                ", price=" + price +
                '}';
    }
}
