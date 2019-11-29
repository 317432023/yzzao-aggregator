package com.yzzao.mesmid.ws;

import java.net.URISyntaxException;
 
public class SocketClientEngine{
 
 
	public static void main(String[] args) {
		try {
			WebClientEnum.initClient(new MsgWebSocketClient("ws://115.159.6.18:8083"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}