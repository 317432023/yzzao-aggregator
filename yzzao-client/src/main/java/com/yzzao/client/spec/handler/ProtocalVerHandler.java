package com.yzzao.client.spec.handler;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import com.yzzao.client.Constants;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.StringUtil;

/**@deprecated*/
public class ProtocalVerHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(ProtocalVerHandler.class);
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		//super.channelRead(ctx, msg);
		
		Message recvMessage = (Message)msg;

		if (recvMessage.getFunTpNo()==0x01 && recvMessage.getSubFunTpNo()==0x03) {//服务器应答(指令内容: 客户端 连接密码请求)
		    
		    // 读会话
            AttributeKey<byte[]> attrKey = AttributeKey.valueOf("sessionId");
            Attribute<byte[]> attr = ctx.channel().attr(attrKey);
            byte[] mydata= attr.get();
            int gauge = StringUtil.bytesToInt2(mydata, 0);
		    
			ctx.writeAndFlush(buildProtocalVerReq(Constants.Ver, gauge));//客户端 发送协议版本信息
		}
		else if (recvMessage.getFunTpNo()==0x01 && recvMessage.getSubFunTpNo()==0x04) {//服务器应答(指令内容: 客户端 发送协议版本信息)
			String parCd = recvMessage.getParCd();
			if (!parCd.equals("01")) {
				// 协议失败，关闭连接
				logger.error("Send Protocal Ver Request err or failure : " + StringUtil.bytesToHex( Message.composeFull(recvMessage) ));
				logger.info("Closing TCP/IP Connection...");
				ctx.close();
			} else {
			    logger.info("Send Protocal Ver Request is ok.");
				//透传
				ctx.fireChannelRead(msg);
			}
		} 
		else if (recvMessage.getFunTpNo()==0x1f && recvMessage.getSubFunTpNo()==0x04) {//服务器请求(指令内容: 服务端 发送协议版本信息) 
		    String verhexstr = recvMessage.getParCd().substring(4);
            String ver = new String(StringUtil.hexToBytes(verhexstr), "UTF-8");
            Message respMessage = null;
            if(Constants.Ver.equals(ver))
                respMessage = buildProtocalVerResp(recvMessage, "01");//允许连接
            else {
                logger.warn("Server protocal version error : " + StringUtil.bytesToHex( Message.composeFull(recvMessage) ));
                respMessage = buildProtocalVerResp(recvMessage, "01");//TODO 协议版本错误,目前暂时允许连接,02 状态不对
            }
            ctx.writeAndFlush(respMessage);
        } 
		else
			//透传
			ctx.fireChannelRead(msg);
		
	}


	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		super.exceptionCaught(ctx, cause);
	}

	private Message buildProtocalVerReq(final String ver, int gauge) {
	    Message reqMessage = new Message();
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
        //header.setLength(length); 长度在编码时确定
        reqMessage.setHeader(header);
        reqMessage.setFunTpNo((byte)0xf1);
        reqMessage.setSubFunTpNo((byte)0x04);
        reqMessage.setParCd(OriknitUtils.str2lenHexstr(ver, null));
        return reqMessage;
	}
	
	private Message buildProtocalVerResp(Message recvMessage, final String parCd) {
        Message respMessage = new Message();
        Header header = new Header();
        header.setComSeqNo(recvMessage.getHeader().getComSeqNo());
        header.setTs(recvMessage.getHeader().getTs());
        Guid reqGuid = new Guid();
        Guid respGuid = new Guid();
        reqGuid.setMerNo(OriknitUtils.merNoOfReq());
        reqGuid.setDevTyNo(OriknitUtils.devTyNoReq());
        reqGuid.setDevSeqNo(/*OriknitUtils.devSeqNoReq()*/recvMessage.getHeader().getRespGuid().getDevSeqNo());
        respGuid.setMerNo(recvMessage.getHeader().getReqGuid().getMerNo());
        respGuid.setDevTyNo(recvMessage.getHeader().getReqGuid().getDevTyNo());
        respGuid.setDevSeqNo(recvMessage.getHeader().getReqGuid().getDevSeqNo());
        header.setReqGuid(reqGuid);
        header.setRespGuid(respGuid);
        //header.setLength(length); 长度在编码时确定
        respMessage.setHeader(header);
        respMessage.setFunTpNo((byte)0x10);
        respMessage.setSubFunTpNo((byte)0x04);
        respMessage.setParCd(parCd);
        return respMessage;
    }
}
