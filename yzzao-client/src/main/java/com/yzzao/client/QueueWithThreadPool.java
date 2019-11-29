package com.yzzao.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.client.spec.client.OriknitClient;

public class QueueWithThreadPool {
	
	private final static Logger logger = Logger.getLogger(QueueWithThreadPool.class);
	
	private static final int MAX_STORAGE = 1<<10;
	
	private ExecutorService service = Executors.newFixedThreadPool(3);
	
	private BlockingQueue<JSONObject> storage = new LinkedBlockingQueue<>(MAX_STORAGE);

	@SuppressWarnings("rawtypes")
	private volatile Map lock = new HashMap();

    final OriknitClient specClient = new OriknitClient();
    //final Map<Integer, OriknitClient> specClientMap = new ConcurrentHashMap();
    
	public void start() {
		logger.info("采集数据线程队列启动...");
		
		service.execute(new ReceiverTask(storage));

		service.execute(new SenderTask(storage, null, specClient));
		
		service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    specClient.connect(Constants.RemotePort, Constants.RemoteIP, 0x1004, new ConcurrentHashMap<Integer, OriknitClient>(), new ConcurrentHashMap<Integer, Map<String, String>>(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }}
		);
		
		// 新增打印机台接收数据包计数 20180131
		//ShowMachineCounterTask smct = new ShowMachineCounterTask();
		//service.execute(smct);
	}
	
    public void startWithWSocket() {

    	logger.info("采集数据线程队列启动....");

    	service.execute(new ReceiverTask(storage));
    	
    	WSClient wsClient = new WSClient(lock);
    	SenderTask st = new SenderTask(storage, wsClient, specClient);

    	service.execute(st);
    	
		// WSClient 断开重连
		synchronized (lock) {
			for (;;) {
				try {

					lock.wait(5000); // 释放锁，等待其他线程唤醒

				} catch (InterruptedException e) {
					// ignore
				}

				logger.debug(wsClient.isColsed() ? "连接关闭" : "连接成功保持");

				if (wsClient.isColsed()) {
					
					if(lock.get("code")!=null && (Integer)lock.get("code")==1003) {//@see CloseReason class in Tomcat7.0.49+ WebSocket-api.jar
						
						logger.debug("服务端拒绝连接:" + lock.get("reason") + ", remote=" + lock.get("remote"));
						
						break;
					
					}
					
					logger.info("WSClient 重新连接...");

					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						//ignore
					}
					
					wsClient = new WSClient(lock);//

					st.setClient(wsClient); // 重新设置一个WebSocket Client

					logger.info("线程池重新加入发送任务...");

					service.execute(st);

				}

			}
		}
		
		if(lock.get("code")!=null && (Integer)lock.get("code")==1003) {
			

			service.shutdown();
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			logger.debug(service.isShutdown());

			logger.info("退出重新连接机制，同时关闭发送和接收线程");

			System.exit(0); // 强制结束，包括结束后台接收数据线程
			
		}
		
    }
    
    
	
    @Deprecated
    public void shutdown() {
    	service.shutdown();
    }
    
    public static void main(String[] args) {
        Map<Integer, OriknitClient> specClientMap = new ConcurrentHashMap();
        specClientMap.put(1,new OriknitClient());
    	System.out.println(specClientMap.get(1));
    }
}
