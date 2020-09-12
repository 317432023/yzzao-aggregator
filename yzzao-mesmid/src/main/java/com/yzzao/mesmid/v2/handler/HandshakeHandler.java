package com.yzzao.mesmid.v2.handler;

import com.yzzao.common.utils.HttpUtil;
import com.yzzao.mesmid.v2.BarWebSocketClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.DateUtil;
import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.Constants;
import com.yzzao.mesmid.MainApp;
import com.yzzao.mesmid.v2.Mesmid2ndUtils;
import com.yzzao.mesmid.v2.WSClient;
import com.yzzao.mesmid.v2.serv.ChannelSession;
import com.yzzao.mesmid.v2.struct.Message;

public class HandshakeHandler extends ChannelInboundHandlerAdapter {
  private final static Logger logger = Logger.getLogger(HandshakeHandler.class);
  private Map<ChannelHandlerContext, Integer> clientOvertimeMap = new ConcurrentHashMap<>();
  private final int MAX_OVERTIME = 2; // 超时次数超过该值则注销连接
  
  // 最后一次发送的时间
  private Map<String, Long> lastSend9004 = new ConcurrentHashMap<>();
  
  private final ConcurrentLinkedQueue<String> mobileScanToFileQueue;
  private final Map<Integer, String> barcodeCardNo;

  private ConcurrentLinkedQueue<String> mesFeedbackScanToFileQueue;
  
  public HandshakeHandler(ConcurrentLinkedQueue<String> mobileScanToFileQueue, Map<Integer, String> barcodeCardNo, ConcurrentLinkedQueue<String> mesFeedbackScanToFileQueue) {
    this.mobileScanToFileQueue = mobileScanToFileQueue;
    this.barcodeCardNo = barcodeCardNo;
    this.mesFeedbackScanToFileQueue = mesFeedbackScanToFileQueue;
  }
  
  private void addUserOvertime(ChannelHandlerContext ctx, Map<ChannelHandlerContext, Integer> overtimeMap) {
    int oldTimes = 0;
    if (overtimeMap.containsKey(ctx)) {
      oldTimes = overtimeMap.get(ctx);
    }
    overtimeMap.put(ctx, oldTimes + 1);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    
    if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {

      Map<ChannelHandlerContext, Integer> map = clientOvertimeMap;
      

      IdleStateEvent event = (IdleStateEvent) evt;
      if (event.state() == IdleState.READER_IDLE) {
        Integer overtimeTimes = map.get(ctx);
        if (overtimeTimes == null || overtimeTimes < MAX_OVERTIME) {
          if(logger.isDebugEnabled()) {
            logger.debug("第" + (overtimeTimes == null ? 1 : (overtimeTimes + 1)) + "次读超时,发送心跳");
          }
          // 纺织机通道才需要主动发心跳
          byte[] bs = ChannelSession.getAttr(ctx.channel(), "MachineID");
          Channel ch = ChannelSession.channel("knit_"+new String(bs));
          if(ch!=null && ch.id().asLongText().equals(ctx.channel().id().asLongText())) {
            
            ctx.writeAndFlush(buildTimeoutcheck());// 20190315 modified 超时检测
            
            addUserOvertime(ctx, map);
            
          }
          
        } else {
          logger.error("设备无响应，Closing TCP/IP Connection...");

          ChannelSession.removeCh(ctx.channel());

          ctx.close();// disconnect改为close
        }

      } else if (event.state() == IdleState.WRITER_IDLE) {
        System.out.println("write 空闲");
      } else if (event.state() == IdleState.ALL_IDLE) {
        System.out.println("ALL_IDLE 空闲");
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.error("捕获异常");
    // 客户端下线或者强制退出等任何情况都触发,把连接从服务器端连接集合中删除
    ChannelSession.removeCh(ctx.channel());
    super.channelInactive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // super.channelRead(ctx, msg);

    clientOvertimeMap.remove(ctx);

    Message recvMessage = (Message) msg;

    // 清空超时次数
    if (recvMessage.getMainCd() == (byte) 0x90) {// 手持设备
      if (recvMessage.getSubCd() == (byte) 0x04) {
        if (recvMessage.getLength() == 7) {// 车间号（1个字节）+手持机ID（2个字节）+当前持有该手持机的员工卡号（4个字节）
          // 存储连接Bytes，连接信息转发MES
          byte[] par = recvMessage.getPar();
          // 设备号
          int machineID = ((par[1] & 0xff) << 8) + (par[2] & 0xff);
          // 车间号
          int workShopID = par[0] & 0xff;
          // 员工卡号
          byte[] cardByte = new byte[4];
          synchronized (this) {
            System.arraycopy(par, 3, cardByte, 0, cardByte.length);
          }
          final String cardNo = Mesmid2ndUtils.cardByte2CardNo(cardByte);
          
          final String sessionId = "mobile_" + String.valueOf(machineID);
          
          // 旧连接
          Channel preCh = null;
          
          boolean sendMES = (preCh = ChannelSession.channel(sessionId)) == null;
          
          if (sendMES) {
            
            logger.info("[9004]手持机新连接=>" + sessionId + ", channelId => " + ctx.channel().id().asLongText());
            
            barcodeCardNo.put(machineID, cardNo);
            
          }else{
            
            final String oldChannelId = preCh.id().asLongText();
            
            if(!oldChannelId.equals(ctx.channel().id().asLongText())) {
              
              ChannelSession.removeCh(preCh);
              
              logger.warn("[9004]手持机新连接=>" + sessionId + ", 关闭旧channelId => " + oldChannelId);

              barcodeCardNo.put(machineID, cardNo);
              
              preCh.close();
              
              sendMES = true;
              
            }else{ // 旧连接心跳
              
              // do nothing
              if(logger.isDebugEnabled()) {
                logger.debug("[9004]手持机心跳=>" + sessionId + ", channelId => " + oldChannelId);
              }
            }
            
          }

          
          if(sendMES) {
            // 存储连接
            ChannelSession.putCh(sessionId, ctx.channel());
            ChannelSession.putAttr(ctx.channel(), "MachineID", String.valueOf(machineID).getBytes());
            //logger.info("存储 "+ sessionId +" 连接， channelId => "+ctx.channel().id().asLongText());
          }
          
          
          if (!sendMES) {
            byte[] workShopIDBytes = ChannelSession.getAttr(ctx.channel(), "WorkShopID");
            byte[] cardNoBytes = ChannelSession.getAttr(ctx.channel(), "CardNo");
            if (workShopIDBytes == null || cardNoBytes == null) {
              logger.error("old workShopID or old cardNo is blank.");
              sendMES = true;
            } else {
              int oldWorkShopID = new Integer(new String(workShopIDBytes));
              String oldCardNo = Mesmid2ndUtils.cardByte2CardNo(cardNoBytes);
              if (oldWorkShopID != workShopID || !oldCardNo.equals(cardNo)) {
                logger.info("workShopID or cardNo change from workShopID:" + oldWorkShopID + " cardNo:" + String.valueOf(oldCardNo)
                    + " to workShopID:" + String.valueOf(workShopID) + " cardNo" + String.valueOf(cardNo));
                sendMES = true;
              }
            }
          }

          ChannelSession.putAttr(ctx.channel(), "WorkShopID", String.valueOf(workShopID).getBytes());
          ChannelSession.putAttr(ctx.channel(), "CardNo", cardByte);


          // 反馈接收结果
          java.util.Calendar cal = java.util.Calendar.getInstance();
          int year = cal.get(Calendar.YEAR) - 2000;
          int month = cal.get(Calendar.MONDAY)+1;
          int day = cal.get(Calendar.DAY_OF_MONTH);
          int hour = cal.get(Calendar.HOUR_OF_DAY);
          int minute = cal.get(Calendar.MINUTE);
          int second = cal.get(Calendar.SECOND);
          Message fb = new Message((byte) 0x80, (byte) 0x03, new byte[] { (byte) 0x90, (byte)0x04, (byte)year, (byte)month, (byte)day, (byte)hour, (byte)minute, (byte)second});
          ctx.writeAndFlush(fb);

          // 每隔一段时间发送转数
          if (!sendMES
              && (!lastSend9004.containsKey(sessionId) || System.currentTimeMillis() - lastSend9004.get(sessionId) >= Constants.sendTurnCountInterval * 1000)) {
            sendMES = true;
          }
          
          // 连接信息转发MES
          if (sendMES) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.element("cd", "9004");
            jsonObj.element("appid", Constants.APPID);
            jsonObj.element("workshopid", workShopID + "");
            jsonObj.element("barmachineid", machineID + "");
            jsonObj.element("employeeCode", cardNo);

            final String text = jsonObj.toString();
            logger.info("[9004]手持机连接信息转发给MES - > " + text);
            if (!MainApp.getQwp().getWscli().send(text)) {
              WSClient.add(text);
            }
            
            lastSend9004.put(sessionId, System.currentTimeMillis());
          }

        } else {
          logger.error("serv decode error 9004");
        }
      } else if (recvMessage.getSubCd() == (byte) 0x05) {
        
        if (recvMessage.getLength() == 24) {// 当前订单号对应的车间号（1个字节）+当前订单号对应的机器号（2个字节）+当前订单号（16个字节）+手持车间号（1）+手持id（2）+包号（2）   [废弃！！！！]
                                            // 当前订单号对应的车间号（1个字节）+当前订单号对应的机器号（2个字节）+当前订单号（16个字节）+手持id（1）+包号（4）
          // 存储连接Bytes，连接信息转发MES

          byte[] par = recvMessage.getPar();
          // 纺织机设备号
          int machineID = ((par[1] & 0xff) << 8) + (par[2] & 0xff);
          // 车间号
          int workShopID = par[0] & 0xff;
          // 员工卡号
          byte[] orderBytes = new byte[16];
          // 手持车间号
          //int barWorkShopID = par[19] & 0xff;
          // 手持id
          byte[] barmachineidBytes1 = new byte[]{par[19]};
          // 包号
          byte[] packIdBytes = new byte[]{par[20], par[21],par[22], par[23]};
          synchronized (this) {
            System.arraycopy(par, 3, orderBytes, 0, orderBytes.length);// 线程安全处理
          }
          
          final String orderNo = new String(orderBytes);
          // 手持设备ID
          Integer barmachineid = null;
          byte barmachineidBytes[] = ChannelSession.getAttr(ctx.channel(), "MachineID");
          //logger.info("barmachineidBytes.length -> " + (barmachineidBytes != null ? barmachineidBytes.length : barmachineidBytes));
          if (barmachineidBytes != null && barmachineidBytes.length > 0) {
            barmachineid = new Integer(new String(barmachineidBytes));// ((barmachineidBytes[0]
                                                                      // & 0xff)
                                                                      // << 8) +
                                                                      // barmachineidBytes[1]
                                                                      // & 0xff;
            int barmachineid1 = barmachineidBytes1[0] & 0xff;
            if(barmachineid1 != barmachineid) {
              logger.warn("手持id前后不一致, 包中取得=" + barmachineid1 +",会话取得="+barmachineid);
            }
            long packId = ((packIdBytes[0] & 0xff) << 24) +((packIdBytes[1] & 0xff) << 16) + ((packIdBytes[2] & 0xff) << 8) + (packIdBytes[3] & 0xff);//Integer.parseInt(new String(packIdBytes));
            String msgid = new StringBuffer(32)
              .append(barmachineid1).append('-')
              .append(packId)
              .toString();
            // 连接信息转发MES
            JSONObject jsonObj = new JSONObject();
            jsonObj.element("cd", "9005");
            jsonObj.element("appid", Constants.APPID);
            jsonObj.element("workshopid", workShopID + "");
            jsonObj.element("barmachineid", barmachineid == null ? null : barmachineid + "");
            jsonObj.element("machineid", machineID + "");
            jsonObj.element("barcode", orderNo);
            jsonObj.element("msgid", msgid);

            final String text = jsonObj.toString();
            
            logger.info("[9005]手持机扫码转发MES - > " + text);

            if(Constants.SCAN_TRANS_MODE == 1) { // Socket 转发扫码信息
              boolean sendResult = MainApp.getQwp().getWscli().send(text);
              if (!sendResult) {
                WSClient.add(text);
              }
            }else { // Post 转发扫码信息
              final String mayJson = HttpUtil.post("http://mes.yzzao.com/XPAdmin-First/ControlMethod-OpenAPI-YZZMES-upDayBarCode.html", jsonObj);
              JSONObject jsonObj2 = null;
              try {
                jsonObj2 = JSONObject.fromObject(mayJson);
              }catch(Exception e) {
                logger.error("扫码post转发异常 =>" + mayJson);
              }
              if(jsonObj2 != null) {

                BarWebSocketClient.process9005(jsonObj2, barcodeCardNo, mesFeedbackScanToFileQueue);
              }

            }

            // 放进写文件队列
            StringBuffer strBuf = new StringBuffer(256);
            strBuf.append(DateUtil.getDate(new Date(), DateUtil.CHN_LONG_FORMAT))
              .append('\t').append("msgid:"+msgid)
              .append('\t').append("机台id:"+machineID)
              .append('\t').append("手持机id:"+barmachineid)
              .append('\t').append("员工No:"+barcodeCardNo.get(barmachineid))
              .append('\t').append("订单条码:"+orderNo);
            
            mobileScanToFileQueue.add(strBuf.toString());
            
            
            /*
            // 反馈接收结果
            Message fb = new Message((byte) 0x80, (byte) 0x03, new byte[] { (byte) 0x90, 0x05 });
            ctx.writeAndFlush(fb);
            */
            
          }else
            logger.error("[9005]无法从会话中获取手持设备ID");

          
        } else {
          logger.error("serv decode error 90"+ StringUtil.bytesToHex(new byte[]{recvMessage.getSubCd()}) );
        }
      }
    } else if (recvMessage.getMainCd() == (byte) 0x80 && recvMessage.getSubCd() == (byte) 0x02) {
      
      if (recvMessage.getLength() == 0) {// 取得下位机握手请求后,发送握手响应
        
        logger.info("[8002]取得下位机握手请求后,发送握手响应");
        
        Message respMessage = buildHandshakeResp();
        ctx.writeAndFlush(respMessage);
      } else {// 取得下位机握手响应

        if (recvMessage.getPar() != null && recvMessage.getPar().length > 0
            && ((recvMessage.getPar())[0] & 0xff) == 0xaa) {
          // 握手成功,什么也不干
          if(logger.isDebugEnabled()) {
            logger.info("[8002][aa]取得下位机握手响应");
          }
        } else {
          // 握手失败,关闭连接
          ctx.close(); // disconnect 改为 close
        }
      }
    } else
      // 透传
      ctx.fireChannelRead(msg);
  }

  private Message buildHandshakeResp() {
    Message message = new Message((byte) 0x80, (byte) 0x02, new byte[] { (byte) 0xaa });
    return message;
  }

  private Message buildTimeoutcheck() {
    Message message = new Message((byte) 0x80, (byte) 0x03, new byte[] { (byte) 0x30, (byte) 0x02 });
    return message;
  }

  public static void main(String[] args) {
    int i = (((byte) 0x03 & 0xff) << 8) + ((byte) 0xd9 & 0xff);
    System.out.println(i);
    
  }
}
