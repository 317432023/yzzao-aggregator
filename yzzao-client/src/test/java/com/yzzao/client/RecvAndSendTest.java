package com.yzzao.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.yzzao.common.utils.StringUtil;


/**
 * 本类测试udp发送数据包给·生产者-仓库-消费者·
 * @author Administrator
 *
 */
public class RecvAndSendTest {

	
	public static void main(String[] args) throws UnsupportedEncodingException {
		long st = System.currentTimeMillis();
		
		System.out.println("请输入目标IP地址：");
		
		Scanner scanner = new Scanner(System.in);
		String ips = scanner.nextLine(); 
		
		// 确定目标IP地址及端口号 
        int port = 4001;  
        InetAddress ip;
		try {
			ip = InetAddress.getByName(ips);
		} catch (UnknownHostException e1) {
			System.err.println("无效的IP地址:" + ips);
			//ignore
			return;
		}
		
		
		DatagramSocket sendSocket = null;
		try {
			for(int i=0; i<100000; i++) {//测试万条
		
				// 创建发送方的套接字，IP默认为本地，端口号随机  
		        sendSocket = new DatagramSocket();  
		        
		        // 确定要发送的消息：  
		        //String mes = "你AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+String.format("%6d", i);  
		        
		        // 由于数据报的数据是以字符数组传的形式存储的，所以传转数据  
		        //byte[] buf = mes.getBytes("GBK");  
		        
		        //String hexString = "0067004000000027010000000000000000000e0421000000000000000010cc00000a160000be0000000000000000000000000000000000000000000000000000000000000000000000003030303030303030303030303030303000000000000000000000000000000001006a000f0000000000000000000000000000000000000000000000000007d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000303030303030303030303030303030300000000000000000000000000000000000fcfc006a004000000027010000000000000000000e0421000000000000000010cc00000a160000be000000000000000000000000000000000000000000000000000000000000000000000000303030303030303030303030303030300000000000000000000000000000000101fcfc006a000f0000000000000000000000000000000000000000000000000007d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000303030303030303030303030303030300000000000000000000000000000000000fcfc666ddd";
		        
		        String hexString = "0067004000000027010000000000000000000e0421000000000000000010cc00000a160000be0000000000000000000000000000000000000000000000000000000000000000000000003030303030303030303030303030303000000000000000000000000000000000";
		        
		        byte[] buf = StringUtil.hexToBytes(hexString);
		        
		        
//		        System.out.println(buf.length);
//		        System.out.println(mes);
		        
		        // 创建发送类型的数据报：  
		        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, ip, port);  
		
		        // 通过套接字发送数据：  
		        sendSocket.send(sendPacket);
	        
//				Thread.sleep(20);
		        Thread.sleep(5000);
	        }
	        
		}catch(IOException e) {
			//ignore
		}catch (InterruptedException e) {
			//ignore
		}finally {
			if(sendSocket != null) 
		        sendSocket.close();
		}
		
		System.out.println("耗时(秒)：" + (System.currentTimeMillis()-st)/1000 );
	}

}
