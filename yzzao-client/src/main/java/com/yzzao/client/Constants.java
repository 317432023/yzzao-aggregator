package com.yzzao.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class Constants {
	private final static Logger logger = Logger.getLogger(Constants.class);
	
	public static int UDPPort = 4001;
	public static String WorkMode;
	
	public static boolean IS_SEND;
	public static String REQ_URL;
	
	public static int APPID;
	public static String APPSecret;
	
	public static String WS_ADDR;
	
	public static String RemoteIP = "112.16.93.229";
	public static int RemotePort = 8100;

    public static String Passwd = "123456";
    public static String Ver = "1.0.0";
    
    public static String ExtParms="";
    /**
     * 公共模拟值
     */
    public static Map<String, String> parMap=new ConcurrentHashMap<>();
	/*
	static {
		InputStream is = null;
		
		is = Constants.class.getResourceAsStream("/app.properties");
		Properties prop = new Properties();
		try {
			prop.load(is);
		} catch (IOException e) {
			//ignore
		}
		
		try {
			if(is != null) 
				is.close();
		} catch (IOException e) {
			//ignore
		}
		WorkMode = (String)prop.get("WorkMode");
		switch(WorkMode) {
		case "A":case "a":
			break;
		case "B":case "b":
			break;
		default:
			logger.error("WorkMode invalid error.");
			break;
		}
		REQ_URL = (String)prop.get("REQ_URL");
		APPID = Integer.parseInt((String)prop.get("APPID"));
		APPSecret = (String)prop.get("APPSecret");
		WS_ADDR = (String)prop.get("WS_ADDR");
	}*/
	
}
