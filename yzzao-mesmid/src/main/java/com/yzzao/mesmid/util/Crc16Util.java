package com.yzzao.mesmid.util;

/**
 * CRC-16/MODBUS 校验位计算
 * @author Administrator
 * @see https://blog.csdn.net/wanyongtai/article/details/79472882
 * @see http://www.ip33.com/crc.html
 */
public class Crc16Util {
    /**crc hexString
     * @param arr_buff 不可以用byte[] arr_buff
     * @return
     */
    public static short getCrc16(short[] arr_buff) {
        int len = arr_buff.length;
        
        //预置 1 个 16 位的寄存器为十六进制FFFF, 称此寄存器为 CRC寄存器。
        int crc = 0xFFFF;
        int i, j;
        for (i = 0; i < len; i++) {
            //把第一个 8 位二进制数据 与 16 位的 CRC寄存器的低 8 位相异或, 把结果放于 CRC寄存器
            crc = ((crc & 0xFF00) | (crc & 0x00FF) ^ (arr_buff[i] & 0xFF));
            for (j = 0; j < 8; j++) {
                //把 CRC 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位
                if ((crc & 0x0001) > 0) {
                    //如果移出位为 1, CRC寄存器与多项式A001进行异或
                    crc = crc >> 1;
                    crc = crc ^ 0xA001;
                } else
                    //如果移出位为 0,再次右移一位
                    crc = crc >> 1;
            }
        }
        return (short)crc;
    }

    public static void main(String[] args) {
        short[] data = {0x00,0x02, 0x13,  0x0a, 0x05, 0xdc};//0002130a05dc
        System.out.println(Integer.toHexString(getCrc16(data)));
        System.out.println(String.format("%04x", getCrc16(data)));
        data = new short[] {0x00, 0x00, 0x13, 0x0a};//0000130a
        System.out.println(Integer.toHexString(getCrc16(data)));
        
        
    }
}
