package com.yzzao.mesmid.v2.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.StringUtil;
import com.yzzao.mesmid.v2.struct.Message;

public final class MessageDecoder extends LengthFieldBasedFrameDecoder {
  private final static Logger logger = Logger.getLogger(MessageDecoder.class);

  /**
   * 
   * @param maxFrameLength
   *          0x7FFF
   * @param lengthFieldOffset
   *          1
   * @param lengthFieldLength
   *          2
   * @param lengthAdjustment
   *          4
   * @param initialBytesToStrip
   *          0
   * @throws IOException
   */
  public MessageDecoder(int maxFrameLength,// 0x7fff
      int lengthFieldOffset, // 1
      int lengthFieldLength,// 2
      int lengthAdjustment,// 4
      int initialBytesToStrip// 0
  ) {
    super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
  }

  /**
   * 把一个数据包解码成对象
   */
  @Override
  protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

    ByteBuf frame = (ByteBuf) super.decode(ctx, in);
    if (frame == null) {
      return null;
    }
    /**
     * private byte frameHead;//1byte 
     * private short length;//2bytes 
     * private byte mainCd;//1byte 
     * private byte subCd;//1byte 
     * private byte[] par;//nbyte 
     * private short verify;//2byte 
     * private short frameTail;//2byte
     */

    Message message = new Message();
    byte[] keyArray = null;

    in.resetReaderIndex();

    keyArray = new byte[1];
    in.readBytes(keyArray);
    message.setFrameHead(keyArray[0]);

    keyArray = new byte[2];
    in.readBytes(keyArray);
    message.setLength((short) StringUtil.bytesToInt2(new byte[] { 0x00, 0x00, keyArray[0], keyArray[1] }, 0));

    keyArray = new byte[1];
    in.readBytes(keyArray);
    message.setMainCd(keyArray[0]);

    keyArray = new byte[1];
    in.readBytes(keyArray);
    message.setSubCd(keyArray[0]);

    keyArray = new byte[message.getLength()];
    in.readBytes(keyArray);
    message.setPar(keyArray);

    keyArray = new byte[2];
    in.readBytes(keyArray);
    message.setVerify((short) StringUtil.bytesToInt2(new byte[] { 0x00, 0x00, keyArray[0], keyArray[1] }, 0));

    keyArray = new byte[2];
    in.readBytes(keyArray);
    message.setFrameTail((short) StringUtil.bytesToInt2(new byte[] { 0x00, 0x00, keyArray[0], keyArray[1] }, 0));

    if(logger.isDebugEnabled()) {
      logger.debug("server recv => " + message.hexComposeFull());
    }
    
    ReferenceCountUtil.release(frame);// 20190315

    return message;
  }

}
