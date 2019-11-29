package com.yzzao.mesmid.v2;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.yzzao.common.utils.JsonUtils;
import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.json.pojo.MESOrder;
import com.yzzao.mesmid.json.pojo.MESOrderDetail;
import com.yzzao.mesmid.v2.struct.Message;

public class Mesmid2ndUtils {
    
    private static final Logger logger = Logger.getLogger(Mesmid2ndUtils.class);
    
    private static final int PACK_MIN = 146;
    
    private static final int parOffset = 5;
    
    private static int c_int(byte b) {
        return b & 0xFF;
    }
    
    public static JSONObject convertBytes2JSONObject(byte[] fd) {
        
        if(fd.length < PACK_MIN) {
            logger.error("采集到的字节码长度不足.");
            return null;
        }
        
        JSONObject obj = new JSONObject();
        //obj.element("APPID", Constants.APPID);
        //obj.element("APPSecret", Constants.APPSecret);
        
        obj.element("CreateTime", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date()));
        
        obj.element("workShopID",c_int(fd[6-parOffset]));//车间号unsigned char
        obj.element("machineID", (c_int(fd[7-parOffset])<<8 ) + c_int(fd[8-parOffset]) );//设备IDuint16
        obj.element("machineState",c_int(fd[9-parOffset]));//设备状态unsigned char
        obj.element("clothLamp", c_int(fd[17-parOffset]));//照布灯unsigned char
        
        obj.element("lightLamp1",c_int(fd[18-parOffset])); //照明灯1unsigned char
        obj.element("lightLamp2",c_int(fd[19-parOffset])); //照明灯2unsigned char
        obj.element("fanSwitch",c_int(fd[20-parOffset])); //风扇开关unsigned char
        obj.element("freSwitch",c_int(fd[21-parOffset])); //变频开关unsigned char
        obj.element("forceMode",c_int(fd[22-parOffset])); //强迫模式unsigned char
        obj.element("workMode",c_int(fd[23-parOffset])); //当前织布模式unsigned char
        obj.element("workNum0",(c_int(fd[24-parOffset])<<8) + c_int(fd[25-parOffset])); //当前织布数uint16
        obj.element("workNum1",c_int(fd[26-parOffset])); //当前清车数unsigned char
        obj.element("turnSUM",(c_int(fd[27-parOffset])<<24) + (c_int(fd[28-parOffset])<<16)+ (c_int(fd[29-parOffset])<<8)+ c_int(fd[30-parOffset])); //总转数uint32
        obj.element("speed",(c_int(fd[31-parOffset])<<8)+ c_int(fd[32-parOffset])); //当前速度uint16
        obj.element("TeamID",c_int(fd[33-parOffset])); //当前班次unsigned char
        obj.element("TeamNo", String.format("%02d%02d%02d%02d",c_int(fd[34-parOffset]),c_int(fd[35-parOffset]),c_int(fd[36-parOffset]),c_int(fd[37-parOffset]))); //操作工号（挡车工号）4个字节
        obj.element("turn",(c_int(fd[38-parOffset])<<24) + (c_int(fd[39-parOffset])<<16)+ (c_int(fd[40-parOffset])<<8)+ c_int(fd[41-parOffset])); //当班圈数uint32
        obj.element("turnA",(c_int(fd[42-parOffset])<<24) + (c_int(fd[43-parOffset])<<16)+ (c_int(fd[44-parOffset])<<8)+ c_int(fd[45-parOffset])); //A班计数uint32
        obj.element("turnB",(c_int(fd[46-parOffset])<<24) + (c_int(fd[47-parOffset])<<16)+ (c_int(fd[48-parOffset])<<8)+ c_int(fd[49-parOffset])); //B班计数uint32
        obj.element("turnC",(c_int(fd[50-parOffset])<<24) + (c_int(fd[51-parOffset])<<16)+ (c_int(fd[52-parOffset])<<8)+ c_int(fd[53-parOffset])); //C班计数uint32
        /*obj.element("turn",(c_int(fd[38-parOffset])*256+c_int(fd[39-parOffset]))*10000+(c_int(fd[40-parOffset])*256+c_int(fd[41-parOffset])));
        obj.element("turnA",(c_int(fd[42-parOffset])*256+c_int(fd[43-parOffset]))*10000+(c_int(fd[44-parOffset])*256+c_int(fd[45-parOffset])));
        obj.element("turnB",(c_int(fd[46-parOffset])*256+c_int(fd[47-parOffset]))*10000+(c_int(fd[48-parOffset])*256+c_int(fd[49-parOffset])));
        obj.element("turnC",(c_int(fd[50-parOffset])*256+c_int(fd[51-parOffset]))*10000+(c_int(fd[52-parOffset])*256+c_int(fd[53-parOffset])));*/
        
        obj.element("manageNo",String.format("%02d%02d%02d%02d",c_int(fd[54-parOffset]),c_int(fd[55-parOffset]),c_int(fd[56-parOffset]),c_int(fd[57-parOffset]))); //管理卡号4个字节
        obj.element("mechanicNo",String.format("%02d%02d%02d%02d",c_int(fd[58-parOffset]),c_int(fd[59-parOffset]),c_int(fd[60-parOffset]),c_int(fd[61-parOffset]))); //机修卡号4个字节
        obj.element("patrolNo",String.format("%02d%02d%02d%02d",c_int(fd[62-parOffset]),c_int(fd[63-parOffset]),c_int(fd[64-parOffset]),c_int(fd[65-parOffset]))); //巡逻卡号4个字节
        obj.element("feedNo",String.format("%02d%02d%02d%02d",c_int(fd[66-parOffset]),c_int(fd[67-parOffset]),c_int(fd[68-parOffset]),c_int(fd[69-parOffset]))); //送料卡号4个字节
        obj.element("call",/*c_int(fd[70-parOffset])*/parseCall(fd[70-parOffset])); //呼叫unsigned char
        obj.element("OilState",c_int(fd[71-parOffset])); //喷油模式unsigned char
        obj.element("workSUM0",(c_int(fd[72-parOffset])<<8)+ c_int(fd[73-parOffset])); //织布设定值uint16
        obj.element("workSUM1",c_int(fd[74-parOffset])); //清车设定值unsigned char
        obj.element("c_interBillNo",generateC_interBillNo(Arrays.copyOfRange(fd, 75-parOffset, 90-parOffset+1))); //当前订单号16个字节
        obj.element("workTime", (c_int(fd[91-parOffset])<<8)+ c_int(fd[92-parOffset])); //联网周期uint16
        obj.element("yarnState",c_int(fd[93-parOffset])); //纱路状态unsigned char
        obj.element("yarn1",(c_int(fd[94-parOffset])<<8)+ c_int(fd[95-parOffset])); //第一路纱长uint16
        obj.element("yarn2",(c_int(fd[96-parOffset])<<8)+ c_int(fd[97-parOffset])); //第二路纱长uint16
        obj.element("yarn3",(c_int(fd[98-parOffset])<<8)+ c_int(fd[99-parOffset])); //第三路纱长uint16
        obj.element("yarn4",(c_int(fd[100-parOffset])<<8)+ c_int(fd[101-parOffset])); //第四路纱长uint16
        obj.element("yarn5",(c_int(fd[102-parOffset])<<8)+ c_int(fd[103-parOffset])); //第五路纱长uint16
        obj.element("yarn6",(c_int(fd[104-parOffset])<<8)+ c_int(fd[105-parOffset])); //第六路纱长uint16
        obj.element("needleState",c_int(fd[106-parOffset])); //针路状态unsigned char
        obj.element("needleNum1",(c_int(fd[107-parOffset])<<8)+ c_int(fd[108-parOffset])); //第一路针数uint16
        obj.element("needleNum2",(c_int(fd[109-parOffset])<<8)+ c_int(fd[110-parOffset])); //第二路针数uint16
        obj.element("needleNum3",(c_int(fd[111-parOffset])<<8)+ c_int(fd[112-parOffset])); //第三路针数uint16
        obj.element("needleNum4",(c_int(fd[113-parOffset])<<8)+ c_int(fd[114-parOffset])); //第四路针数uint16
        obj.element("qualityNum1",c_int(fd[115-parOffset])); //坏针数量unsigned char
        obj.element("qualityNum2",c_int(fd[116-parOffset])); //花针数量unsigned char
        obj.element("qualityNum3",c_int(fd[117-parOffset])); //油针数量unsigned char
        obj.element("qualityNum4",c_int(fd[118-parOffset])); //破洞数量unsigned char
        obj.element("qualityNum5",c_int(fd[119-parOffset])); //漏纱数量unsigned char
        obj.element("qualityNum6",c_int(fd[120-parOffset])); //脏纱数量unsigned char
        obj.element("temperature1",(c_int(fd[121-parOffset])<<8)+ c_int(fd[122-parOffset])); //第一路温度uint16
        obj.element("temperature2",(c_int(fd[123-parOffset])<<8)+ c_int(fd[124-parOffset])); //第二路温度uint16
        obj.element("temperature3",(c_int(fd[125-parOffset])<<8)+ c_int(fd[126-parOffset])); //第三路温度uint16
        obj.element("temperature4",(c_int(fd[127-parOffset])<<8)+ c_int(fd[128-parOffset])); //第四路温度uint16
        obj.element("temperature5",(c_int(fd[129-parOffset])<<8)+ c_int(fd[130-parOffset])); //第五路温度uint16
        obj.element("humidity1",(c_int(fd[131-parOffset])<<8)+ c_int(fd[132-parOffset])); //第一路湿度uint16
        obj.element("humidity2",(c_int(fd[133-parOffset])<<8)+ c_int(fd[134-parOffset])); //第二路湿度uint16
        obj.element("machineNum",(c_int(fd[135-parOffset])<<8)+ c_int(fd[136-parOffset])); //机器针数uint16
        obj.element("first_status",c_int(fd[150-parOffset]));//当前优先级最高的状态unsigned char
        
        // 状态列表
        
        if( (fd.length - PACK_MIN) % 3 != 0) {
            logger.warn("收到的状态列表字节长度不正确");
            logger.warn(StringUtil.bytesToHex(fd));
            return obj;
        }
        
        int size = ( fd.length - PACK_MIN ) / 3;
        
        JSONArray array = new JSONArray();
        
        for(int i=0; i<size; i++) {
            int pos = PACK_MIN + i*3;
            JSONObject status = new JSONObject();
            status.element("code", c_int(fd[pos]));
            status.element("start_time", c_int(fd[pos+1]));
            status.element("end_time", c_int(fd[pos+2]));
            
            array.element(status);
        }
        
        obj.element("status_list", array);
        
        return obj;
    }
    
    private static String generateC_interBillNo(byte... bs) {
        if(bs == null || bs.length == 0) return "";
        if(bs.length > 16) bs = Arrays.copyOf(bs, 16);
        String interBillNo = "";
        try {
            interBillNo = new String(bs,"ascii");
        } catch (UnsupportedEncodingException e) {
            //ignore
        }
        return interBillNo;
    }
    
    private static JSONArray parseCall(int call) {
        String cs = Integer.toBinaryString(call);
        String _cs = String.format("%4s", cs);// 取后4位，倒数第5位备用
        _cs = _cs.substring(_cs.length() - 4);
        _cs = _cs.replaceAll(" ", "0");
        
        char[] cc = _cs.toCharArray();
        
        return JSONArray.fromObject(cc);
    }
    
    /**
     * 转MES下单信息为 Message对象
     * @param jsonStr
     * @return
     * @throws UnsupportedEncodingException 
     */
    public static Map<String, List<Message>> jsonStr2Message(String jsonStr) throws UnsupportedEncodingException {
        Map<String , List<Message>> msglstMap = new LinkedHashMap<>();
        MESOrder order = JsonUtils.jsonToPojo(jsonStr, MESOrder.class);
        //0x20 0x01 订单号
        //0x12 0x05 工厂消息通知
        //0x20 0x05 订单进度
        MESOrderDetail[] dtls = order.getData();
        for(MESOrderDetail dtl: dtls){
            if(StringUtils.isNotBlank(dtl.getMachineid()) && msglstMap.get(dtl.getMachineid()) == null){
                msglstMap.put(dtl.getMachineid(), new ArrayList<Message>());
            }
            
            if(StringUtils.isNotBlank(dtl.getBillno()) && msglstMap.get(dtl.getMachineid()) != null) {
                // 不足16位补0
                if(dtl.getBillno().length()>16) {
                    dtl.setBillno(dtl.getBillno().substring(0,16));
                }else if(dtl.getBillno().length()<16) {
                    StringBuffer sbff = new StringBuffer();
                    for(int i=0; i<16-dtl.getBillno().length(); i++)//16-dtl.getBillno().length()
                        sbff.append("0");
                    dtl.setBillno(sbff.toString()+dtl.getBillno());
                }
                
                byte[] buf = dtl.getBillno().getBytes("ascii");
                Message msg = new Message((byte)0x20,(byte)0x01,buf);
                msglstMap.get(dtl.getMachineid()).add(msg);
            }
            if(StringUtils.isNotBlank(dtl.getNotice()) && msglstMap.get(dtl.getMachineid()) != null) {
                byte[] buf = dtl.getNotice().getBytes("gbk");
                Message msg = new Message((byte)0x12,(byte)0x05,buf);
                msglstMap.get(dtl.getMachineid()).add(msg);
            }
            if(dtl.getOrderschedule()!=null && msglstMap.get(dtl.getMachineid()) != null) {
                int sch = dtl.getOrderschedule();
                byte[] bs = StringUtil.intToBytes2(sch);
                Message msg = new Message((byte)0x20,(byte)0x05,new byte[]{bs[2],bs[3]});
                msglstMap.get(dtl.getMachineid()).add(msg);
            }
        }
        /*if(!Constants.APPID.equals(order.getAppid())) {
            logger.error("recv appid uncorrect: " + order.getAppid());
            return null;
        }
        if(!Constants.APPSecret.equals(order.getAppsecret())) {
            logger.error("recv password uncorrect: " + order.getAppsecret());
            return null;
        }*/
        /*
         * {"cd":"8000",
         *    "aapid":"12431234"
         *  }
         * */
        /*{
         * "cd":9006,
         * ...
         * []
         * }
         * 
         * {
         *  "cd":8001,
            "appid": "436",
            "appsecret": "123465",
            "data": [
                {
                    "billNo": "2018101214520021",
                    "notice": "通知上下班都要打卡",
                    "orderSchedule": 68,
                    "machineID": "1"
                },
                {
                    "billNo": "2018101214520021",
                    "notice": "通知上下班都要打卡",
                    "orderSchedule": 68,
                    "machineID": "1"
                },
                {
                    "billNo": "2018101214520021",
                    "notice": "通知上下班都要打卡",
                    "orderSchedule": 68,
                    "machineID": "1"
                }
            ]
        }*/
        
        return msglstMap;
    }
    
    public static String cardByte2CardNo(final byte[] cardByte) {
        String s1="";
        for(byte b:cardByte) {
            s1+=b&0xff;
        }
        return s1;
    }
    
    public static void main(String[] args) throws UnsupportedEncodingException {
        //System.out.println(String.format("%02d", 0xd2));
        final String hexs="0102000203120a06110e3408000000000000000000000000be61000000000000000000216300000000000000000000000000000000000000000000000000000000000007d00030303030303030303030303030303030000600000000000000000000000000000000000000000000000000000000000000000000000000000000000007d0000000000000000000000000000e0afcfc0cfcfc0dfcfc0efcfc";
        byte[] par = StringUtil.hexToBytes(hexs);
        JSONObject jobj = convertBytes2JSONObject(par);
        System.out.println(jobj.toString());
        
        final String jsonStr = "{\"appid\": \"436\",\"appsecret\": \"123465\",\"data\": [{\"billNo\": \"2018101214520021\",\"notice\": \"通知上下班都要打卡\",\"orderSchedule\": 68,\"machineID\": \"1\"}]}";
        Map<String, List<Message>> map = jsonStr2Message(jsonStr);
        System.out.println(map);
    }
}
