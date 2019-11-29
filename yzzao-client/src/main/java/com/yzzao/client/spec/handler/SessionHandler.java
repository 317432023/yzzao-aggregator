package com.yzzao.client.spec.handler;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.StringUtil;

public class SessionHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(SessionHandler.class);

    private int gauge;
    
    public SessionHandler(int gauge) {
        super();
        this.gauge = gauge;
    }
    
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 当连接建立时发送会话请求
//	    logger.info("tcp/ip 连接建立");
//	    Message message = buildSessionReq(gauge);
//		ctx.writeAndFlush(message);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		//super.channelRead(ctx, msg);
		
		Message message = (Message)msg;
		
		if (message.getFunTpNo()==0x01 && message.getSubFunTpNo()==0x01) {//服务器应答(指令内容: 客户端 会话请求)
			String loginResult = message.getParCd();
			if (!loginResult.equals("03")) {
			    logger.error("Session is failure : " + StringUtil.bytesToHex(Message.composeFull(message)));
			    logger.info("Closing TCP/IP Connection...");
				ctx.close();
			} else {// 成功
                
				logger.info("Session is ok. ");
				ctx.fireChannelRead(msg);
			}
		} else if (message.getFunTpNo()==0x1f && message.getSubFunTpNo()==0x01) {//服务器请求(指令内容: 服务端 会话请求) 
		    ctx.writeAndFlush(buildSessionResp(message));
		} 
		else
			ctx.fireChannelRead(msg);
		
	}


	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		super.exceptionCaught(ctx, cause);
	}

	/**
	 * 00015b936bcd 10000001 018001 000000000000001001 10000001 000000 000000000000000001<br>
     * 00015b936bcd 10000001 018001 000000000000001001 10000001 010101 100000011000000101 00000002 1f 01
	 * @return
	 */
	public static Message buildSessionReq(int gauge) {

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
        message.setFunTpNo((byte)0xf1);
        message.setSubFunTpNo((byte)0x01);
        message.setParCd(null);
        return message;
	}
	public static Message buildSessionResp(Message reqMessage) {
        
        Message message = new Message();
        Header header = new Header();
        header.setComSeqNo(reqMessage.getHeader().getComSeqNo());
        header.setTs(reqMessage.getHeader().getTs());
        Guid reqGuid = new Guid();
        Guid respGuid = new Guid();
        reqGuid.setMerNo(OriknitUtils.merNoOfReq());
        reqGuid.setDevTyNo(OriknitUtils.devTyNoReq());
        reqGuid.setDevSeqNo(/*OriknitUtils.devSeqNoReq()*/reqMessage.getHeader().getRespGuid().getDevSeqNo());
        respGuid.setMerNo(reqMessage.getHeader().getReqGuid().getMerNo());
        respGuid.setDevTyNo(reqMessage.getHeader().getReqGuid().getDevTyNo());
        respGuid.setDevSeqNo(reqMessage.getHeader().getReqGuid().getDevSeqNo());
        header.setReqGuid(reqGuid);
        header.setRespGuid(respGuid);
        //header.setLength(length); 长度编码时确定
        message.setHeader(header);
        message.setFunTpNo((byte)0x10);
        message.setSubFunTpNo((byte)0x01);
        message.setParCd("03");
        return message;
	}
	
	public static void main(String[] args) {
	    byte b = (byte)0xf1;
	    byte[] bs = new byte[1];
	    bs[0] = b;
	    System.out.println(StringUtil.bytesToHex(bs));
	}
	
}
