package com.yzzao.mesmid.v2;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.mesmid.Constants;
import com.yzzao.mesmid.packet.ForwardingStrategy;

public class SenderTask implements Runnable {

  private final static Logger logger = Logger.getLogger(SenderTask.class);

  /** 并发队列，用于存放数据包转成的Json对象 */
  // private final ConcurrentLinkedQueue<JSONObject> storage;
  private final BlockingQueue<JSONObject> storage;

  /** 存储每台机器数据包上次转发MES成功的时间 */
  private final Map<String, Long> lastSendSuccessPacketTimestamp;

  public SenderTask(BlockingQueue<JSONObject> storage, Map<String, Long> lastPacktimeMap) {
    this.storage = storage;
    this.lastSendSuccessPacketTimestamp = lastPacktimeMap;
  }

  @Override
  public void run() {
    Thread.currentThread().setName("Thread-SenderTask-");

    logger.info("转发线程SenderTask" + Thread.currentThread().getId() + " 启动");
    for (;;) {

      if (Constants.transmitSleepTime > 0) {
        // 休眠 20190130
        try {
          if (Thread.interrupted()) {
            logger.fatal("转发线程SenderTask" + Thread.currentThread().getId() + " 异常中断退出.");
            break;
          }
          Thread.sleep(Constants.transmitSleepTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();// 设置终端标志，供下次循环判断
        }
      }

      final JSONObject jsonObj = storage.poll();

      if (jsonObj == null) {
        continue;
      }
      
      ForwardingStrategy.forward(jsonObj, lastSendSuccessPacketTimestamp, 2);

    }

  }

}
