package com.yzzao.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

public class WSClient {

	private static final Logger logger = Logger.getLogger(WSClient.class);
	
	private WebSocketClient client;
	
	public WSClient( @SuppressWarnings("rawtypes") final Map lock) {
		
		URI uri = null;
		try {
			uri = new URI(Constants.WS_ADDR);
		} catch (URISyntaxException e) {
			logger.error("Websocket Server URI非法", e);
			// ignore
		}
		if(uri == null) return;
		
		client = new WebSocketClient(uri, new Draft_6455()) {

			@Override
			public void onOpen(ServerHandshake handshakedata) {
				logger.debug("打开连接");
			}

			@Override
			public void onMessage(String message) {
				logger.debug("收到消息" + message);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void onClose(int code, String reason, boolean remote) {
				logger.debug("链接已关闭:" + reason);
				logger.debug("唤醒上次释放这把锁的其他线程（干一些事，重起一个WSClient）");
				
				synchronized(lock) {
					lock.put("code", code);// 1003 表示 CANNOT_ACCEPT
					lock.put("reason", reason);
					lock.put("remote", remote);
					lock.notifyAll();//唤醒上次释放这把锁的其他线程（干一些事，重起一个WSClient）
				}
			}

			@Override
			public void onError(Exception ex) {
				logger.debug("发生错误已关闭:" + ex.getMessage());
			}

			@Override
			public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
				logger.debug("onWebsocketMessageFragment()");
				super.onWebsocketMessageFragment(conn, frame);
			}

			@Override
			public void onWebsocketCloseInitiated(WebSocket conn, int code, String reason) {
				logger.debug("onWebsocketCloseInitiated()");
				super.onWebsocketCloseInitiated(conn, code, reason);
			}

			@Override
			public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {
				logger.debug("onWebsocketClosing()");
				super.onWebsocketClosing(conn, code, reason, remote);
			}

			@Override
			public void onCloseInitiated(int code, String reason) {
				logger.debug("onCloseInitiated()");
				super.onCloseInitiated(code, reason);
			}

			@Override
			public void onClosing(int code, String reason, boolean remote) {
				logger.debug("onClosing(), reason->" + reason);
				super.onClosing(code, reason, remote);
			}

			@Override
			public void onMessage(ByteBuffer bytes) {
				logger.debug("onMessage(bytes)");
				super.onMessage(bytes);
			}

			@Override
			public void onFragment(Framedata frame) {
				logger.debug("onFragment(frame)");
				super.onFragment(frame);
			}
			
		};
		
		client.connect();
	    
	}
	
	public WebSocketClient getClient() {return this.client;}

	public void send(String text) {
		/*if(client.getReadyState()==READYSTATE.OPEN) {
			client.send(text);
		}else {
			logger.error("wsc发送失败=>"+text);
		}*/
		client.send(text);
	}
	
	public boolean isColsed() {
		return client.isClosed();
	}
	
	public void close() {
		client.close();
	}
	
	public boolean isOpen() {
		return client.isOpen();
	}
}
