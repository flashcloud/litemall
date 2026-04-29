package org.linlinjava.litemall.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "litemall.wx")
public class WxProperties {

    private String appId;

    private String appSecret;

    // 微信支付专用的appid（如果与小程序/公众号appid不同）
    private String payAppId;
    private String payAppSecret;

    private String mchId;

    private String mchKey;

    private String notifyUrl;

    private String keyPath;

    private String privateKeyPath;

    private String privateCertPath;

    private String apiV3Key;

    private String certSerialNo;

    private String token;

    private String aesKey;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getMchKey() {
        return mchKey;
    }

    public void setMchKey(String mchKey) {
        this.mchKey = mchKey;
    }

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取微信支付使用的appid
     * 如果配置了payAppId则使用payAppId，否则使用appId
     * @return 微信支付appid
     */
    public String getPayAppId() {
        return this.payAppId != null && !this.payAppId.isEmpty() ? this.payAppId : this.appId;
    }

    public void setPayAppId(String payAppId) {
        this.payAppId = payAppId;
    }

    public String getPayAppSecret() {
        return this.payAppSecret != null && !this.payAppSecret.isEmpty() ? this.payAppSecret : this.appSecret;
    }

    public void setPayAppSecret(String payAppSecret) {
        this.payAppSecret = appSecret;
    }    

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPrivateCertPath() {
        return privateCertPath;
    }

    public void setPrivateCertPath(String privateCertPath) {
        this.privateCertPath = privateCertPath;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public void setApiV3Key(String apiV3Key) {
        this.apiV3Key = apiV3Key;
    }

    public String getCertSerialNo() {
        return certSerialNo;
    }

    public void setCertSerialNo(String certSerialNo) {
        this.certSerialNo = certSerialNo;
    }
}
