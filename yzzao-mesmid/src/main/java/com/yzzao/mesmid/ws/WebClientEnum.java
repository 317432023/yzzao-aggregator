package com.yzzao.mesmid.ws;
 
 
public enum WebClientEnum {
 
	CLIENT;
	
	private static MsgWebSocketClient socketClient = null;
	
	public static void initClient(MsgWebSocketClient client) {
		socketClient = client;
		if(socketClient !=null) {
			socketClient.connect();
			socketClient.send("测试websocket。。。");
		}
		boolean flag = true;
		int i=1000;
		while(flag) {
			socketClient.send("测试websocket。。。"+(i--));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(i == 0) {
				flag = false;
			}
		}
	}
	
}