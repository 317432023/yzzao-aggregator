package com.yzzao.mesmid.v2;

import com.yzzao.common.utils.HttpUtil;
import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.Constants;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket.READYSTATE;
import org.java_websocket.client.WebSocketClient;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WSClient {
  private ExecutorService service = Executors.newCachedThreadPool();
  private static final Logger logger = Logger.getLogger(WSClient.class);
  private StringBuffer strbuff;
  private WebSocketClient client;
  private URI uri;
  private long connFailCount = 0;
  private long reConnCount = 0;
  private ConcurrentLinkedQueue<String> mesFeedbackScanToFileQueue;
  private Map<Integer, String> barcodeCardNo;

  /** 并发队列 */
  private static final int qlen = 100;
  private final static ArrayBlockingQueue<String> storage = new ArrayBlockingQueue<>(qlen);
  public static ArrayBlockingQueue<String> getStorage() {return storage;}

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

  public void initClient() {
    if("yes".equalsIgnoreCase(Constants.WS_START_FLAG)) {
      // 连接前先请求此地址激活ws
      HttpUtil.get(Constants.WS_START_URL, null, 2000, 2000);
    }

    strbuff = new StringBuffer(256);

    client = new BarWebSocketClient(uri, service, strbuff, barcodeCardNo, mesFeedbackScanToFileQueue);

    client.connect();

    logger.info(client.getDraft());

    int i = 0;
    while(!client.isOpen()) {
      try {
        Thread.sleep(500L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      logger.debug("connecting mes socket...");

      i++;
      if(i > 10) {
        //break;
        logger.info("连接失败" + ++connFailCount + "次，待发送队列="+ storage.size()+"/"+qlen+","+ "稍后后重连");
        return;
      }
    }

    logger.debug("connected mes socket ok.");
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
