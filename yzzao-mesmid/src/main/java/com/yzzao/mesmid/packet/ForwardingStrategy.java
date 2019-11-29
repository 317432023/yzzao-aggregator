package com.yzzao.mesmid.packet;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.DateUtil;
import com.yzzao.common.utils.HttpUtil;
import com.yzzao.mesmid.Constants;

public final class ForwardingStrategy {

  private final static Logger logger = Logger.getLogger(ForwardingStrategy.class);
  
  /**
   * 转发消息
   * @param jsonObj
   * @param lastSendSuccessPacketTimestamp 上次发送成功的包创建时间
   * @param grade 1-一代机; 2-二代机
   */
  public static void forward(final JSONObject jsonObj, final Map<String, Long> lastSendSuccessPacketTimestamp, final int grade) {
    // 发送开始时间
    final long nowStartTimestamp = System.currentTimeMillis();

    final int machineId = jsonObj.getInt("machineID");

    final String mesKey = "knit_" + machineId;

    // 每取出一条消息，检查是否超时，超时记录日志不转发MES

    Date headPackDate = DateUtil.getDate(jsonObj.getString("CreateTime"), "yyyy/MM/dd HH:mm:ss");
    final long headPackTimestamp = headPackDate.getTime();
    
    // 消息延时毫秒
    
    final long diff = nowStartTimestamp - headPackTimestamp;
    
    if (Constants.transmitStrategy == 1 && diff >= (Constants.mesOffLineThreshold - Constants.avgPostCost)) {
      logger.warn(new StringBuffer(1000).append(grade).append("下位机[").append(machineId).append("]消息超时")
          .append((int) (diff / 1000)).append("秒，").append("忽略转发此消息=>\r\n")
          .append(jsonObj.toString()).toString());
      return;
    }

    // 转发队列消息，若转发成功记录上次转发的时间

    final String jsonStr = jsonObj.toString();
    Map<String, String> map = new LinkedHashMap<>();
    map.put("APPID", Constants.APPID);
    map.put("APPSecret", Constants.APPSecret);
    map.put("data", jsonStr);

    final String ret = HttpUtil.post(grade==1?Constants.REQ_URL_V1:Constants.REQ_URL_V2, map, Constants.connectTimeout, Constants.readTimeout, false);

    // 发送结束 时间
    long nowEndTimestamp = System.currentTimeMillis();

    if (ret == null) {

      logger.error(new StringBuffer(1000).append(grade).append("post fail, GUID:[").append(nowStartTimestamp).append("],").append(" machineID:[").append(machineId).append("], 耗时:[")
          .append(nowEndTimestamp - nowStartTimestamp).append("]毫秒,消息延时:[").append(diff).append("]毫秒").append(",APPID=")
          .append(Constants.APPID).append("&APPSecret=").append(Constants.APPSecret).append("&data=").append(jsonStr)
          .toString());

    } else {

      if (logger.isInfoEnabled()) {
        
        if(Constants.logSuccessPack==0) {
          logger.info(new StringBuffer(1000).append(grade).append("post ok, GUID:[").append(nowStartTimestamp).append("],").append(" machineID:[").append(machineId).append("], 耗时:[")
              .append(nowEndTimestamp - nowStartTimestamp).append("]毫秒, 消息延时:[").append(nowEndTimestamp - headPackTimestamp).append("]毫秒").toString());
        }else{
          logger.info(new StringBuffer(1600).append(grade).append("post ok, GUID:[").append(nowStartTimestamp).append("],").append(" machineID:[").append(machineId).append("], 耗时:[")
              .append(nowEndTimestamp - nowStartTimestamp).append("]毫秒, 消息延时:[").append(nowEndTimestamp - headPackTimestamp).append("]毫秒")
              .append(",APPID=").append(Constants.APPID).append("&APPSecret=").append(Constants.APPSecret)
              .append("&data=").append(jsonStr).toString());
        }
        
      }

      lastSendSuccessPacketTimestamp.put(mesKey, headPackTimestamp);
    }
  }
  
  public static void warnDropPackMsg( final String logMsg ) {
    logger.warn(logMsg); 
  }
  
}
