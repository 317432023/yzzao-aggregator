package com.yzzao.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.yzzao.client.spec.struct.ext.Param;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.DateUtil;
import com.yzzao.common.utils.StringUtil;

/**
 * 主程序
 * 
 * @author kangtengjiao.co
 * @since 2017-07-06
 *
 */
public class MainApp {

	private static final Logger logger = Logger.getLogger(MainApp.class);

	public static void main(String[] args) throws Exception  {
		
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
		
		Constants.UDPPort = Integer.parseInt((String)prop.get("UDPPort"));
		Constants.WorkMode = (String)prop.get("WorkMode");
		
		Constants.IS_SEND = "yes".equalsIgnoreCase((String)prop.get("IS_SEND"))?true:false;
		Constants.REQ_URL = (String)prop.get("REQ_URL");
		Constants.APPID = Integer.parseInt((String)prop.get("APPID"));
		Constants.APPSecret = (String)prop.get("APPSecret");
		Constants.WS_ADDR = (String)prop.get("WS_ADDR");
		
		if(Constants.APPID <= 0) {logger.error("APPID invalid error.");return;}
		
		Constants.RemoteIP = (String)prop.get("RemoteIP");
		Constants.RemotePort = Integer.parseInt((String)prop.get("RemotePort"));
		
        Constants.Passwd = (String)prop.get("Passwd");
        Constants.Ver = (String)prop.get("Ver");
        
        logger.debug(Constants.UDPPort);
		logger.debug(Constants.WorkMode);
		logger.debug(Constants.IS_SEND);
		logger.debug(Constants.REQ_URL);
		//logger.debug(Constants.APPSecret);
		logger.debug(Constants.WS_ADDR);
		logger.debug(Constants.RemoteIP);
        logger.debug(Constants.RemotePort);
        //logger.debug(Constants.Passwd);
        logger.debug(Constants.Ver);
		
        // 转文本为json对象
        if(args.length>1) {
            File file = new File(args[1]);
            FileReader reader = new FileReader(file);//定义一个fileReader对象，用来初始化BufferedReader，此句遇到UTF8中文会乱码
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
            BufferedReader bReader = new BufferedReader(/*reader*/isr);//new一个BufferedReader对象，将文件内容读取到缓存
            StringBuffer sb = new StringBuffer();//定义一个字符串缓存，将字符串存放缓存中
            String s = "";
            while ((s =bReader.readLine()) != null) {//逐行读取文件内容，不读取换行符和末尾的空格
                sb.append(s + "\n");//将读取的字符串添加换行符后累加存放在缓存中
                //System.out.println(s);
            }
            bReader.close();
            String str = sb.toString();
            
            List<Map<String, String>> list = new ArrayList<>();
            JSONArray jsonArray = JSONArray.fromObject(str);
            for (Object obj : jsonArray)
            {
                JSONObject jsonObject = (JSONObject) obj;
                Map<String, String> map = new HashMap<>();
                Iterator it = jsonObject.keys();
                while (it.hasNext())
                {
                    String key = (String) it.next();
                    Object value = jsonObject.get(key);
                    map.put(key.trim(), value.toString().trim());
                }
                list.add(map);
            }
            List<Param> plist = new ArrayList<>();
            for(Map<String,String> mp:list) {
                Param p = new Param();
                p.setParGrp(mp.get("parGrp"));
                p.setParSub(mp.get("parSub"));
                p.setParCnNm(mp.get("parCnNm"));
                p.setParEnNm(mp.get("parEnNm"));
                p.setParType(mp.get("parType"));
                p.setParVal(mp.get("parVal"));
                plist.add(p);
            }

            StringBuffer sb2 = new StringBuffer();
            for(Param p:plist) {
                String val = p.getParVal();
                String hexval=null;
                String tp = p.getParType().toUpperCase();
                switch(tp) {
                case "BYTE":
                    hexval = /*String.format("%02x", Integer.parseInt(val));  */StringUtil.bytesToHex( new byte[]{StringUtil.intToBytes2(Integer.parseInt(val))[3]} );
                    break;
                case "INT32":
                    hexval = StringUtil.bytesToHex( StringUtil.intToBytes2(Integer.parseInt(val)) );
                    break;
                case "UINT16":
                    byte[] bs = StringUtil.intToBytes2(Integer.parseInt(val));
                    hexval = /*StringUtil.bytesToHex( Arrays.copyOfRange(StringUtil.intToBytes2(Integer.parseInt(val)), 2, 4)  );*/
                    StringUtil.bytesToHex( new byte[]{bs[2],bs[3]} );
                    break;
                case "STRING":
                    hexval = OriknitUtils.str2lenHexstr(val, "utf8");
                    break;
                case "DATETIME":
                    Date d = DateUtil.getDate(val, "yyyy-MM-dd HH:mm:ss");
                    if(d!=null) {
                        hexval = OriknitUtils.hexTs(d);
                    }
                    break;
                }
                
//                if(p.getParGrp().equals("2001") && p.getParSub().equals("0002")) {
//                    
//                    sb2.append(p.getParGrp()).append(p.getParSub()).append("05");
//                    Constants.parMap.put(p.getParGrp()+p.getParSub(), "05");
//                    
//                }else{
                        if(hexval!=null && !"".equals(hexval.trim())) {
                        
                            sb2.append(p.getParGrp()).append(p.getParSub()).append(hexval);
                            Constants.parMap.put(p.getParGrp()+p.getParSub(), hexval);
                            
                        }
//                }
                
            }
            Constants.ExtParms  = sb2.toString();
        }
        
        System.out.println(Constants.ExtParms);
        
        for(String s:Constants.parMap.keySet()) {
            System.out.println(s+"->"+Constants.parMap.get(s));
        }
        
		if(StringUtils.isBlank(Constants.REQ_URL)) {logger.error("REQ_URL missing or blank.");return;}
		com.yzzao.client.multicli.QueueWithThreadPool q = new com.yzzao.client.multicli.QueueWithThreadPool();
        q.start();
        
	}

}
