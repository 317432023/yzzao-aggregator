package com.yzzao.common.utils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil
{
  public static String str2HexStr(String str)
  {
    StringBuffer sb = new StringBuffer( 100 );
    byte[] bs = str.getBytes();

    char[] chars = "0123456789abcdef".toCharArray();
    for (int i = 0; i < bs.length; i++) {
      int bit = (bs[i] & 0xF0) >> 4;
      sb.append(chars[bit]);
      bit = bs[i] & 0xF;
      sb.append(chars[bit]);
    }
    return sb.toString();
  }
  
  public static String str2HexStr(String str, String encoding)
  {
    byte[] bs = new byte[0];
    try {
      bs = str.getBytes(encoding);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return bytesToHex(bs);
  }

  public static byte[] hexToBytes(String hexString)
  {
    if ((hexString == null) || (hexString.equals(""))) {
      return null;
    }
    
    int length = hexString.length() / 2;
    char[] hexChars = hexString.toCharArray();
    byte[] bytes = new byte[length];
    String hexDigits = "0123456789abcdef";
    for (int i = 0; i < length; i++) {
      int pos = i * 2;
      int h = hexDigits.indexOf(hexChars[pos]) << 4;
      int l = hexDigits.indexOf(hexChars[(pos + 1)]);
      if ((h == -1) || (l == -1)) {
        return null;
      }
      bytes[i] = ((byte)(h | l));
    }
    return bytes;
  }
 
  public static String bytesToHex(byte[] src)
  {
    if ((src == null) || (src.length <= 0)) {
      return null;
    }
    
    StringBuffer stringBuffer = new StringBuffer(100);
    for (int i = 0; i < src.length; i++)
    {
      String str = Integer.toHexString(src[i] & 0xFF);
      if (str.length() < 2) {
        stringBuffer.append(0);
      }
      stringBuffer.append(str);
    }
    return stringBuffer.toString();
  }

  public static byte[] intToBytes(int value)
  {
    byte[] src = new byte[4];
    src[3] = ((byte)(value >> 24 & 0xFF));
    src[2] = ((byte)(value >> 16 & 0xFF));
    src[1] = ((byte)(value >> 8 & 0xFF));
    src[0] = ((byte)(value & 0xFF));
    return src;
  }

  public static byte[] intToBytes2(int value)
  {
    byte[] src = new byte[4];
    src[0] = ((byte)(value >> 24 & 0xFF));
    src[1] = ((byte)(value >> 16 & 0xFF));
    src[2] = ((byte)(value >> 8 & 0xFF));
    src[3] = ((byte)(value & 0xFF));
    return src;
  }
  
  public static byte[] longToBytes2(long value)
  {
    byte[] src = new byte[8];
    src[0] = ((byte)(int)(value >> 56 & 0xFF));
    src[1] = ((byte)(int)(value >> 48 & 0xFF));
    src[2] = ((byte)(int)(value >> 40 & 0xFF));
    src[3] = ((byte)(int)(value >> 32 & 0xFF));
    src[4] = ((byte)(int)(value >> 24 & 0xFF));
    src[5] = ((byte)(int)(value >> 16 & 0xFF));
    src[6] = ((byte)(int)(value >> 8 & 0xFF));
    src[7] = ((byte)(int)(value & 0xFF));
    return src;
  }
  









  public static int bytesToInt(byte[] src, int offset)
  {
    int value = src[offset] & 0xFF | 
      (src[(offset + 1)] & 0xFF) << 8 | 
      (src[(offset + 2)] & 0xFF) << 16 | 
      (src[(offset + 3)] & 0xFF) << 24;
    return value;
  }
  



  public static int bytesToInt2(byte[] src, int offset)
  {
    int value = (src[offset] & 0xFF) << 24 | 
      (src[(offset + 1)] & 0xFF) << 16 | 
      (src[(offset + 2)] & 0xFF) << 8 | 
      src[(offset + 3)] & 0xFF;
    return value;
  }
 
  public static byte[] merge(byte[] data1, byte[] data2)
  {
    byte[] data3 = new byte[data1.length + data2.length];
    synchronized(StringUtil.class) {
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
    }
    return data3;
  }
  
  public static void main(String[] args) throws UnsupportedEncodingException {
    String ssbs = "00 6A 00 09 00 00 00 23 FD 00 00 00 00 00 00 00 00 00 04 04 B0 00 00 00 00 00 00 00 00 07 D0 00 00 04 4C 00 00 B8 00 00 00 00 00 00 00 00 00 00 00 00 E2 60 D6 B1 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 FC FC";
    byte[] bs = hexToBytes("00 6A 00 09 00 00 00 23 FD 00 00 00 00 00 00 00 00 00 04 04 B0 00 00 00 00 00 00 00 00 07 D0 00 00 04 4C 00 00 B8 00 00 00 00 00 00 00 00 00 00 00 00 E2 60 D6 B1 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 FC FC".replaceAll(" ", "").toLowerCase());
    String s = bytesToHex(bs);
    System.out.println(s);
    System.out.println(new String(bs, "GBK"));
    
    String ssbs2 = "0006313233343536";
    byte[] bs2 = hexToBytes("0006313233343536");
    byte[] arrayOfByte1; int j = (arrayOfByte1 = bs2).length; for (int i = 0; i < j; i++) { byte b = arrayOfByte1[i];
      System.out.println((char)b);
    }
    String s2 = bytesToHex(bs2);
    System.out.println(s2);
    
    String ssbs3 = "0808080a";
    byte[] bs3 = hexToBytes("0808080a");
    Date d = new Date();
    System.out.println(Integer.valueOf("0808080a", 16));
    d.setTime(Integer.valueOf("0808080a", 16).intValue() * 1000);
    System.out.println(new SimpleDateFormat("yyyyMMddHHmmss").format(d));
    
    int i = (int)(System.currentTimeMillis() / 1000L);
    byte[] bbb = intToBytes2(i);
    System.out.println(bytesToHex(bbb));
    String ssss = String.format("%08x", new Object[] { Integer.valueOf(i) });
    System.out.println(ssss);
    
    long l = 268435457L;
    System.out.println(String.format("%08x", new Object[] { Long.valueOf(l) }));
    l = Long.parseLong("10000001", 16);
    
    System.out.println(String.format("%04x", new Object[] { Integer.valueOf(1) }));
    
    System.out.println(Long.valueOf("10000001", 16));
    System.out.println(String.format("%09x", new Object[] { Integer.valueOf(268435457) }));
    
    System.out.println(str2HexStr("123"));
    
    byte[] bs4 = hexToBytes("00000002f101");
    System.out.println(new String(bs4, "UTF-8"));
    
    System.out.println(str2HexStr("."));
    System.out.println(new String(hexToBytes("562e312e31")));
    System.out.println(str2HexStr("V.1.1"));
    
    String testpc = "100100010002363410010002000630313830303110010003000000401001000400000000100100050000000410010006000831303030303030331002000100083130303030303033100200020005562e312e31100300020005562e312e311004000200000000100400030000000020020005012004000100000013200400010000000020040001000027012007000D000000002007000D00000000";
    byte[] tps = hexToBytes("100100010002363410010002000630313830303110010003000000401001000400000000100100050000000410010006000831303030303030331002000100083130303030303033100200020005562e312e31100300020005562e312e311004000200000000100400030000000020020005012004000100000013200400010000000020040001000027012007000D000000002007000D00000000");
    System.out.println(tps);
    
    byte[] sbs = "泉州佰源机械股份有限公司".getBytes("gbk");
    System.out.println(
      bytesToHex(sbs));
  }
}
