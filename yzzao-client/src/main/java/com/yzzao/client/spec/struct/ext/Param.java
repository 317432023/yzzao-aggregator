package com.yzzao.client.spec.struct.ext;

public class Param {
    /**参数模块*/
    private String parGrp;
    /**子参数*/
    private String parSub;
    /**参数中英文名称*/
    private String parEnNm;
    /**参数中文名称*/
    private String parCnNm;
    /**数据类型*/
    private String parType;
    /**参数值*/
    private String parVal;

    public String getParType() {
        return parType;
    }
    public void setParType(String parType) {
        this.parType = parType;
    }
    public String getParEnNm() {
        return parEnNm;
    }
    public void setParEnNm(String parEnNm) {
        this.parEnNm = parEnNm;
    }
    public String getParCnNm() {
        return parCnNm;
    }
    public void setParCnNm(String parCnNm) {
        this.parCnNm = parCnNm;
    }
    
    public String getParGrp() {
        return parGrp;
    }
    public void setParGrp(String parGrp) {
        this.parGrp = parGrp;
    }
    public String getParSub() {
        return parSub;
    }
    public void setParSub(String parSub) {
        this.parSub = parSub;
    }
    public String getParVal() {
        return parVal;
    }
    public void setParVal(String parVal) {
        this.parVal = parVal;
    }
}
