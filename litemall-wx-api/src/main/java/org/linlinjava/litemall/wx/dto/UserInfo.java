package org.linlinjava.litemall.wx.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;

public class UserInfo {
    private String userName;
    private String nickName;
    private String avatarUrl;
    private String country;
    private String province;
    private String city;
    private String language;
    private Byte gender;
    private String accessToken;
    private String sessionKey;
    private String wxOpenId;
    private List<LitemallTrader> managedTraders;
    private List<LitemallTrader> traders;

    private String mobile;
    private LocalDateTime addTime;
    private int addDays;

    public static UserInfo cloneFromUser(LitemallUser user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(user.getUsername());
        userInfo.setNickName(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatar());
        userInfo.setGender(user.getGender());
        userInfo.setMobile(user.getMobile());
        userInfo.setAddTime(user.getAddTime());
        return userInfo;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getWxOpenId() {
        return wxOpenId;
    }

    public void setWxOpenId(String wxOpenId) {
        this.wxOpenId = wxOpenId;
    }

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

    public List<LitemallTrader> getTraders() {
        return traders;
    }

    public void setTraders(List<LitemallTrader> traders) {
        this.traders = traders;
    }
}
