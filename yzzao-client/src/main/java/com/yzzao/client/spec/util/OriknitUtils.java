package com.yzzao.client.spec.util;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.yzzao.client.spec.MerCd;
import com.yzzao.client.spec.var.ComSeqNoCounter;
import com.yzzao.common.utils.StringUtil;

public final class OriknitUtils {
    public static String comSeqNo() {
        //return String.format("%04x", ComSeqNoCounter.getAndIncrement());//string.format多线程不安全
        int i = ComSeqNoCounter.getAndIncrement();
        byte[] bs = StringUtil.intToBytes2(i);
        return StringUtil.bytesToHex(new byte[]{bs[2],bs[3]});
    }
    public static String hexTs(Date date) {
        int i = (int)(date.getTime()/1000);
        //return String.format("%08x", i);//string.format多线程不安全
        byte[] bbb = StringUtil.intToBytes2(i);
        return StringUtil.bytesToHex(bbb);
    }
    /**
     * 当前时间戳(十六进制字符串)
     */
    public static String curHexTs() {
        int i = (int)(System.currentTimeMillis()/1000);
        //return String.format("%08x", i);//string.format多线程不安全
        byte[] bbb = StringUtil.intToBytes2(i);
        return StringUtil.bytesToHex(bbb);
    }
    /**
     * 请求方厂商代码
     * @return
     */
    public static String merTitleOfReq() {
        return MerCd.BY.title();
    }
    /**
     * 请求方厂商代码
     * @return
     */
    public static String merNoOfReq() {
        return MerCd.BY.value();
    }
    /**
     * 响应方厂商代码
     * @return
     */
    public static String merNoOfResp() {
        return MerCd.ZL.value();
    }
    /**
     * 设备类型代码
     * @return
     */
    public static String devTyNoReq() {
        //TODO 设备类型代码将来要动态确定
        return "018001";//大圆机
    }
    public static String devTyNoResp() {
        return "000000";//对方是服务器,不是纺织机械
    }
    /**
     * 设备序列号
     * @return
     */
    public static String devSeqNoReq() {
        return "000000000000001004";
    }
    public static String devSeqNoReq(int gauge) {
        //TODO 设备序列号将来要动态确定(打开会话的设备序列号固定, 但是传输机台信息的设备序列号是动态的)
        return "0000000000"+StringUtil.bytesToHex(StringUtil.intToBytes2(gauge));
        //return "000000000000001004";
    }
    public static String devSeqNoResp() {
        //TODO 设备序列号将来要动态确定(打开会话的设备序列号固定, 但是传输机台信息的设备序列号是动态的)
        return "000000000000000001";
    }
    
    /**
     * 密码/协议版本
     * @param str
     * @return
     */
    public static String str2lenHexstr(String str, String encoding) {
        if(str==null || str.length()==0 || str.length() > 0xffff /*65535*/) return "0000";
        String _hexstr = StringUtils.isBlank(encoding)?StringUtil.str2HexStr(str):StringUtil.str2HexStr(str,encoding);
        int len = _hexstr.length()/2;
        //String lenstr = String.format("%04x", len);//string.format多线程不安全
        byte[]  bs = StringUtil.intToBytes2(len);
        String lenstr = StringUtil.bytesToHex(new byte[]{bs[2],bs[3]});
        return lenstr+_hexstr;
    }
    
    /**
     * 协议版本
     * @param passwd
     * @return
     */
    public static String ver(String ver) {
        if(ver==null || ver.length()==0 || ver.length() > 0xffff /*65535*/) return "0000";
        String passwdhex = StringUtil.str2HexStr(ver);
        int len = passwdhex.length()/2;
        //String lenstr = String.format("%04x", len);//string.format多线程不安全
        byte[]  bs = StringUtil.intToBytes2(len);
        String lenstr = StringUtil.bytesToHex(new byte[]{bs[2],bs[3]});
        return lenstr+passwdhex;
    }
    
    /**
     * MES 设备号转上报设备序列号
     * @param machineID
     * @return
     */
    public static int machineID2DevSeq(int machineID) {
        return 0x1004+(machineID-1);
    }
    
    public static void main(String[] args) {
        /*System.out.println(str2lenHexstr("123456"));
        System.out.println(curHexTs());
        System.out.println(OriknitUtils.str2lenHexstr("V.1.1"));*/
        System.out.println(curHexTs());
    }
}
