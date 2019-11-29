package com.yzzao.mesmid.json.pojo;

public class MESOrder {
    private String cd;
    private String appid;
    private String appsecret;
    private MESOrderDetail[] data;
    public String getCd() {
        return cd;
    }
    public void setCd(String cd) {
        this.cd = cd;
    }
    public String getAppid() {
        return appid;
    }
    public void setAppid(String appid) {
        this.appid = appid;
    }
    public String getAppsecret() {
        return appsecret;
    }
    public void setAppsecret(String appsecret) {
        this.appsecret = appsecret;
    }
    public MESOrderDetail[] getData() {
        return data;
    }
    public void setData(MESOrderDetail[] data) {
        this.data = data;
    }
    
}
