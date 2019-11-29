package com.yzzao.mesmid.v2.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

import org.apache.log4j.Logger;

import com.yzzao.mesmid.v2.struct.Message;

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
  protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {

    if (msg == null)
      throw new Exception("The encode message is null");
    if (msg.getLength() != msg.getPar().length)
      throw new Exception("The encode message length error");

    out.writeBytes(new byte[] { msg.getFrameHead() });
    out.writeShort(msg.getLength());
    out.writeBytes(new byte[] { msg.getMainCd() });
    out.writeBytes(new byte[] { msg.getSubCd() });
    out.writeBytes(msg.getPar());
    out.writeShort(msg.getVerify());
    out.writeShort(msg.getFrameTail());

    if(logger.isDebugEnabled()) {
      logger.debug("server send =>" + msg.hexComposeFull());
    }
  }

}
