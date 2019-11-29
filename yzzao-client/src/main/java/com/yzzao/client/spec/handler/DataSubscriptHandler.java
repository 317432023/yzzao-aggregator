package com.yzzao.client.spec.handler;

import org.apache.log4j.Logger;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.yzzao.client.Constants;
import com.yzzao.client.QueueWithThreadPool;
import com.yzzao.client.spec.client.OriknitClient;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.StringUtil;

public class DataSubscriptHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(DataSubscriptHandler.class);
    private int gauge;
    public DataSubscriptHandler(int gauge) {
        super();
        this.gauge = gauge;
    }
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		Message message = (Message)msg;

		if (message.getFunTpNo()==(byte)0x2f && message.getSubFunTpNo()==(byte)0x06) {
		    String req = message.getParCd();
		    Message dataSubcriptResp = buildDataSubscriptResp(message,"06");
            ctx.writeAndFlush(dataSubcriptResp);
		}else if (message.getFunTpNo()==(byte)0x2f && message.getSubFunTpNo()==(byte)0x07) {
            String req = message.getParCd();
            Message dataSubcriptResp = buildDataSubscriptEndResp(message,"06");
            ctx.writeAndFlush(dataSubcriptResp);
        }else
			//透传
			ctx.fireChannelRead(msg);
		
	}


	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		super.exceptionCaught(ctx, cause);
	}
	private Message buildDataSubscriptResp(Message recvMessage, final String parcd) {
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
        respMessage.setFunTpNo((byte)0x20);
        respMessage.setSubFunTpNo((byte)0x06);
        respMessage.setParCd(parcd);
        return respMessage;
	}
    private Message buildDataSubscriptEndResp(Message recvMessage, final String parcd) {
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
        respMessage.setFunTpNo((byte)0x20);
        respMessage.setSubFunTpNo((byte)0x07);
        respMessage.setParCd(parcd);
        return respMessage;
    }
}
