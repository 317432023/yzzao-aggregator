package com.yzzao.client.spec.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.yzzao.client.spec.client.OriknitClient;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.StringUtil;

public class HeartBeatHandler extends /*ChannelHandlerAdapter*/ChannelInboundHandlerAdapter  {
    private final static Logger logger = Logger.getLogger(HeartBeatHandler.class);
    private int gauge;
    private Map<Integer, OriknitClient> conns;
    private volatile Map<Integer, Map<String, String>> realMap;
    public HeartBeatHandler(int gauge, Map<Integer, OriknitClient>  conns,Map<Integer, Map<String, String>> realMap) {
        super();
        this.gauge = gauge;
        this.conns = conns;
        this.realMap = realMap;
    }
    
    private volatile ScheduledFuture<?> heartBeat;
    
    //客户端超时次数   
    private Map<ChannelHandlerContext,Integer> clientOvertimeMap = new ConcurrentHashMap<>();
    private final int MAX_OVERTIME  = 1;  //超时次数超过该值则注销连接

    private void addUserOvertime(ChannelHandlerContext ctx) {
        int oldTimes = 0;
        if (clientOvertimeMap.containsKey(ctx)) {
            oldTimes = clientOvertimeMap.get(ctx);
        }
        clientOvertimeMap.put(ctx, oldTimes + 1);
    }

	@Override  
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {  
	    
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {  
            IdleStateEvent event = (IdleStateEvent) evt;  
            if (event.state() == IdleState.READER_IDLE) {
                Integer overtimeTimes = clientOvertimeMap.get(ctx);
                if(overtimeTimes==null || overtimeTimes < MAX_OVERTIME){
                    logger.info("第"+(overtimeTimes==null?1:(overtimeTimes+1))+"次读超时,发送心跳");
                    
                    ctx.writeAndFlush(buildHeatBeatReq(gauge));
                    addUserOvertime(ctx);
                }else{
                    logger.error("服务端无响应,Closing TCP/IP Connection...");
                    //读取属性
                    AttributeKey<byte[]> ak = AttributeKey.valueOf("authId");
                    Attribute<byte[]> attr = ctx.channel().attr(ak);
                    byte[] mydata = attr.get();
                    if(mydata!=null) {
                        int gauge = StringUtil.bytesToInt2(mydata, 0);
                        if(gauge!=this.gauge) {logger.warn("gauge!=this.gauge, gauge=>"+gauge+",this.gauge=>"+this.gauge);}
                    }
                    //this.conns.remove(this.gauge);
                    ctx.close();
                }

            } else if (event.state() == IdleState.WRITER_IDLE) {
                logger.info("write 空闲");
            }  else if (event.state() == IdleState.ALL_IDLE) {
                logger.info("ALL_IDLE 空闲");
            }
        }
    }
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	    logger.error("关闭连接["+StringUtil.bytesToHex(StringUtil.intToBytes2(gauge))+"]");
        // 客户端下线或者强制退出等任何情况都触发,把连接从服务器端连接集合中删除
//        byte[] data = ChannelSession.getAttr(ctx.channel(), "MachineID");
//        if(data!=null) {
//            String sessionId = new String(data);
//            ChannelSession.removeCh("mobile_"+sessionId);
//            ChannelSession.removeCh("knit_"+sessionId);
//        }
        //读取属性
        AttributeKey<byte[]> ak = AttributeKey.valueOf("authId");
        Attribute<byte[]> attr = ctx.channel().attr(ak);
        byte[] mydata = attr.get();
        if(mydata!=null) {
            int gauge = StringUtil.bytesToInt2(mydata, 0);
            if(gauge!=this.gauge) {logger.warn("gauge!=this.gauge, gauge=>"+gauge+",this.gauge=>"+this.gauge);}
        }
        //this.conns.remove(this.gauge);
        super.channelInactive(ctx);
    }
    @Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		//super.channelRead(ctx, msg);
        
        // 清空超时次数
        clientOvertimeMap.clear();
        
		Message recvMessage = (Message)msg;
		
		if (recvMessage.getFunTpNo()==0x01 && (recvMessage.getSubFunTpNo()==0x03||recvMessage.getSubFunTpNo()==0x04)) {//服务器应答(指令内容: 客户端 连接密码请求 或 发送协议版本信息),发送心跳保持 见 userEventTriggered
			/*heartBeat = ctx.executor().scheduleAtFixedRate(new HeartBeatTask(ctx), 0, 5000,
					TimeUnit.MILLISECONDS);*/
		}
		else if(recvMessage.getFunTpNo()==0x01 && recvMessage.getSubFunTpNo()==0x05) {//取得服务端心跳响应后, 不做任何事
			//System.out.println("Client receive server heart beat response : ---> " + message);
		    
		}
		else if(recvMessage.getFunTpNo()==0x1f && recvMessage.getSubFunTpNo()==0x05) {//取得服务端心跳请求后,发送心跳响应
            //System.out.println("Client receive server heart beat request : ---> " + message);
		    Message respMessage = buildResp(recvMessage, (byte)0x10, (byte)0x05, null);
            ctx.writeAndFlush(respMessage);
        }
		//1211 服务器 断开请求
		else if(recvMessage.getFunTpNo()==0x1f && recvMessage.getSubFunTpNo()==0x06) {//取得服务端断开请求后,发送响应
            Message respMessage = buildResp(recvMessage, (byte)0x10, (byte)0x06, "05");
            ctx.writeAndFlush(respMessage);
        }
		// 1211 服务器 会话三类指令错误
		else if(recvMessage.getFunTpNo()==0x1f && recvMessage.getSubFunTpNo()==0x07) {//取得服务端断开请求后,发送响应
            Message respMessage = buildResp(recvMessage, (byte)0x10, (byte)0x07, "01");
            ctx.writeAndFlush(respMessage);
        }
		//1211 服务器实时状态查询
        else if(recvMessage.getFunTpNo()==0x2f && recvMessage.getSubFunTpNo()==0x03) {//
            String state = null;//默认关机
            if(realMap.get(gauge)!=null) {state = realMap.get(gauge).get("20010002");}
            Message respMessage = buildResp(recvMessage, (byte)0x20, (byte)0x03, state==null?"01":state);
            ctx.writeAndFlush(respMessage);
        }
		//1211 服务器历史状态查询
        else if(recvMessage.getFunTpNo()==0x2f && recvMessage.getSubFunTpNo()==0x04) {//
            String state = null;
            Message respMessage = buildResp(recvMessage, (byte)0x20, (byte)0x04, state==null?"06":(OriknitUtils.hexTs(new Date())+state));
            ctx.writeAndFlush(respMessage);
        }
		//1212 服务器报警查询
        else if(recvMessage.getFunTpNo()==0x2f && recvMessage.getSubFunTpNo()==0x05) {//
            String code = null;
            Message respMessage = buildResp(recvMessage, (byte)0x20, (byte)0x05, code==null?"06":(OriknitUtils.hexTs(new Date())+code));
            ctx.writeAndFlush(respMessage);
        }
		//1212 参数设置
        else if(recvMessage.getFunTpNo()==0x3f && recvMessage.getSubFunTpNo()==0x01) {//
            Message respMessage = buildResp(recvMessage, (byte)0x30, (byte)0x01, "06");
            ctx.writeAndFlush(respMessage);
        }
		//1212 状态设置
        else if(recvMessage.getFunTpNo()==0x3f && recvMessage.getSubFunTpNo()==0x02) {//
            Message respMessage = buildResp(recvMessage, (byte)0x30, (byte)0x02, "06");
            ctx.writeAndFlush(respMessage);
        }
		//1212 文件传输
        else if(recvMessage.getFunTpNo()==0x4f && (recvMessage.getSubFunTpNo()>=0x01 && recvMessage.getSubFunTpNo()<=0x05)) {//
            Message respMessage = buildResp(recvMessage, (byte)0x40, recvMessage.getSubFunTpNo(), "04");
            ctx.writeAndFlush(respMessage);
        }
        //1212 针织电控系统管理               5f
        else if(recvMessage.getFunTpNo()==0x5f && (recvMessage.getSubFunTpNo()>=0x01 && recvMessage.getSubFunTpNo()<=0x03)) {//
            Message respMessage = buildResp(recvMessage, (byte)0x50, recvMessage.getSubFunTpNo(), "04");
            ctx.writeAndFlush(respMessage);
        }
		else 
			//透传
			ctx.fireChannelRead(msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		//super.exceptionCaught(ctx, cause);
		cause.printStackTrace();
		if (heartBeat != null) {
			heartBeat.cancel(true);
			heartBeat = null;
		}
		ctx.fireExceptionCaught(cause);
	}

	private class HeartBeatTask implements Runnable {
		private final ChannelHandlerContext ctx;

		public HeartBeatTask(final ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void run() {
			Message heartBeatMessage = buildHeatBeatReq(gauge);
			logger.info("Client send heart beat messsage to server.");
			ctx.writeAndFlush(heartBeatMessage);
		}

	}
	
	private Message buildHeatBeatReq(int gauge) {
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
        //header.setLength(length); 长度在编码时确定
        message.setHeader(header);
        message.setFunTpNo((byte)0xf1);
        message.setSubFunTpNo((byte)0x05);
        message.setParCd(null);
        
        return message;
    }
	
	private Message buildCommonResp(Message recvMessage) {
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
        return respMessage;
	}
	
    private Message buildResp(Message recvMessage, byte funTpNo, byte subFunTpNo, String parCd) {
        Message respMessage = buildCommonResp(recvMessage);
        respMessage.setFunTpNo(funTpNo);
        respMessage.setSubFunTpNo(subFunTpNo);
        respMessage.setParCd(parCd);
        return respMessage;
    }
}
