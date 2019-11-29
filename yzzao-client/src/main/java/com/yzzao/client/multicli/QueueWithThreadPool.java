package com.yzzao.client.multicli;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.client.ReceiverTask;
import com.yzzao.client.spec.client.OriknitClient;

public class QueueWithThreadPool {
	
	private final static Logger logger = Logger.getLogger(QueueWithThreadPool.class);
	
	private static final int MAX_STORAGE = 1<<10;
	
	private ExecutorService service = Executors.newFixedThreadPool(3);
	
	// 队列
	private volatile BlockingQueue<JSONObject> storage = new LinkedBlockingQueue<>(MAX_STORAGE);

	// TCP 客户端
    private volatile Map<Integer, OriknitClient> specClientMap = new ConcurrentHashMap<>();
    
    // 定时器
    private volatile Map<Integer, Long> timer = new ConcurrentHashMap<>();//20181126 
    
    /**
     * 实时参数查询
     */
    private volatile Map<Integer, Map<String, String>> realMap = new ConcurrentHashMap<>();
    
	public void start() {
		logger.info("采集数据线程队列启动...");
		
		service.execute(new ReceiverTask(storage));

		service.execute(new SenderTask(storage, specClientMap, realMap, timer));
		
		service.execute(new TimerTask(timer, specClientMap));//20181126 
		
		// 新增打印机台接收数据包计数 20180131
		//ShowMachineCounterTask smct = new ShowMachineCounterTask();
		//service.execute(smct);
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
