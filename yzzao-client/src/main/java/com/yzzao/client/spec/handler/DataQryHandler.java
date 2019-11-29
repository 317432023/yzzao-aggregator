package com.yzzao.client.spec.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.yzzao.client.Constants;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;

public class DataQryHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter {
    private final static Logger logger = Logger.getLogger(DataQryHandler.class);
    private int gauge;
    private volatile Map<Integer, Map<String, String>> realMap;
    public DataQryHandler(int gauge, Map<Integer, Map<String, String>> realMap) {
        super();
        this.gauge = gauge;
        this.realMap = realMap;
    }
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		Message message = (Message)msg;
		
		// 服务器实时参数查询
		if (message.getFunTpNo()==(byte)0x2f && message.getSubFunTpNo()==(byte)0x01) {
		    String req = message.getParCd();
		    String resp= null;
		    if(realMap.get(gauge)!=null) {
		        resp = realMap.get(gauge).get(req);
		        if(resp==null) resp = Constants.parMap.get(req);
		    }
		    Message dataSubcriptResp = buildDataRealQryResp(message,resp);
            ctx.writeAndFlush(dataSubcriptResp);
		}
		// 服务器历史参数查询
		else if (message.getFunTpNo()==(byte)0x2f && message.getSubFunTpNo()==(byte)0x02) {
            //String req = message.getParCd();
            Message dataSubcriptResp = buildDataHisQryResp(message,null);
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
	private Message buildDataRealQryResp(Message recvMessage, final String parcd) {
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
        respMessage.setSubFunTpNo((byte)0x01);
        if(parcd==null) {
            respMessage.setParCd("06");
        }else{
            respMessage.setParCd(parcd);
        }
        return respMessage;
	}
    private Message buildDataHisQryResp(Message recvMessage, final String parcd) {
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
        respMessage.setSubFunTpNo((byte)0x02);
        respMessage.setParCd(parcd==null?"06":OriknitUtils.hexTs(new Date())+parcd);
        return respMessage;
    }
}
