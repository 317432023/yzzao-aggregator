package com.yzzao.mesmid.v2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.yzzao.mesmid.ws.WebClientEnum;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.yzzao.common.utils.DateUtil;
import com.yzzao.mesmid.Constants;
import com.yzzao.mesmid.v2.serv.MidServer;

public class QueueWithThreadPool {
  private final static Logger logger = Logger.getLogger(QueueWithThreadPool.class);

  // 20190317改为缓冲线程池
  private ExecutorService service = Executors.newCachedThreadPool();

  /** 并发队列，用于存放数据包转成的Json对象 */
  private final BlockingQueue<JSONObject> storage = new ArrayBlockingQueue<>(Constants.mesOffLineThreshold
      / Constants.avgPostCost);

  /** 存储每台机器数据包上次转发MES成功的时间 */
  private final Map<String, Long> lastSendSuccessPacketTimestamp = new ConcurrentHashMap<>();

  /** 手持机扫码 */
  private final ConcurrentLinkedQueue<String> mobileScanToFileQueue = new ConcurrentLinkedQueue<>();
  /** 接收MES反馈扫码结果 */
  private final ConcurrentLinkedQueue<String> mesFeedbackScanToFileQueue = new ConcurrentLinkedQueue<>();
  
  /** 手持机-员工 map*/
  private final Map<Integer, String> barcodeCardNo = new ConcurrentHashMap<>(64);
  
  /** 机台-crc 映射*/
  private final Map<Integer, Short> machineCrcMap = new ConcurrentHashMap<>(256);
  
  @SuppressWarnings("rawtypes")
  private volatile Map lock = new HashMap();

  private volatile WSClient wscli;

  public WSClient getWscli() {
    return wscli;
  }

  public void start() throws Exception {

    new MidServer(storage, mobileScanToFileQueue, barcodeCardNo, machineCrcMap, mesFeedbackScanToFileQueue).bind();

    // 启动转发下位机转数线程
    for (int i = 0; i < Constants.transThreadsCountV2; i++) {
      service.execute(new SenderTask(storage, lastSendSuccessPacketTimestamp));
    }

    if (logger.isDebugEnabled()) {
      logger.debug("二代采集数据线程队列启动...");
      logger.debug(new StringBuilder(50).append("二代初始化").append(Constants.transThreadsCountV2).append("个转发MES的线程.")
          .toString());
    }
    
    // 启动WebSocket客户端
    service.execute(new Runnable() {
      @Override
      public void run() {
        wscli = new WSClient(lock, Constants.WS_ADDR, mesFeedbackScanToFileQueue, barcodeCardNo);

        // 新的连接(相对于wscli.connect()方法)
        wscli.initClient();
        // 自动检测与重连
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            try{
              JSONObject jsonObject = new JSONObject();
              jsonObject.put("APPID", Constants.APPID);
              jsonObject.put("cd", "9006");
              //jsonObject.put("barmachineid", Constants.APPID);
              jsonObject.put("msgid", ComSeqNoCounter.getAndIncrement());
              wscli.send(jsonObject.toString());
            }catch(Exception e){
              ArrayBlockingQueue<String> queue = WSClient.getStorage();
              logger.info("检测到连接已经失效或断开，待发送队列="+ queue.size()+"/"+100);
              wscli.getClient().close();
              wscli.initClient();
            }
          }
        }, new Date(), Constants.WS_RECONNECT_INTERVAL);// 心跳间隔

        // 线程内死循环发送手持机队列消息
        new Thread(new Runnable(){
          @Override
          public void run() {
            ArrayBlockingQueue<String> queue = WSClient.getStorage();
            while (wscli.getClient().isOpen() && queue.size() > 0) {
              final String text = queue.peek();
              if (StringUtils.isBlank(text) || wscli.send(text) ) {
                // 发送成功移除
                queue.poll();
                logger.info("重新发送ws消息成功，当前重发队列占用" + queue.size() + "/100，已发送消息内容-->" + text);

                try {
                  Thread.sleep(50L);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }

              } else {
                logger.error("重新发送ws消息失败，下次检测将重新发送 -->" + text);
              }
            }
          }
        }).start();

      }

    });
    
    // 启动写文件9005线程
    service.execute(new Runnable() {
      @Override
      public void run() {
        
        for(;;) {
          
          writeFileByThread(mobileScanToFileQueue, "barScan");
          
          try {
            Thread.sleep(500L);
          } catch (InterruptedException e) {
            logger.error("写文件线程异常！-> " + e.getMessage());
          }
        }
      }// end run()
      
    });

    // 启动写文件9005反馈线程
    service.execute(new Runnable() {
      @Override
      public void run() {
        for(;;) {
        
          writeFileByThread(mesFeedbackScanToFileQueue, "scanFeedback");
          
          try {
            Thread.sleep(500L);
          } catch (InterruptedException e) {
            logger.error("写文件线程异常！-> " + e.getMessage());
          }
        }
      }// end run()
      
    });
    
    
  }
  
  private void writeFileByThread(ConcurrentLinkedQueue<String> queue, String fileNamePrefix) {
    
    String string = null;
    
    while( ( string = queue.poll() ) != null ){
      
      Calendar cal = Calendar.getInstance();
      
      final String dateString = DateUtil.getDate(cal.getTime(), "yyyyMMdd");
      
      String filePath = new StringBuffer(100).append(Constants.scanFilePath).append(File.separator)
          .append("scan").append( File.separator)
          .append(cal.get(Calendar.YEAR)).append(File.separator)
          .append( ( cal.get(Calendar.MONTH)+1 ) ).toString();
      File directory = new File(filePath);
      
      boolean exists = directory.exists();
      if(!exists) {
        logger.info("创建目录 " + filePath);
        exists = directory.mkdirs();
      }
      
      if(exists) {
        final String fileName = fileNamePrefix + "_" + dateString + ".dat";
        String fullfileName = new StringBuffer(120).append(filePath).append(File.separator).append(fileName).toString();
        File file = new File(fullfileName);
        if(!file.exists()) {
          try {

            logger.info("创建文件 " + fullfileName);
            file.createNewFile();
          } catch (IOException e) {
            logger.error("创建文件失败！-> " + fullfileName);
          }
        }
        
        logger.info("开始写文件");
        OutputStreamWriter out = null;
        try {
          //FileUtils.writeStringToFile(file, string + "\r\n", "UTF-8");
          out = new OutputStreamWriter(
              new FileOutputStream(file, true), // true to append
              "UTF-8"
          );
          out.write(string + "\r\n");
          out.flush();

        } catch (IOException e) {
          logger.error("写文件记录失败！-> " + string);
        } finally {
          if(out !=null)
            try {
              out.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
        }
        
      }else{
        logger.error("目录无法创建！-> " + filePath +", 本次内容-> " + string);
      }
      
    }//end while
    
  }

  public static void main(String[] args) {
    final Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        System.out.println("ok");
        //this.cancel();
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    },new Date(), 1000);// 5秒
  }
  
}
