package com.yzzao.client.spec.codec;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.common.utils.StringUtil;

public final class MessageEncoder extends MessageToByteEncoder<Message> {
    private final static Logger logger = Logger.getLogger(MessageEncoder.class);
	@Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        System.out.println("tcp/ip closing...");
        super.close(ctx, promise);
    }

    /**
	 * 把一个对象编码成数据包
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out)
			throws Exception {
		
		if (msg == null || msg.getHeader() == null)
		    throw new Exception("The encode message is null");
		
		// 指令头
		Header header = msg.getHeader();
		
		// 通讯序号
		String comSeqNo = header.getComSeqNo();
		out.writeBytes(StringUtil.hexToBytes(comSeqNo));
		
		// 时间戳
		String ts = header.getTs();
		out.writeBytes(StringUtil.hexToBytes(ts));

		// 请求方IDc
		Guid reqGuid = header.getReqGuid();
		writeGuid(out, reqGuid);
		
		// 应答方IDc
		Guid respGuid  = header.getRespGuid();
		writeGuid(out, respGuid);
		
		// 信息内容总长度(在计算完后面的参数代码长度后确定)
//		int length = header.getLength();
		int length = msg.getParCd()!=null ?(2+msg.getParCd().length()/2):2;
		out.writeInt(length);
		
		// 功能类型代码
		out.writeByte(msg.getFunTpNo());
		// 子功能类型代码
		out.writeByte(msg.getSubFunTpNo());
		
		// 参数（代码）可变长度n
		if(msg.getParCd() != null)
			out.writeBytes(StringUtil.hexToBytes(msg.getParCd()));
		
		if(logger.isInfoEnabled())
		{
		    logger.info("client send =>" + StringUtil.bytesToHex(Message.composeFull(msg)));
		}
		
	}
	
	/**
	 * 编码IDc
	 * @param out
	 * @param guid
	 */
	private void writeGuid(final ByteBuf out, final Guid guid) {
		String merNo = guid.getMerNo();
		String  devTyNo = guid.getDevTyNo();
		String devSeqNo = guid.getDevSeqNo();
		out.writeBytes(StringUtil.hexToBytes(merNo));
		out.writeBytes(StringUtil.hexToBytes(devTyNo));
		out.writeBytes(StringUtil.hexToBytes(devSeqNo));
	}

}
