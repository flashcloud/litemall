package org.linlinjava.litemall.db.domain;

import java.math.BigDecimal;
import java.util.Arrays;

public class OrderGoodsVo {
    private Integer id;
    private String goodsName;
    private String picUrl;
    private Integer goodsId;
    private Integer productId;
    private String[] specifications;
    private Integer number;
    private BigDecimal price;
    private String location;

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String[] getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String[] specifications) {
        this.specifications = specifications;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    private String serial;
    public String getSerial() {
        return serial;
    }
    public void setSerial(String value) {
        this.serial = value;
    }

    private String boundSerial;
    // 此条产品关联到的其他产品的序列号
    public String getBoundSerial() {
        return boundSerial;
    }
    public void setBoundSerial(String value) {
        this.boundSerial = value;
    }

    private Short maxClientsCount;
    // 允许的最大授权计算机站点数
    public Short getMaxClientsCount() {
        return maxClientsCount;
    }
    public void setMaxClientsCount(Short value) {
        this.maxClientsCount = value;
    }

    private Short maxRegisterUsersCount;
    // 允许的最大注册用户数
    public Short getMaxRegisterUsersCount() {
        return maxRegisterUsersCount;
    }
    public void setMaxRegisterUsersCount(Short value) {
        this.maxRegisterUsersCount = value;
    }
}
