package com.yzzao.client.multicli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.yzzao.client.Constants;
import com.yzzao.client.spec.client.OriknitClient;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.HttpUtil;
import com.yzzao.common.utils.StringUtil;

public class SenderTask implements Runnable {
    private static final Logger logger = Logger.getLogger(SenderTask.class);
    private volatile BlockingQueue<JSONObject> storage;
    private volatile Map<Integer, OriknitClient> specClientMap;
    private volatile Map<Integer, Long> timer;
    /**
     * 实时参数查询
     */
    private volatile Map<Integer, Map<String, String>> realMap = new ConcurrentHashMap<>();
    private ExecutorService service = Executors.newCachedThreadPool();
    public SenderTask(BlockingQueue<JSONObject> storage, Map<Integer, OriknitClient> specClientMap, Map<Integer, Map<String, String>> realMap, Map<Integer, Long> timer) {
        
        this.storage = storage;
        this.specClientMap = specClientMap;
        this.realMap = realMap;
        this.timer = timer;
    }
    @Override
    public void run() {
        for(;;){
            // 睡眠100ms，缓解CPU占用
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e.toString());
            }
            
            final JSONObject jsonObj = storage.poll();
            
            // add for 决定是否发送 by kangtengjiao 20180203
            if(!Constants.IS_SEND) continue; 
            
            final String jsonStr = jsonObj != null ? jsonObj.toString() : null;
            
            if(StringUtils.isNotBlank(jsonStr)) {

                // 测试后删除
                //jsonObj.element("machineID", 1);
                
                // add by kangtengjiao 20181009 上报OPC数据
                
                // 1. 发送tcp消息
                //jsonObj.element("factor_id", 3);
                final Message message = com.yzzao.client.SenderTask.buildDataUpReq(jsonObj, realMap);
                try {
                    int machineID = jsonObj.getInt("machineID");
                    final int gauge = OriknitUtils.machineID2DevSeq(machineID);
                    timer.put(gauge, System.currentTimeMillis());//20181126 
                    if(gauge==0) {
                        logger.warn("machineID="+machineID);
                        continue;
                    }
                    final OriknitClient cc = specClientMap.get(gauge)==null?new OriknitClient():specClientMap.get(gauge);
                    
                    if(specClientMap.get(gauge)==null) {
                        specClientMap.put(gauge, cc);
                        //if(cc.getFuture() == null || !cc.getFuture().channel().isOpen()) {
                            service.execute(new Runnable(){
                                @Override
                                public void run() {
                                    cc.connect(Constants.RemotePort, Constants.RemoteIP, gauge, specClientMap, realMap, timer);
                                }
                            });
//                            //等待两秒让连接完成
//                            try {
//                                Thread.sleep(2000);
//                            } catch (InterruptedException e) {
//                                logger.error(e.toString());
//                            }
                        //}
                        
                    }
                    
                    boolean re = cc.sendMessage(message);
                    
                    // 发送报警
                    if(message.isAlarm()) {
                        Thread.sleep(50);
                        Message alarmMsg = buildAlarmMsg(gauge,realMap);
                        cc.sendMessage(alarmMsg);
                    }
                    
                    if(!re) logger.error("tcpip send fail =>" + StringUtil.bytesToHex(Message.composeFull(message)));
                }catch(Exception e) {
                    logger.error("tcpip send error =>" + e.getMessage() + " "+StringUtil.bytesToHex(Message.composeFull(message)));
                }
                
                // 2. POST给MES
                service.execute(new Runnable() {
                    @Override
                    public void run() {

                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("APPID", Constants.APPID+"");
                        map.put("APPSecret", Constants.APPSecret);
                        map.put("data", jsonStr);
                        String ret = HttpUtil.post(Constants.REQ_URL, map);
                        if(ret == null) {
                            logger.error("post fail:"+jsonStr);
                        }else{
                            logger.info("post:"+jsonStr);
                        }
                        
                    }
                });
                
            }//end if
        }
    }
    
    public static Message buildAlarmMsg(final int gauge,  Map<Integer, Map<String, String>> realmap) {
        Message message = new Message();
        Header header = new Header();
        header.setComSeqNo(OriknitUtils.comSeqNo());
        header.setTs(OriknitUtils.curHexTs());
        Guid reqGuid = new Guid();
        Guid respGuid = new Guid();
        reqGuid.setMerNo(OriknitUtils.merNoOfReq());
        reqGuid.setDevTyNo(OriknitUtils.devTyNoReq());
        reqGuid.setDevSeqNo(OriknitUtils.devSeqNoReq(gauge));
        respGuid.setMerNo(OriknitUtils.merNoOfResp());
        respGuid.setDevTyNo(OriknitUtils.devTyNoResp());
        respGuid.setDevSeqNo(OriknitUtils.devSeqNoResp());
        
        header.setReqGuid(reqGuid);
        header.setRespGuid(respGuid);
        //header.setLength(message.getParCd() != null ? 2+message.getParCd().length()/2 : 2); //长度编码时确定
        message.setHeader(header);
        message.setFunTpNo((byte)0xf2);
        message.setSubFunTpNo((byte)0x01);
        final String cd = realmap.get(gauge).get("111alarm");
        message.setParCd(cd);
        return message;
    }
}
