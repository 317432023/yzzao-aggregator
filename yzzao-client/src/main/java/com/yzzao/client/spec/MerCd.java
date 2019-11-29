package com.yzzao.client.spec;

/**
 * 厂商信息代码
 * @author kangtengjiao
 * @version 2018-10-05
 */
public enum MerCd {
    ZL("浙理","10000001"),
    JNDX("江南大学","10000002"),
    BY("泉州佰源机械股份有限公司","10000003"),
    RF("日发","10000004"),
    HQ("恒强","10000005");

    private String title;
    private String value;

    private MerCd(String title,String value) {
        this.title = title;
        this.value = value;
    }
    public String title() {
        return this.title;
    }
    public String value() {
        return this.value;
    }
}
