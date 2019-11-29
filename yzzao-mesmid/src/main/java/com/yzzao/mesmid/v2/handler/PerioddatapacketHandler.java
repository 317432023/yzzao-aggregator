package com.yzzao.mesmid.v2.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.packet.ForwardingStrategy;
import com.yzzao.mesmid.v2.Mesmid2ndUtils;
import com.yzzao.mesmid.v2.serv.ChannelSession;
import com.yzzao.mesmid.v2.struct.Message;

public class PerioddatapacketHandler extends ChannelInboundHandlerAdapter {

    private final static Logger logger = Logger.getLogger(PerioddatapacketHandler.class);

    //private final ConcurrentLinkedQueue<JSONObject> storage;
    private final BlockingQueue<JSONObject> storage;

    public PerioddatapacketHandler(BlockingQueue<JSONObject> storage) {
        this.storage = storage;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final int parOffset = 5;
        Message message=(Message)msg;
        if(message.getMainCd()==(byte)0x30 && message.getSubCd()==(byte)0x02) {//接收长数据包
            byte[] par = message.getPar();
            // 取得设备号
            int machineID = ((par[7-parOffset] & 0xff) << 8) + par[8-parOffset] & 0xff;
            // 存储连接
            String skey ="knit_"+machineID;
            
            ChannelSession.putCh(skey, ctx.channel());
            
            ChannelSession.putAttr(skey, "MachineID", (machineID+"").getBytes());
            
            if(logger.isDebugEnabled()) {
              logger.info("[3002]recv period packet is ok, 取得纺织机"+machineID+",存储连接 ");
            }
            
            JSONObject jsonObj =  Mesmid2ndUtils.convertBytes2JSONObject(par);
            
            if(jsonObj != null) {
            	boolean b = storage.offer(jsonObj);
	            
	            if(!b){
	              final String logMsg = new StringBuffer(1500)
                  .append("消息队列v2已满[").append(storage.size()).append("]，忽略数据包:").append( "\r\n" )
                  .append(StringUtil.bytesToHex(message.composeFull())).append( "\r\n" )
                  .append(jsonObj.toString())
                  .toString();
      					ForwardingStrategy.warnDropPackMsg(logMsg);
	            }
            }
            
        	// 接收长数据包并且转换成功,响应
        	ctx.writeAndFlush(buildPerioddatapacketResp());//发送长数据包响应
        }
        else
            ctx.fireChannelRead(msg);
        
    }
    
    private Message buildPerioddatapacketResp() {
        Message message = new Message((byte)0x80, (byte)0x03, new byte[] {0x30,0x02});
        return message;
    }
}