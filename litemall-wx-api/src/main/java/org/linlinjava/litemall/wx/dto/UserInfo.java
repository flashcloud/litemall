package org.linlinjava.litemall.wx.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.linlinjava.litemall.db.domain.LitemallGoodsSpecification;
import org.linlinjava.litemall.db.domain.LitemallTrader;

public class UserInfo {
    private String userName;
    private String nickName;
    private String avatarUrl;
    private String country;
    private String province;
    private String city;
    private String language;
    private Byte gender;
    private List<LitemallTrader> managedTraders;

    private String mobile;
    private LocalDateTime addTime;
    private int addDays;
    private String memberPlan;
    private String memberType;
    private String memberExpire;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getMobile() {
        return mobile;
    }
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public LocalDateTime getAddTime() {
        return addTime;
    }
    public void setAddTime(LocalDateTime addTime) {
        this.addTime = addTime;

        //计算addTime到现在的天数
        this.addDays = (int) java.time.Duration.between(addTime, LocalDateTime.now()).toDays();
    }

    public int getAddedDays() {
        return addDays;
    }

    public List<LitemallTrader> getManagedTraders() {
        return managedTraders;
    }

    public void setManagedTraders(List<LitemallTrader> managedTraders) {
        this.managedTraders = managedTraders;
    }

    public void setMemberPlan(String memberPlan) {
        this.memberPlan = memberPlan;
    }

    public String getMemberPlan() {
        return this.memberPlan;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberDes) {
        this.memberType = memberDes;
    }

    public String getMemberExpire() {
        return memberExpire;
    }

    public void setMemberExpire(String memberExpire) {
        this.memberExpire = memberExpire;
    }
}
