package com.yzzao.mesmid.v2.struct;

import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.util.Crc16Util;

public class Message {
    private byte frameHead;//1byte
    private short length;//2bytes
    private byte mainCd;//1byte
    private byte subCd;//1byte
    private byte[] par;//nbyte
    private short verify;//2byte
    private short frameTail;//2byte
    
    // 20181207 System.arrayCopy不是线程安全的
    public byte[] composeFull() {
        final int n = par.length;
        byte[] array = new byte[1+2+1+1+n+2+2];
        synchronized(Message.class) {
	        System.arraycopy(new byte[]{frameHead}, 0, array, 0, 1);
	        System.arraycopy(StringUtil.intToBytes2(length), 2, array, 0+1, 2);
	        System.arraycopy(new byte[]{mainCd}, 0, array, 0+1+2, 1);
	        System.arraycopy(new byte[]{subCd}, 0, array, 0+1+2+1, 1);
	        if(n>0) System.arraycopy(par, 0, array, 0+1+2+1+1, n);
	        System.arraycopy(StringUtil.intToBytes2(verify), 2, array, 0+1+2+1+1+n, 2);
	        System.arraycopy(StringUtil.intToBytes2(frameTail), 2, array, 0+1+2+1+1+n+2, 2);
        }
        return array;
    }
    
    public String hexComposeFull() {
        /*StringBuffer sbd = new StringBuffer();
        sbd.append(StringUtil.bytesToHex(new byte[]{frameHead}))
        .append(StringUtil.bytesToHex(StringUtil.intToBytes2(length)).substring(2))
        .append(StringUtil.bytesToHex(new byte[]{mainCd}))
        .append(StringUtil.bytesToHex(new byte[]{subCd}))
        .append(StringUtil.bytesToHex(par))
        .append(StringUtil.bytesToHex(StringUtil.intToBytes2(verify)).substring(2))
        .append(StringUtil.bytesToHex(StringUtil.intToBytes2(frameTail)).substring(2))
        ;
        
        return sbd.toString();*/
        return StringUtil.bytesToHex(composeFull());
    }
    
    public byte getFrameHead() {
        return frameHead;
    }
    public void setFrameHead(byte frameHead) {
        this.frameHead = frameHead;
    }
    public short getLength() {
        return length;
    }
    public void setLength(short length) {
        this.length = length;
    }
    public byte getMainCd() {
        return mainCd;
    }
    public void setMainCd(byte mainCd) {
        this.mainCd = mainCd;
    }
    public byte getSubCd() {
        return subCd;
    }
    public void setSubCd(byte subCd) {
        this.subCd = subCd;
    }
    public byte[] getPar() {
        return par;
    }
    public void setPar(byte[] par) {
        this.par = par;
    }
    public short getVerify() {
        return verify;
    }
    public void setVerify(short verify) {
        this.verify = verify;
    }
    public short getFrameTail() {
        return frameTail;
    }
    public void setFrameTail(short frameTail) {
        this.frameTail = frameTail;
    }
    
    public Message(){}
    public Message(byte mainCd, byte subCd, byte[] par) {
        if(0x7FFF<par.length) System.err.println("par length must not greater than 32767(0x7fff).");
        frameHead = (byte)0xEF;
        frameTail = (short)0xCECF;
        this.mainCd = mainCd;
        this.subCd = subCd;
        this.par = par;
        length = (short)par.length;
        short[] data = new short[4 + length];
        data[0] = (short)((length & 0xff00) >> 8);
        data[1] = (short)(length & 0xff) ;// 将byte符号为当数字使用
        data[2] = (short)(this.mainCd&0xff);
        data[3] = (short)(this.subCd&0xff);
        for(int i=0; i<length; i++) {
            data[4+i] =  (short)(this.par[i]&0xff);
        }
        verify = Crc16Util.getCrc16(data);
    }
    
    public static void main(String[] args) {
        byte a = (byte)0xaa;
        System.out.println(StringUtil.bytesToHex(new byte[]{a}));
        System.out.println(a);
        System.out.println(Integer.parseInt(StringUtil.bytesToHex(new byte[]{a}), 16));
        System.out.println(8 & 0xff00 >> 8);
        System.out.println((8 & 0xff00) >> 8);
        
        int i = a & 0xff;
        System.out.println(i);
        
        System.out.println(String.format("%02d",0x2));
        
        Message message = new Message((byte)0x80, (byte)0x03, new byte[]{(byte)0x30,(byte)0x02});
       System.out.println(StringUtil.bytesToHex( message.composeFull()));
        
    }
}
