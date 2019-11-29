package com.yzzao.client.spec.handler;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.Attribute;
import io.netty.util.DefaultAttributeMap;

import com.yzzao.client.Constants;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.StringUtil;

public class LoginAuthHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(LoginAuthHandler.class);
    private int gauge;
    
    public LoginAuthHandler(int gauge) {
        super();
        this.gauge = gauge;
    }
    @Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		Message message = (Message)msg;

		if (message.getFunTpNo()==0x01 && message.getSubFunTpNo()==0x02) {//服务器应答(指令内容: 客户端 发送连接码)
		    
			ctx.writeAndFlush(buildLoginAuthReq(gauge));
		}
		else if (message.getFunTpNo()==0x01 && message.getSubFunTpNo()==0x03) {//服务器应答(指令内容: 客户端 连接密码请求)
		    String passwdhex = message.getParCd().substring(4);
            String passwd = new String(StringUtil.hexToBytes(passwdhex), "UTF-8");
            if(Constants.Passwd.equals(passwd)) {
                // 写入验证会话
                AttributeKey<byte[]> srcdataAttrKey = AttributeKey.valueOf("authId");
                byte[] mydata= StringUtil.hexToBytes(message.getHeader().getRespGuid().getDevSeqNo().substring(10));
                Attribute<byte[]> srcdataAttr = ctx.channel().attr(srcdataAttrKey);
                srcdataAttr.set(mydata);
                logger.info("Login Auth is ok. ");
                //透传
                ctx.fireChannelRead(msg);
            }else {
                // 握手失败，关闭连接
                logger.error("Login Auth server response passwd uncorrect : " + passwd);
                logger.info("Closing TCP/IP Connection...");
                ctx.close();
            }
		}
		else if (message.getFunTpNo()==0x1f && message.getSubFunTpNo()==0x03) {//服务器请求(指令内容: 服务端 连接密码请求) 
            Message respMessage = buildLoginAuthResp(message);
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

	private Message buildLoginAuthReq(int gauge) {
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
        reqMessage.setSubFunTpNo((byte)0x03);
        reqMessage.setParCd(null);
        
        return reqMessage;
	}
	
	private Message buildLoginAuthResp(Message recvMessage) {
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
        respMessage.setSubFunTpNo((byte)0x03);
        respMessage.setParCd(OriknitUtils.str2lenHexstr(Constants.Passwd, null));
        return respMessage;
    }
}
