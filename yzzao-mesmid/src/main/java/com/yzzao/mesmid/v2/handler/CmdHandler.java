package com.yzzao.mesmid.v2.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.mesmid.Constants;
import com.yzzao.mesmid.MainApp;
import com.yzzao.mesmid.v2.WSClient;
import com.yzzao.mesmid.v2.serv.ChannelSession;
import com.yzzao.mesmid.v2.struct.Message;

public class CmdHandler extends ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(CmdHandler.class);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message)msg;
        if(message.getMainCd()==(byte)0x80 && message.getSubCd()==(byte)0x03) {//接收响应
            byte[] par = message.getPar();
            if(par.length >=2) {
                if(par[0] == (byte)0x20 &&  par[1] == (byte)0x01) {
                    logger.info("[2001]下位机成功接收订单号");
                }else if(par[0] == (byte)0x20 &&  par[1] == (byte)0x02) {
                    logger.info("[2002]下位机成功接收批量订单号");
                }else if(par[0] == (byte)0x20 &&  par[1] == (byte)0x05) {
                    logger.info("[2005]下位机成功接收订单进度");
                }else if(par[0] == (byte)0x12 &&  par[1] == (byte)0x05) {
                    logger.info("[1205]下位机成功接收工厂消息通知");
                    // 反馈给MES
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.element("cd", "8002");
                    jsonObj.element("appid", Constants.APPID);
                    jsonObj.element("appsecret", Constants.APPSecret);
                    jsonObj.element("noticestate", "1");

                    Integer knitId = null;
                    byte[] knitIdBytes = ChannelSession.getAttr(ctx.channel(), "MachineID");
                    
                    //logger.info("knitIdBytes.length -> "+ (knitIdBytes!=null?knitIdBytes.length:knitIdBytes) );
                    
                    if(knitIdBytes!=null && knitIdBytes.length>0) {
                        knitId = new Integer(new String(knitIdBytes));//((knitIdBytes[0] & 0xff) << 8) + knitIdBytes[1] & 0xff;

                        jsonObj.element("machineid", knitId==null?null:knitId.toString());
                        
                        final String text = jsonObj.toString();
                        
                        logger.info("[1205]转发[8002]到MES ->" + text);
                        
                        if(!MainApp.getQwp().getWscli().send(text)){
                          WSClient.add(text);
                        }
                    } else logger.error("[1205]无法从会话中获取下位机ID");
                    
                    
                }else if(par[0] == (byte)0x90 &&  par[1] == 0x06) {
                    logger.info("[9006]手持机成功接收工厂消息通知");
                    // 反馈给MES
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.element("cd", "9008");
                    jsonObj.element("appid", Constants.APPID);
                    jsonObj.element("appsecret", Constants.APPSecret);
                    jsonObj.element("noticestate", "1");

                    Integer barmachineid = null;
                    byte[] barmachineidBytes = ChannelSession.getAttr(ctx.channel(), "MachineID");
                    //logger.info("barmachineidBytes.length -> "+ (barmachineidBytes!=null?barmachineidBytes.length:barmachineidBytes) );
                    if(barmachineidBytes!=null && barmachineidBytes.length>0)
                        barmachineid = new Integer(new String(barmachineidBytes));//((knitIdBytes[0] & 0xff) << 8) + knitIdBytes[1] & 0xff;
                    else logger.error("[9006]无法从会话中获取手持机ID");
                    
                    jsonObj.element("barmachineid", barmachineid==null?null:barmachineid.toString());
                    
                    final String text = jsonObj.toString();
                    
                    logger.info("[9006]转发[9008]给MES ->" + text);
                    
                    if(!MainApp.getQwp().getWscli().send(text)){
                    	WSClient.add(text);
                    }
                    
                }else if(par[0] == (byte)0x90 &&  par[1] == 0x07) {
                    logger.info("[9007]手持机成功接收纺织机转数");
                }
            }
        }
        else{
            ctx.fireChannelRead(msg);
        }
    }
    public static void main(String[] args) {
        System.out.println((byte)0x80);
    }
}
