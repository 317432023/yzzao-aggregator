package com.yzzao.client.spec.handler;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.yzzao.client.QueueWithThreadPool;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.common.utils.StringUtil;

public class DataUpHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(DataUpHandler.class);
    
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		Message message = (Message)msg;

		if (message.getFunTpNo()==(byte)0x02 && message.getSubFunTpNo()==(byte)0x01) {//取得上报响应后的处理,检查是否上报成功
		    String resp = message.getParCd();
            if (!resp.equals("01")) {
                // 上报失败
                logger.error("Data up fail : " + StringUtil.bytesToHex(Message.composeFull(message)));
            } else {
                logger.info("Data up success : " + StringUtil.bytesToHex(Message.composeFull(message)));
            }
            //透传
            //ctx.fireChannelRead(msg);
		}else
			//透传
			ctx.fireChannelRead(msg);
		
	}


	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		super.exceptionCaught(ctx, cause);
	}

}
