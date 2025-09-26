package org.linlinjava.litemall.db.domain;

import lombok.Data;

@Data
public class BankAccountInfo {
    private Integer id;
    private String bankType;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String bankCode;
    private String logo;
}
