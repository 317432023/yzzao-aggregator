package com.yzzao.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.yzzao.client.spec.client.OriknitClient;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.HttpUtil;
import com.yzzao.common.utils.StringUtil;

public class SenderTask implements Runnable {
	
	private final static Logger logger = Logger.getLogger(SenderTask.class);
	
	// 阻塞队列，用于存放数据包转成的Json对象
	private BlockingQueue<JSONObject> blockingQueue;
	public SenderTask(BlockingQueue<JSONObject> blockingQueue) {
		this.blockingQueue = blockingQueue;
	}
	public SenderTask(BlockingQueue<JSONObject> blockingQueue, WSClient wsClient, OriknitClient specClient) {
		this.wsClient = wsClient;
		this.blockingQueue = blockingQueue;
		this.specClient = specClient;
	}

	private volatile WSClient wsClient;
	
	private volatile OriknitClient specClient;
	
	private ExecutorService executor = Executors.newCachedThreadPool();
	
	@Override
	public void run() {
		
		for(;;) {
		    try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
		    
			final JSONObject jsonObj = blockingQueue.poll();

			// add for 决定是否发送 by kangtengjiao 20180203
			if(!Constants.IS_SEND) continue; 
			
			final String jsonStr = jsonObj != null ? jsonObj.toString() : null;
			
			if(StringUtils.isNotBlank(jsonStr)) {
			    // add by kangtengjiao 20181009 上报OPC数据
                executor.execute(new Runnable() {

                    @Override
                    public void run() {

                        final Message message = buildDataUpReq(jsonObj, new ConcurrentHashMap<Integer, Map<String,String>>());
                        try {
                            boolean re = specClient.sendMessage(message);
                            if(!re) logger.warn("tcpip send fail =>" + StringUtil.bytesToHex(Message.composeFull(message)));
                        }catch(Exception e) {

                            logger.error("tcpip send error =>" + e.getMessage() + " "+StringUtil.bytesToHex(Message.composeFull(message)));
                        }
                    }
                });
			    
//              // 方式一：用　client 来控制使用 WebSocket　还是使用 Http-Post
//              synchronized(this) {//需对本对象加锁，setClient同步化，防止外部引用该对象设置client时造成client判断不准确
//                  if(client != null) {
//                      
//                      if(client.isOpen()) {
//                          
//                          client.send(jsonStr);
//                      
//                      }else {
//                          logger.error("连接已经关闭，退出本次发送线程");
//                          break;
//                      }
//                      
//                  }else {
//                      
//                      String ret = HttpUtil.post(Constants.REQ_URL, jsonStr);
//                      
//                      if(ret == null) {
//                          logger.error("fail");
//                      }
//                      
//                  }
//              }
                
                // 方式二：用Constants启动参数控制使用 WebSocket　还是使用 Http-Post
                Map<String, String> map = new LinkedHashMap<>();
                map.put("APPID", Constants.APPID+"");
                map.put("APPSecret", Constants.APPSecret);
                map.put("data", jsonStr);
                
                switch(Constants.WorkMode) {
                case "A":case "a":
                    
                    String ret = HttpUtil.post(Constants.REQ_URL, map);
                        
                    if(ret == null) {
                        logger.error("fail:"+jsonStr);
                    }
                    
                    break;
                case "B":case "b":
                    
                    if(wsClient != null) {
                        
                        if(wsClient.isOpen()) {
                            
                            wsClient.send(jsonStr);
                        
                        }else {
                            logger.error("连接已经关闭，退出本次发送线程");
                            break;
                        }
                        
                    }
                    
                    break;
                }
                
			    
			}//end if
		
		}
		
	}

	public synchronized void setClient(WSClient client) {this.wsClient = client;}
	
	/**
     * 00015b936bcd 10000001 018001 000000000000001001 10000001 000000 000000000000000001<br>
     * 00015b936bcd 10000001 018001 000000000000001001 10000001 010101 100000011000000101 00000002 1f 01
     * @return
     */
    public static Message buildDataUpReq(JSONObject jsonObj, Map<Integer, Map<String, String>> realMap) {
        int machineID = jsonObj.getInt("machineID");
        int gauge = OriknitUtils.machineID2DevSeq(machineID);
        
        // 若不存在，放入
        if(realMap.get(gauge) == null) realMap.put(gauge, new ConcurrentHashMap<String,String>());
        Map<String, String> map = realMap.get(gauge);
        
        Message message = new Message();
        Header header = new Header();
        header.setComSeqNo(OriknitUtils.comSeqNo());
        header.setTs(OriknitUtils.curHexTs());
        Guid reqGuid = new Guid();
        Guid respGuid = new Guid();
        reqGuid.setMerNo(OriknitUtils.merNoOfReq());
        reqGuid.setDevTyNo(OriknitUtils.devTyNoReq());
        reqGuid.setDevSeqNo(OriknitUtils.devSeqNoReq(gauge));
        respGuid.setMerNo(OriknitUtils.merNoOfResp());
        respGuid.setDevTyNo(OriknitUtils.devTyNoResp());
        respGuid.setDevSeqNo(OriknitUtils.devSeqNoResp());
        
        header.setReqGuid(reqGuid);
        header.setRespGuid(respGuid);
        //header.setLength(message.getParCd() != null ? 2+message.getParCd().length()/2 : 2); //长度编码时确定
        message.setHeader(header);
        message.setFunTpNo((byte)0xf2);
        message.setSubFunTpNo((byte)0x01);
        
        StringBuffer sbf = new StringBuffer();
        //begin 0x1001（机械信息）
        //设备编号10010001
//        String machine_id = OriknitUtils.str2lenHexstr(""+jsonObj.getInt("machineID"));
//        sbf.append("1001").append("0001").append(machine_id);
        //设备类型
        String machine_type = OriknitUtils.str2lenHexstr(OriknitUtils.devTyNoReq(), null);
        sbf.append("1001").append("0002").append( machine_type );map.put("10010002", machine_type);
        
        //机号10010003
        //int gauge = jsonObj.getInt("machineID");
        sbf.append("1001").append("0003").append(StringUtil.bytesToHex( StringUtil.intToBytes2(gauge) ) );map.put("10010003", StringUtil.bytesToHex( StringUtil.intToBytes2(gauge) ) );
        //总针数
        int needle_count = 0;
        needle_count+=jsonObj.getInt("needleNum1");
        needle_count+=jsonObj.getInt("needleNum2");
        needle_count+=jsonObj.getInt("needleNum3");
        needle_count+=jsonObj.getInt("needleNum4");
        sbf.append("1001").append("0004").append(StringUtil.bytesToHex( StringUtil.intToBytes2(needle_count) ) );map.put("10010004", StringUtil.bytesToHex( StringUtil.intToBytes2(needle_count) ));
        //路数
        int feeder_number = 0;
        int _needsta = jsonObj.getInt("needleState");
        int _needsta1 = _needsta & 1;
        int _needsta2 = _needsta & (1<<1);
        int _needsta3 = _needsta & (1<<2);
        int _needsta4 = _needsta & (1<<3);
        feeder_number = 4 - (_needsta1+_needsta2+_needsta3+_needsta4);
        sbf.append("1001").append("0005").append(StringUtil.bytesToHex( StringUtil.intToBytes2(feeder_number) ) );map.put("10010005", StringUtil.bytesToHex( StringUtil.intToBytes2(feeder_number) ) );
        //机械厂家
        String machine_company = OriknitUtils.str2lenHexstr(OriknitUtils.merNoOfReq(), null);
        sbf.append("1001").append("0006").append(machine_company);map.put("10010006", machine_company);
        //end 0x1001（机械信息）
        //begin 0x1002（主控信息）
        //控制系统厂家
        String control_sys_company = OriknitUtils.str2lenHexstr(OriknitUtils.merTitleOfReq(), "utf8");
        sbf.append("1002").append("0001").append(control_sys_company);map.put("10020001", control_sys_company);
        //主控系统软件版本号
        String msys_verion = OriknitUtils.str2lenHexstr(Constants.Ver, null);
        sbf.append("1002").append("0002").append(msys_verion);map.put("10020002", msys_verion);
        //end 0x1002（主控信息）
        //begin 0x1003（人机信息）
        //人机软件版本号
        String ui_sys_ver = OriknitUtils.str2lenHexstr(Constants.Ver, null);
        sbf.append("1003").append("0002").append(ui_sys_ver);map.put("10030002", ui_sys_ver);
        //end 0x1003（人机信息）
        //begin 0x1004（工厂信息
//        //车间
//        int workshop_id = jsonObj.getInt("workShopID");
//        sbf.append("1004").append("0002").append(StringUtil.bytesToHex( StringUtil.intToBytes2(workshop_id) ) );map.put("10040002", StringUtil.bytesToHex( StringUtil.intToBytes2(workshop_id) ) );
        //机组
        int unit_id = jsonObj.getInt("TeamID");
        sbf.append("1004").append("0003").append(StringUtil.bytesToHex( StringUtil.intToBytes2(unit_id) ) );map.put("10040003", StringUtil.bytesToHex( StringUtil.intToBytes2(unit_id) ) );
        //end 0x1004（工厂信息
        //begin 0x2002（运行参数）
        //喷油状态    
        byte injection_state = (byte)(jsonObj.getInt("OilState")+1);
        sbf.append("2002").append("0005").append(StringUtil.bytesToHex( new byte[]{injection_state} ) );map.put("20020005", StringUtil.bytesToHex( new byte[]{injection_state} ));
        if(injection_state>1) {
            //喷油参数1
            int injection_time = injection_state==2?jsonObj.getInt("OilParam11"):jsonObj.getInt("OilParam21");
            sbf.append("2002").append("0006").append(StringUtil.bytesToHex( StringUtil.intToBytes2(injection_time) ) );map.put("20020006", StringUtil.bytesToHex( StringUtil.intToBytes2(injection_time) ) );
            //喷油参数2
            int injection_interval = injection_state==2?jsonObj.getInt("OilParam12"):jsonObj.getInt("OilParam22");
            sbf.append("2002").append("0007").append(StringUtil.bytesToHex( StringUtil.intToBytes2(injection_interval) ) );map.put("20020007", StringUtil.bytesToHex( StringUtil.intToBytes2(injection_interval) ) );
        }
        //end 0x2002（运行参数）
        //begin 0x2004（编织信息
        //当前速度
        int cur_speed = jsonObj.getInt("speed");
        sbf.append("2004").append("0001").append(StringUtil.bytesToHex( StringUtil.intToBytes2(cur_speed) ) );map.put("20040001", StringUtil.bytesToHex( StringUtil.intToBytes2(cur_speed) ));
        int sum_output = jsonObj.getInt("workNum0");
        sbf.append("2004").append("0002").append(StringUtil.bytesToHex( StringUtil.intToBytes2(sum_output) ) );map.put("20040002", StringUtil.bytesToHex( StringUtil.intToBytes2(sum_output) ));
        //end 0x2004（编织信息
        //begin 0x2007（班次信息）
        //当前班次工号
        int user_id = jsonObj.getInt("TeamNo");
        sbf.append("2007").append("0002").append(StringUtil.bytesToHex( StringUtil.intToBytes2(user_id) ) );map.put("20070002", StringUtil.bytesToHex( StringUtil.intToBytes2(user_id) ) );
        //当前班次产量
        int shift_amount_yield = jsonObj.getInt("turnA");
        sbf.append("2007").append("0003").append(StringUtil.bytesToHex( StringUtil.intToBytes2(shift_amount_yield) ) );map.put("20070003", StringUtil.bytesToHex( StringUtil.intToBytes2(shift_amount_yield) ));
        //菜单设定刷卡序列号
        int menu_no = jsonObj.getInt("menuNo");
        sbf.append("2007").append("000d").append(StringUtil.bytesToHex( StringUtil.intToBytes2(menu_no) ) );map.put("2007000d", StringUtil.bytesToHex( StringUtil.intToBytes2(menu_no) ) );
        //机修刷卡序列号
        int repair_sn = jsonObj.getInt("repair_sn");
        sbf.append("2007").append("000e").append(StringUtil.bytesToHex( StringUtil.intToBytes2(repair_sn) ) );map.put("2007000e", StringUtil.bytesToHex( StringUtil.intToBytes2(repair_sn) ) );
        //机修刷卡时间 TODO 无法匹配
        //int repair_card_time
        //end 0x2007（班次信息）
        //begin 0x2008（花型信息
        //订单编号 TODO 类型无法匹配
        //int order_id = jsonObj.getInt("c_interBillNo");
        //end 0x2008（花型信息
        
        //begin 0x2100（传感器报警）
        
        // 布破
        int first_status = jsonObj.getInt("first_status"); 
        int cloth_broken_alarm = (first_status == 11)?1:0;
        // 断纱
        String yarn_broken_alarm = (first_status == 13||first_status == 14)?"1":"0";
        
        JSONArray array = jsonObj.getJSONArray("status_list");
        if(array!=null && !array.isEmpty()) {
            Iterator it = array.iterator();
            JSONObject obj = null;
            int code = 0;
            while(it.hasNext()) {
                obj = (JSONObject) it.next();
                code = obj.getInt("code");
                if(code == 11) cloth_broken_alarm= 1;
                else if(code==13||code==14) yarn_broken_alarm="1";
            }
        }

        //20181116新增设备状态
        String devState = "05";
        if(//cloth_broken_alarm==1||yarn_broken_alarm=="1"||
                //first_status==0||//0 停止
                first_status==2//2：测针故障
                ||first_status==3//3：测纱故障
                ||first_status==6//6：机器超速
                ||first_status==7//7：机门故障
                ||first_status==8//7：变频故障
                ||first_status==9//9：缺气
                ||first_status==10//10：缺油
                ||first_status==11//11：布破
                ||first_status==13//13：中段断纱
                ||first_status==14//14：上段断纱
                ||first_status==15//15：5欠压故障
                //||first_status==16// 16：12V欠压
                ||first_status==17) {//17：模块故障通信
            message.setAlarm(true);
            devState = "04";
            if(/*cloth_broken_alarm==1||*/first_status==11) {
                //sbf.append("2100").append("0006").append("0001");
                map.put("21000006", "0001");
                map.put("111alarm","21000006"+"0001");
            }else if(/*"1".equals(yarn_broken_alarm)||*/first_status==13||first_status==14) {
                //sbf.append("2100").append("0007").append(OriknitUtils.str2lenHexstr(yarn_broken_alarm, null) );
                map.put("21000007", OriknitUtils.str2lenHexstr(yarn_broken_alarm, null));
                map.put("111alarm","21000007"+OriknitUtils.str2lenHexstr(yarn_broken_alarm, null));
            }
            else if(first_status==2) {
                //sbf.append("2100").append("000a").append("0001");
                map.put("2100000a", "0001");
                map.put("111alarm","2100000a"+"0001");
            }
            else if(first_status==6) {
                //sbf.append("2100").append("000b").append("0001");
                map.put("2100000b", "0001");
                map.put("111alarm","2100000b"+"0001");
            }
            else if(first_status==7) {
                //sbf.append("2100").append("0004").append("0001" );
                map.put("21000004", "0001");
                map.put("111alarm","21000004"+"0001");
            }
            else if(first_status==8) {
                //sbf.append("2100").append("000c").append("0001" );
                map.put("2100000c", "0001");
                map.put("111alarm","2100000c"+"0001");
            }
            else if(first_status==9) {
                //sbf.append("2100").append("0001").append("0001" );
                map.put("21000001", "0001");
                map.put("111alarm","21000001"+"0001");
            }
            else if(first_status==10) {
                //sbf.append("2100").append("0003").append("0001" );
                map.put("21000003", "0001");
                map.put("111alarm","21000003"+"0001");
            }
            else if(first_status==15) {
                //sbf.append("2100").append("0002").append("0001" );
                map.put("21000002", "0001");
                map.put("111alarm","21000002"+"0001");
            }
            else if(first_status==17) {
                //sbf.append("2101").append("0004").append("0001" );
                map.put("21010004", "0001");
                map.put("111alarm","21010004"+"0001");
            }
        }else if(first_status==0) {
            devState = "03";//停车
        }
        
        int needleState = jsonObj.getInt("needleState");
        needleState= needleState & 0b00111111;
        if(needleState>0) {
            if(!devState.equals("03")) { 
                // 03 停车的优先级大于报警 20190107
                devState = "04"; 
            }
            //sbf.append("2100").append("0008").append("0001" );
            map.put("21000008", "0001");
            map.put("111alarm","21000008"+"0001");
        }
        
        sbf.append("2001").append("0002").append(devState);
        map.put("20010002", devState);
        //end 0x2100（传感器报警）
        
        // 新增扩展参数（模拟值，见simulation.json）20181115
        sbf.append(Constants.ExtParms);
        
        message.setParCd(sbf.toString().toLowerCase());
        
        return message;
    }
    
    public static void main(String[] args) {
        
        Map<String, Map<String, String>> a = new HashMap<>();
        a.put("a", new HashMap<String, String>());
        Map aa = a.get("a");
        aa.put("aa", "aa");
        aa = a.get("a");
        System.out.println(aa.get("aa"));
        
        System.out.println(StringUtil.bytesToHex( new byte[]{0,(byte)1} ));
        System.out.println(StringUtil.bytesToHex( Arrays.copyOfRange(StringUtil.intToBytes2(Integer.parseInt("1")), 2, 4)  ));
    }
}
