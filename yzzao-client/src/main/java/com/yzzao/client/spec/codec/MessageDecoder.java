package com.yzzao.client.spec.codec;

import java.io.IOException;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import com.yzzao.client.spec.handler.ProtocalVerHandler;
import com.yzzao.client.spec.struct.Guid;
import com.yzzao.client.spec.struct.Header;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.common.utils.StringUtil;

public final class MessageDecoder extends LengthFieldBasedFrameDecoder {
    private final static Logger logger = Logger.getLogger(MessageDecoder.class);
    /**
     * 
     * @param maxFrameLength 65535
     * @param lengthFieldOffset 38
     * @param lengthFieldLength 4
     * @throws IOException
     */
	public MessageDecoder(int maxFrameLength, int lengthFieldOffset,
			int lengthFieldLength) {
		super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
		
	}
	
	/**
	 * 把一个数据包解码成对象
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in)
			throws Exception {
		
		ByteBuf frame = (ByteBuf) super.decode(ctx, in);
		if (frame == null) {
		    return null;
		}
		
		Message message = new Message();
		byte[] keyArray = null;
		
		Header header = new Header();
		
		in.resetReaderIndex();
		
		// 通讯序号
		keyArray = new byte[2];
		in.readBytes(keyArray);
		header.setComSeqNo(StringUtil.bytesToHex(keyArray));
		
		// 时间戳
		keyArray = new byte[4];
		in.readBytes(keyArray);
		header.setTs(StringUtil.bytesToHex(keyArray));
		
		// 请求方IDc
		Guid reqGuid = new Guid();
		readGuid(reqGuid, in);
		header.setReqGuid(reqGuid);
		
		// 应答方IDc
		Guid respGuid = new Guid();
		readGuid(respGuid, in);
		header.setRespGuid(respGuid);
		
		// 信息内容总长度
		header.setLength(in.readInt());
		if(in.readableBytes() < header.getLength()){
            throw new Exception("消息不正确");
        }
		
		// 指令头
		message.setHeader(header);
		
		// 功能类型代码
		message.setFunTpNo(in.readByte());
		
		// 子功能类型代码
		message.setSubFunTpNo(in.readByte());
		
		// 参数（代码）
		keyArray = new byte[in.readableBytes()];
		in.readBytes(keyArray);
		String parCd = StringUtil.bytesToHex(keyArray);
		message.setParCd(parCd);
		
		if(logger.isInfoEnabled()) {
		    logger.info("client recv => " + StringUtil.bytesToHex(/*ByteBufUtil.getBytes(in, 0, in.readerIndex())*/Message.composeFull(message)));
		}
		return message;
	}

	/**
	 * 解码GUID
	 * @param guid
	 * @param in
	 */
	private void readGuid(final Guid guid, final ByteBuf in) {
		byte[] keyArray = null;
		// 厂商信息代码+设备类型代码+设备序列码 4Byte+3Byte+9Byte
		keyArray = new byte[4];
		in.readBytes(keyArray);
		guid.setMerNo(StringUtil.bytesToHex(keyArray));
		keyArray = new byte[3];
		in.readBytes(keyArray);
		guid.setDevTyNo(StringUtil.bytesToHex(keyArray));
		keyArray = new byte[9];
		in.readBytes(keyArray);
		guid.setDevSeqNo(StringUtil.bytesToHex(keyArray));
	}
	
	public static void main(String[] args) {
		System.out.println(StringUtil.bytesToHex(new byte[0]));
	}
	
}
