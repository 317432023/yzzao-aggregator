package com.yzzao.mesmid.v2;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocket.READYSTATE;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import com.yzzao.common.utils.DateUtil;
import com.yzzao.common.utils.HttpUtil;
import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.Constants;
import com.yzzao.mesmid.v2.serv.ChannelSession;
import com.yzzao.mesmid.v2.struct.Message;

public class WSClient {
  private ExecutorService service = Executors.newCachedThreadPool();
  private static final Logger logger = Logger.getLogger(WSClient.class);
  private StringBuffer strbuff;
  private WebSocketClient client;
  private URI uri;
  private long connFailCount = 0;
  private long reConnCount = 0;
  private static final int qlen = 100;
  /** 并发队列 */
  private final static ArrayBlockingQueue<String> storage = new ArrayBlockingQueue<>(qlen);
  private ConcurrentLinkedQueue<String> mesFeedbackScanToFileQueue;
  private Map<Integer, String> barcodeCardNo;
  
  public WSClient(@SuppressWarnings("rawtypes") final Map lock, String addr, ConcurrentLinkedQueue<String> mesFeedbackScanToFileQueue, Map<Integer, String> barcodeCardNo) {
    if (addr == null || !(addr.startsWith("ws:") || addr.startsWith("wss:"))) {
      logger.fatal("ws server 地址非法");
      System.exit(0);
    }
    try {
      uri = new URI(addr);
    } catch (URISyntaxException e) {
      logger.fatal("Websocket Server URI非法", e);
      // ignore
    }
    if (uri == null)
      return;
    
    this.mesFeedbackScanToFileQueue = mesFeedbackScanToFileQueue;
    this.barcodeCardNo = barcodeCardNo;
  }

  public void connect() {
    
    if("yes".equalsIgnoreCase(Constants.WS_START_FLAG)) {
      // 连接前先请求此地址激活ws
      HttpUtil.get(Constants.WS_START_URL, null, 2000, 2000);
    }
    
    strbuff = new StringBuffer(256);
    
    client = new WebSocketClient(uri, new Draft_6455()/* new Draft_17() */) {

      @Override
      public void onOpen(ServerHandshake handshakedata) {
        // 已连接,发送工厂ID
        final String facID = "{\"cd\":\"8000\",\"appid\":\"" + Constants.APPID + "\"}";
        logger.info("WebSocket握手成功,发送工厂ID -> " + facID);
        client.send(facID);
      }

      @Override
      public void onMessage(String message) {
        
        strbuff.append(message);
        String mayJson = strbuff.toString();
        JSONObject jsonObj = null;
        try {
          jsonObj = JSONObject.fromObject(mayJson);
        }catch(Exception e) {
          logger.warn("解析文本json失败=>" + mayJson);
        }
        
        if (jsonObj!=null) {
          logger.info("接收WebSocket JSON : " + mayJson);
          strbuff = new StringBuffer(256);
          
          // 下发
          final String cd = jsonObj.has("cd")?jsonObj.getString("cd"):(jsonObj.has("code")?jsonObj.getString("code"):"");
          if ("8001".equals(cd)) {
            Map<String, List<Message>> msglstMap = null;
            try {
              msglstMap = Mesmid2ndUtils.jsonStr2Message(mayJson);
            } catch (UnsupportedEncodingException ex) {
              logger.error("error occured -> " + ex.getMessage());
            }
            if (msglstMap == null) {
              logger.error("WS消息解析失败");
              return;
            }
            if (msglstMap.size()==0) {
              return;
            }
            
            for (final Map.Entry<String, List<Message>> e : msglstMap.entrySet()) {

              // 检查数据是否空数据
              if (StringUtils.isBlank(e.getKey()) || e.getValue() == null || e.getValue().size() == 0)
                continue;

              final String sessionId = "knit_" + e.getKey();

              // 检查机器是否在线
              if (ChannelSession.channel(sessionId) == null) {
                logger.error("machine " + e.getKey() + " offline.");
                continue;
              }

              // 准备订单和进度
              StringBuffer sbff = new StringBuffer();
              Message shedMsg = null;
              for (Message msg : e.getValue()) {
                logger.info(StringUtil.bytesToHex(new byte[] { msg.getMainCd() })
                    + StringUtil.bytesToHex(new byte[] { msg.getSubCd() }) + ":" + new String(msg.getPar()));
                if (msg.getMainCd() == 0x20 && msg.getSubCd() == 0x01) {
                  sbff.append(StringUtil.bytesToHex(msg.getPar()));
                } else if (shedMsg != null && msg.getMainCd() == 0x20 && msg.getSubCd() == 0x05) {
                  shedMsg = msg;
                }
              }
              final Message ord = new Message((byte) 0x20, sbff.length() > 16 ? (byte) 0x02 : (byte) 0x01,
                  StringUtil.hexToBytes(sbff.toString()));
              final Message shed = shedMsg;

              service.execute(new Runnable() {
                @Override
                public void run() {
                  // 发送订单及进度
                  logger.info("[8001]转发[200?]订单给下位机");
                  ChannelSession.sendMessage(sessionId, ord.composeFull());
                  if (shed != null) {
                    try {
                      Thread.sleep(50);
                    } catch (InterruptedException ex) {
                      logger.error("停顿50ms异常.");
                    }
                    logger.info("[8001]转发[2005]进度给下位机");
                    ChannelSession.sendMessage(sessionId, shed.composeFull());
                  }

                  // 发送通知
                  for (Message msg : e.getValue()) {
                    if (msg.getMainCd() == (byte) 0x20 && msg.getSubCd() == (byte) 0x01)
                      continue;
                    if (msg.getMainCd() == (byte) 0x20 && msg.getSubCd() == (byte) 0x05)
                      continue;
                    if (msg.getMainCd() == (byte) 0x12 && msg.getSubCd() == (byte) 0x05) {
                      logger.info("[8001]转发[1205]通知给下位机");
                      ChannelSession.sendMessage(sessionId, msg.composeFull());
                      try {
                        Thread.sleep(50);
                      } catch (InterruptedException ex) {
                        logger.error("停顿50ms异常.");
                      }
                      break;
                    }
                  }

                }
              });

            }
          } else if ("9004".equals(cd)) {
            // 转发手持设备工厂消息通知
            final String barmachineid = jsonObj.getString("barmachineid");
            final String sessionId = "mobile_" + barmachineid;
            final JSONArray jsonArr = jsonObj.getJSONArray("data");
            service.execute(new Runnable() {

              @Override
              public void run() {
                byte[] ball = new byte[0];
                for (int i = 0; i < jsonArr.size(); i++) {
                  JSONObject _jo = jsonArr.getJSONObject(i);
                  final String turncount = _jo.getString("turncount");// 总转数
                  final String notice = _jo.getString("notice");// 工厂通知信息
                  final String machineid = _jo.getString("machineid");// 纺织机设备号
                  if (StringUtils.isNotBlank(barmachineid)) {
                    if (StringUtils.isNotBlank(notice) && !"null".equals(notice)) {
                      Message msg = new Message((byte) 0x90, (byte) 0x06, notice.getBytes());
                      logger.info("[9004]转发[9006]工厂通知信息给手持机");
                      ChannelSession.sendMessage(sessionId, msg.composeFull());
                      try {
                        Thread.sleep(500);
                      } catch (InterruptedException e) {
                        logger.error("error occured!");
                      }
                    }
                    // 机器号1（2个字节）+当班圈数1（4个字节）+机器号2（2个字节）+当班圈数2（4个字节）+...以此类推
                    if (StringUtils.isNotBlank(machineid) && StringUtils.isNotBlank(turncount)) {
                      int machineidint = Integer.parseInt(machineid);
                      int turncountint = Integer.parseInt(turncount);
                      byte[] b1 = StringUtil.intToBytes2(machineidint);
                      byte[] b2 = StringUtil.intToBytes2(turncountint);
                      byte[] bs = new byte[] { b1[2], b1[3], b2[0], b2[1], b2[2], b2[3] };
                      ball = StringUtil.merge(ball, bs);
                    }
                  }
                }
                // 发送纺织机转数给手持设备
                if (ball.length > 0) {
                  
                  Message msg = new Message((byte) 0x90, (byte) 0x07, ball);
                  logger.info("[9004]转发[9007]转数给手持机");
                  try {
                    Thread.sleep(1000);
                  } catch (InterruptedException e) {
                    logger.error("error occured!");
                  }
                  
                  ChannelSession.sendMessage(sessionId, msg.composeFull());
                  
                }

              }

            });

          }else if("9005".equals(cd)) {
            
            final String barmachineid = jsonObj.getString("barmachineid");
            final String sessionId = "mobile_" + barmachineid;
            final int state = jsonObj.getInt("state");
            final String msg = jsonObj.getString("msg");
            final String msgid = jsonObj.getString("msgid");
            
            String[] msgidArr = StringUtils.split(msgid, '-');
            final int barWorkShopID = new Integer(msgidArr[0]);
            final int barmachindid1 = new Integer(msgidArr[1]);
            final int packId = new Integer(msgidArr[2]);
            
            service.execute(new Runnable() {
              @Override
              public void run() {
                // 反馈接收结果
                Message fb = new Message((byte) 0x80, (byte) 0x03, new byte[] { (byte) 0x90, (byte)0x05,
                  (byte)state, 
                  (byte)(barWorkShopID & 0xFF), 
                  (byte) (barmachindid1 >> 8 & 0xFF), (byte) (barmachindid1 & 0xFF),
                  (byte) (packId >> 8 & 0xFF), (byte) (packId & 0xFF) 
                });
                logger.info("[9005]转发[8003]给手持机反馈MES接收结果" + msg);
                ChannelSession.sendMessage(sessionId, fb.composeFull());
              }
            });
            
            // 放进写文件队列
            StringBuffer strBuf = new StringBuffer(256);
            
            strBuf.append(DateUtil.getDate(new Date(), DateUtil.CHN_LONG_FORMAT))
              .append('\t').append("手持机id:" + barmachineid)
              .append('\t').append("员工No:" + barcodeCardNo.get(barmachineid))
              .append('\t').append("state:" + state)
              .append('\t').append("msg:" + msg);
            
            mesFeedbackScanToFileQueue.add(strBuf.toString());
          }

        }

      }

      @Override
      public void onClose(int code, String reason, boolean remote) {
        logger.error("链接已关闭:" + "reason=" + reason + ",code=" + code);
      }

      @Override
      public void onError(Exception ex) {
        logger.error("发生错误已关闭:" + ex.getMessage());
      }

      @Override
      public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
        logger.info("onWebsocketMessageFragment()");
        super.onWebsocketMessageFragment(conn, frame);
      }

      @Override
      public void onWebsocketCloseInitiated(WebSocket conn, int code, String reason) {
        logger.info("onWebsocketCloseInitiated()");
        super.onWebsocketCloseInitiated(conn, code, reason);
      }

      @Override
      public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("onWebsocketClosing(), code-> "+ code);
        super.onWebsocketClosing(conn, code, reason, remote);
      }

      @Override
      public void onCloseInitiated(int code, String reason) {
        logger.info("onCloseInitiated()");
        super.onCloseInitiated(code, reason);
      }

      @Override
      public void onClosing(int code, String reason, boolean remote) {
        logger.info("onClosing(), code-> " + code);
        super.onClosing(code, reason, remote);
      }

      @Override
      public void onMessage(ByteBuffer bytes) {
        logger.info("onMessage(bytes)");
        super.onMessage(bytes);
      }

      @Override
      public void onFragment(Framedata frame) {
        logger.info("onFragment(frame)");
        super.onFragment(frame);
      }
    };
    
    client.connect();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e1) {
      logger.equals("error occured -> " + e1.getMessage());
    }
    
    // 连接状态检测
    while (client.isOpen()) {
      
      // 已连接重发ws消息，发送成功移除
      boolean assumeSendSuccess = true;
      
      while (storage.size() > 0 && assumeSendSuccess) {
        final String text = storage.peek();
        if (StringUtils.isBlank(text) || (assumeSendSuccess = this.send(text)) ) {
          // 发送成功移除
          storage.poll();
          logger.info("重新发送ws消息成功，当前重发队列占用" + storage.size() + "/100，已发送消息内容-->" + text);
          
          try {
            Thread.sleep(50L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          
        } else {
          logger.error("重新发送ws消息失败，下次检测将重新发送 -->" + text);
        }
      }

      // 设置心跳检测间隔
      try {
        Thread.sleep(Constants.WS_HEARTBEAT_INTERVAL);
      } catch (InterruptedException e) {
        logger.error("error occured! " + e.getMessage());
      }

    }
    
    logger.info("连接失败" + ++connFailCount + "次，待发送队列="+ String.valueOf(storage.size())+"/"+String.valueOf(qlen)+","+ String.valueOf(Constants. WS_RECONNECT_INTERVAL/1000) + "秒后重连");
    
    // 关闭连接中的链路
    client.close();
    
    // 连接失败,x秒后重新连接
    try {
      Thread.sleep(Constants.WS_RECONNECT_INTERVAL);
    } catch (InterruptedException e) {
      logger.error("error occured! " + e.getMessage());
    }
    
    logger.info("第n次连接断开后重新连接.n=" + ++reConnCount);
    connect();
    
  }

  public WebSocketClient getClient() {
    return this.client;
  }

  public boolean send(String text) {
    if (text == null)
      return true;

    if (client.getReadyState() == READYSTATE.OPEN) {
      client.send(text);
      return true;
    } else {
      logger.error("websocket未连接发送失败=>" + text);
      return false;
    }
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

  public static boolean add(String text) {
    return storage.add(text);
  }

  public static void main(String[] args) throws UnsupportedEncodingException {
    final String s = "{a:null}";
    JSONObject jo = JSONObject.fromObject(s);
    System.out.println(jo.getString("a"));
    byte[] bs = StringUtil.hexToBytes("d778d176");
    System.out.println(new String(bs, "gbk"));
    long i = Long.parseLong("d778d176", 16);
    System.out.println(i);
    String s1 = "";
    for (byte b : bs) {
      s1 += (b & 0xff);
    }
    System.out.println(s1);
  }
}
