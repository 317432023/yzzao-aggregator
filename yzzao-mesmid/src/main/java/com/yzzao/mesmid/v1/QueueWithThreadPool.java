package com.yzzao.mesmid.v1;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.mesmid.Constants;

public class QueueWithThreadPool {

  private final static Logger logger = Logger.getLogger(QueueWithThreadPool.class);

  private ExecutorService service = Executors.newCachedThreadPool();

  //private final ConcurrentLinkedQueue<JSONObject> storage = new ConcurrentLinkedQueue<>();
  private final BlockingQueue<JSONObject> storage = new ArrayBlockingQueue<>(Constants.mesOffLineThreshold
      / Constants.avgPostCost);
  
  /** 存储每台机器数据包上次转发MES成功的时间 */
  private final Map<String, Long> lastSendSuccessPacketTimestamp = new ConcurrentHashMap<>();

  public void start() {
    if (logger.isDebugEnabled()) {
      logger.debug("采集数据线程队列启动...");
    }

    service.execute(new ReceiverTask(storage));

    for (int i = 0; i < Constants.transThreadsCountV1; i++) {
      service.execute(new SenderTask(storage, lastSendSuccessPacketTimestamp));
    }

    // 新增打印机台接收数据包计数 20180131
    // ShowMachineCounterTask smct = new ShowMachineCounterTask();
    // service.execute(smct);
  }

}
