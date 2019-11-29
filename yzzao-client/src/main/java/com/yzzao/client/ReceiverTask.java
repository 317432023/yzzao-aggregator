package com.yzzao.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.StringUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ReceiverTask implements Runnable {
	
	private final static Logger logger = Logger.getLogger(ReceiverTask.class);
	
	private BlockingQueue<JSONObject> blockingQueue;
	
	public ReceiverTask(BlockingQueue<JSONObject> blockingQueue) {
		this.blockingQueue = blockingQueue;
	}
	
	private static final short PACK_MIN = 106;
	
	private void recv() {
		
		DatagramSocket socket = null;
		
		try {
			
			socket = new DatagramSocket(Constants.UDPPort);
			
			byte[] buf = new byte[2048];
			
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			socket.receive(packet);
			
			byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
			

            logger.info("byte length -> " + data.length + ", socket addr -> " + packet.getSocketAddress());
			/*if(logger.isDebugEnabled()) {
				
				logger.debug("byte length -> " + data.length + ", socket addr -> " + packet.getSocketAddress());
			}*/
			
			
			// 拆包
			List<byte[]> packlist = new ArrayList<>(); // 一个字节数组代表一个包
				
			byte[] pack = null; // 拆分后的单独包
			
			int pindex = 0; // 拆分后的单独包位于接收包中的下标
			
			int plen = -1; // 拆分后的每个单独包的长度(不含定义长度的前两个字节) 
			
			while( ( data.length - pindex ) >= PACK_MIN ) { // 后面3个字节是固定长度字节
				
				plen = ( data[ pindex ] & 0xff ) * 256 + ( data[ pindex + 1 ] & 0xff ) + 3; // 每个单独包前2个字节定义了包长度，后面3个字节是固定长度字节

				if( plen < PACK_MIN ) break;
				
				if( ( data.length - pindex ) < plen ) {
					logger.error("解包失败，下标 -> " + pindex + " ，字节数组 -> " + StringUtil.bytesToHex(data));
					break;
				}
				
				pack = new byte[ plen ]; 
				
				System.arraycopy( data, pindex, pack, 0, plen );
				
				packlist.add( pack );
				
				pindex += plen; // 计算下一个单独包位于接收包中的下标
				
			}
			
			// 打印拆包结果
			switch(packlist.size()) {
			case 0:
				//logger.error("无效包， 字节数 -> " + data.length + "，字节数组 -> " + StringUtil.bytesToHex(data));
				return;
			case 1:
				//logger.debug("不需要拆包");
				break;
			default:
				//logger.info("拆出 " + packlist.size() + " 个包");
				break;
			}
			

			/*if(packlist.size() > 0  && data.length - pindex != 0) {
				logger.warn("第" + (packlist.size() + 1) + "个粘包拆包失败，位置 -> " + (pindex*2) + "，字节数组-> " + StringUtil.bytesToHex(data));
			}*/
			
			
			// 拆包结果转为JSON放到阻塞队列
			for( byte[] decode: packlist ) { 
				
				JSONObject jsonObj = convertBytes2JSONObject(decode);
				
				// 调试
				/*if((Integer)jsonObj.get("machineID") == 24) {
					if(logger.isInfoEnabled()) {
						logger.info(StringUtil.bytesToHex(data));
					}
				}*/
				
				// add for 决定是否发送，先放进阻塞队列 by kangtengjiao 20180203
				// 数据放进一个阻塞队列
				blockingQueue.put(jsonObj);
				
				
				// 新增计数器 20180131
				//CountHolder.plus( (Integer)jsonObj.get("machineID"));
				
			}
			
		} catch (SocketException e) {
			//ignore
		} catch (IOException e) {
			//ignore
		} catch (InterruptedException e) {
			//ignore
		} finally {
			if(socket!=null)
				socket.close();
		}
		
	}
	
	private static int c_int(byte b) {
		return b & 0xFF;
	}
	
	private JSONObject convertBytes2JSONObject(byte[] fd) {
		
		if(fd.length < PACK_MIN) {
			logger.error("采集到的字节码长度不足.");
			return null;
		}
		
		JSONObject obj = new JSONObject();
    	obj.element("APPID", Constants.APPID);
        obj.element("APPSecret", Constants.APPSecret);
        
        obj.element("CreateTime", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date()));
        
        obj.element("workShopID",c_int(fd[2]));//车间号
        obj.element("machineID",c_int(fd[3]));//地址号
        obj.element("TeamID",c_int(fd[4]));//班别
        
        obj.element("turnA",(c_int(fd[5])*256+c_int(fd[6]))*10000+(c_int(fd[7])*256+c_int(fd[8])));      //A班计数
        obj.element("turnB",(c_int(fd[9])*256+c_int(fd[10]))*10000+(c_int(fd[11])*256+c_int(fd[12])));    //B班计数
        obj.element("turnC",(c_int(fd[13])*256+c_int(fd[14]))*10000+(c_int(fd[15])*256+c_int(fd[16])));  //C班计数
        obj.element("turnSUM",(c_int(fd[17])*256+c_int(fd[18]))*10000+(c_int(fd[19])*256+c_int(fd[20])));//总转数
        
        obj.element("OilState",c_int(fd[21]));   //喷油
        obj.element("OilParam11",c_int(fd[22])); //秒间歇喷参数
        obj.element("OilParam12",c_int(fd[23])); //秒间歇停止参数
        obj.element("OilParam21",c_int(fd[24])); //圈间歇喷参数
        obj.element("OilParam22",c_int(fd[25])); //圈间歇停止参数
        		
        obj.element("driver0",c_int(fd[26])); //断针驱动
        obj.element("driver1",c_int(fd[27])); //缺油驱动
        obj.element("driver2",c_int(fd[28])); //剖布驱动

        obj.element("workSUM0",c_int(fd[29])*256+c_int(fd[30])); //织布数
        obj.element("workSUM1",c_int(fd[31])); //清车数
                
        obj.element("workMode",c_int(fd[32]));  //工作模式
        obj.element("workNum0",c_int(fd[33])*256+c_int(fd[34])); //织布数
        obj.element("workNum1",c_int(fd[35]));  //当前清车数

        obj.element("speed", (c_int(fd[36])*256+c_int(fd[37]))/10.0 ); //速度

        obj.element("yarn1",(c_int(fd[38])*256+c_int(fd[39]))); //第一路纱长
        obj.element("yarn2",(c_int(fd[40])*256+c_int(fd[41]))); //第二路纱长
        obj.element("yarn3",(c_int(fd[42])*256+c_int(fd[43]))); //第三路纱长
        obj.element("yarn4",(c_int(fd[44])*256+c_int(fd[45]))); //第四路纱长
        obj.element("yarn5",(c_int(fd[46])*256+c_int(fd[47]))); //第五路纱长
        obj.element("yarn6",(c_int(fd[48])*256+c_int(fd[49]))); //第六路纱长
        
        obj.element("TeamNo",String.format("%02d%02d%02d%02d", c_int(fd[50]),c_int(fd[51]),c_int(fd[52]),c_int(fd[53]))); //班别刷卡序列号
        obj.element("menuNo",String.format("%02d%02d%02d%02d", c_int(fd[54]),c_int(fd[55]),c_int(fd[56]),c_int(fd[57]))); //菜单设定刷卡序列号
        obj.element("repair_sn",String.format("%02d%02d%02d%02d", c_int(fd[58]),c_int(fd[59]),c_int(fd[60]),c_int(fd[61]))); //机修刷卡序列号
        
        obj.element("temperature",c_int(fd[62])); //温度数据
        obj.element("humidity",c_int(fd[63])); //湿度数据

        obj.element("needleState",c_int(fd[64]));  //针路状态

        obj.element("needleNum1",c_int(fd[65])*256+c_int(fd[66])); //第一针路数 
        obj.element("needleNum2",c_int(fd[67])*256+c_int(fd[68])); //第二针路数
        obj.element("needleNum3",c_int(fd[69])*256+c_int(fd[70])); //第三针路数
        obj.element("needleNum4",c_int(fd[71])*256+c_int(fd[72])); //第四针路数

        obj.element("materialOpt",c_int(fd[73]));                //材料选择

        obj.element("c_interBillNo",generateC_interBillNo(Arrays.copyOfRange(fd, 74, 90)));
        obj.element("qualityNum1",c_int(fd[90])*256+c_int(fd[91])); //坏针、花针、油针数量
        obj.element("qualityNum2",c_int(fd[92])*256+c_int(fd[93])); //破洞数量
        obj.element("qualityNum3",c_int(fd[94])*256+c_int(fd[95])); //脏纱数量
        obj.element("qualityNum4",c_int(fd[96])*256+c_int(fd[97])); //油污数量

        //obj.element("call", c_int(fd[98]));//呼叫
        obj.element("call", parseCall(c_int(fd[98])));
        
        obj.element("repair_card_time",c_int(fd[99]));  //机修刷卡时间
        obj.element("patrol",c_int(fd[100]));  //呼巡逻标志

        obj.element("first_status",c_int(fd[105]));
        
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
	
	@Override
	public void run() {
		for(;;) {
			recv();
		}
	}

	public static void main(String[] args) {
		
		int i = 14;
		JSONArray co = parseCall(i);
		
		System.out.println(co);
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.element("call", parseCall(14));
		
		System.out.println( jsonObject.toString() );
		
		jsonObject = JSONObject.fromObject( jsonObject.toString() );
		
		JSONArray oa = (JSONArray) jsonObject.get("call");
		
		int a = oa.getInt(3);
		
		System.out.println(a);
		
		System.out.println(1/10.0);
	}
}
