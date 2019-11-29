package com.yzzao.mesmid;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.yzzao.mesmid.v2.QueueWithThreadPool;

public class MainApp {

    private static final Logger logger = Logger.getLogger(MainApp.class);
    private static com.yzzao.mesmid.v2.QueueWithThreadPool qwp ;
    private static com.yzzao.mesmid.v1.QueueWithThreadPool oldQwp ;
    public static com.yzzao.mesmid.v2.QueueWithThreadPool getQwp() {
        return qwp;
    }
    public static void setQwp(QueueWithThreadPool qwp) {
        MainApp.qwp = qwp;
    }
    public static com.yzzao.mesmid.v1.QueueWithThreadPool getOldQwp() {
        return oldQwp;
    }

    public static void setOldQwp(com.yzzao.mesmid.v1.QueueWithThreadPool old_qwp) {
        MainApp.oldQwp = old_qwp;
    }
    public static void main(String[] args) throws Exception  {
        // load property file .
        if(args==null || args.length==0) {
            logger.error("无效参数异常");
            throw new Exception("无效参数异常");}

        Properties prop = null;
        InputStream in = null;
        try {
            prop = new Properties();
            in = new BufferedInputStream(new FileInputStream(args[0]));
        
            prop.load(in);
        } catch (IOException e) {
            logger.error(e);
        }finally {
            try {
                if(in!=null)in.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

        // posting parm
        Constants.APPID = prop.getProperty("APPID");
        Constants.APPSecret = prop.getProperty("APPSecret");
        
        // posting strategy
        Constants.totalMachine = Integer.parseInt(prop.getProperty("totalMachine"));
        Constants.packageFrequency = Integer.parseInt(prop.getProperty("packageFrequency"));
        Constants.avgPostCost = Integer.parseInt(prop.getProperty("avgPostCost"));
        
        Constants.transmitSleepTime = Integer.parseInt(prop.getProperty("transmitSleepTime"));
        Constants.mesOffLineThreshold = Integer.parseInt(prop.getProperty("mesOffLineThreshold"));
        Constants.transmitStrategy = Integer.parseInt(prop.getProperty("transmitStrategy"));
        
        if(Constants.transmitStrategy < 0 || Constants.transmitStrategy > 1) Constants.transmitStrategy = 0;
        
        Constants.connectTimeout = Integer.parseInt(prop.getProperty("connectTimeout"));
        Constants.readTimeout = Integer.parseInt(prop.getProperty("readTimeout"));

        Constants.logSuccessPack = Integer.parseInt(prop.getProperty("logSuccessPack"));
        
        Constants.sendTurnCountInterval = Integer.parseInt(prop.getProperty("sendTurnCountInterval"));
        
        
        logger.debug(Constants.APPID);
        logger.debug(Constants.APPSecret);
        
        logger.debug(Constants.transmitSleepTime);
        logger.debug(Constants.mesOffLineThreshold);
        logger.debug(Constants.transmitStrategy);
        logger.debug(Constants.connectTimeout);
        logger.debug(Constants.readTimeout);
        logger.debug(Constants.logSuccessPack);
        logger.debug(Constants.sendTurnCountInterval);
        
        // 工作模式
        Constants.WORK_MODE = prop.getProperty("WORK_MODE", "A");
        if("A".equalsIgnoreCase(Constants.WORK_MODE)) {
          startOld(prop);
	        start(prop);
        }else if("B".equalsIgnoreCase(Constants.WORK_MODE)){
        	startOld(prop);
        }else{
        	start(prop);
        }
        
    }
    
    private static void startOld(Properties prop)throws Exception {
    	  // set property values into Constants for 一代板.
        Constants.UDPPort = Integer.parseInt((String)prop.get("UDPPort"));
        Constants.REQ_URL_V1 = prop.getProperty("REQ_URL_V1");
        
        Constants.IS_SEND = "yes".equalsIgnoreCase(prop.getProperty("IS_SEND"));
        
        int threads = Integer.parseInt(prop.getProperty("transThreadsCountV1"));
        Constants.transThreadsCountV1 = threads<=0? (int)Math.ceil( Constants.totalMachine*Constants.avgPostCost/ (1000*Constants.packageFrequency) ) : threads;
        
        logger.debug(Constants.UDPPort);
        logger.debug(Constants.REQ_URL_V1);
        logger.debug(Constants.transThreadsCountV1);
        
        if(StringUtils.isBlank(Constants.REQ_URL_V1)) {logger.error("REQ_URL_V1 missing or blank.");return;}
		
        oldQwp = new com.yzzao.mesmid.v1.QueueWithThreadPool();
        
        oldQwp.start();
        
    }
    private static void start(Properties prop) throws Exception {

    	  Constants.REQ_URL_V2 = prop.getProperty("REQ_URL_V2");
        
        // set property values into Constants for 二代板.
        Constants.ServIP = prop.getProperty("ServIP");
        Constants.TCPServPort = Integer.parseInt(prop.getProperty("TCPServPort", "81"));
        if(Constants.TCPServPort==0) Constants.TCPServPort = 81;
        
        Constants.HttpServPort = Integer.parseInt(prop.getProperty("HttpServPort", "82"));
        if(Constants.HttpServPort==0) Constants.HttpServPort = 82;

        Constants.WS_ADDR = prop.getProperty("WS_ADDR");
        Constants.WS_START_FLAG = prop.getProperty("WS_START_FLAG");
        Constants.WS_START_URL = prop.getProperty("WS_START_URL");
        Constants.WS_HEARTBEAT_INTERVAL = Integer.parseInt(prop.getProperty("WS_HEARTBEAT_INTERVAL"));
        Constants.WS_RECONNECT_INTERVAL = Integer.parseInt(prop.getProperty("WS_RECONNECT_INTERVAL"));
        
        int threads = Integer.parseInt(prop.getProperty("transThreadsCountV2"));
        Constants.transThreadsCountV2 = threads<=0? (int)Math.ceil( Constants.totalMachine*Constants.avgPostCost/ (1000*Constants.packageFrequency) ) : threads;
        
        Constants.scanFilePath = prop.getProperty("scanFilePath");
        
        logger.debug(Constants.REQ_URL_V2);
        logger.debug(Constants.ServIP);
        logger.debug(Constants.TCPServPort);
        logger.debug(Constants.HttpServPort);
        logger.debug(Constants.WS_ADDR);

        logger.debug(Constants.transThreadsCountV2);
        logger.debug(Constants.scanFilePath);
        
        // 正式开始
        qwp = new QueueWithThreadPool();
        qwp.start();
    }

}
