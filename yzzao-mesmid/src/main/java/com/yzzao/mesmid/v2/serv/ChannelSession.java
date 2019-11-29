package com.yzzao.mesmid.v2.serv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.StringUtil;

public class ChannelSession {
    private static final Logger logger = Logger.getLogger(ChannelSession.class);

    private volatile static Map<String, Channel> session = new ConcurrentHashMap<>();

    public static Channel channel(final String sessionId) {
        return session.get(sessionId);
    }

    public static void putCh(final String sessionId, Channel ch) {
        session.put(sessionId, ch);
    }

    public static Channel removeCh(final String sessionId) {
        return session.remove(sessionId);
    }

    public static void removeCh(Channel ch) {
      for(String s:session.keySet())
      {
        if(session.get(s) == ch || session.get(s).equals(ch)) {
          session.remove(s);
          break;
        }
      }
    }
    /**
     * 
     * @param sessionId
     *            会话id,一般使用machineID+""的字符串化
     * @param attrKey
     *            属性名称,比如"machineID"
     * @param value
     *            属性值,比如(machineID+""的字符串化).getBytes()
     * @return
     */
    public static boolean putAttr(final String sessionId, final String attrKey, byte[] value) {
        Channel ch = channel(sessionId);
        if (ch == null)
            return false;
        if (!ch.isOpen() || !ch.isActive())
            return false;

        return putAttr(ch, attrKey, value);
    }

    /**
     * 
     * @param attrKey
     *            属性名称,比如"machineID"
     * @param value
     *            属性值,比如(machineID+""的字符串化).getBytes()
     * @return
     */
    public static boolean putAttr(Channel ch, final String attrKey, byte[] value) {

        // 往channel中写入属性
        AttributeKey<byte[]> srcdataAttrKey = AttributeKey.valueOf(attrKey);
        Attribute<byte[]> srcdataAttr = ch.attr(srcdataAttrKey);
        srcdataAttr.set(value);
        return true;
    }

    /**
     * 
     * @param sessionId
     *            会话id,一般使用machineID+""的字符串化
     * @param attrKey
     *            属性名称,比如"machineID"
     * @return
     */
    public static byte[] getAttr(final String sessionId, final String attrKey) {
        Channel ch = channel(sessionId);
        if (ch == null)
            return null;
        if (!ch.isOpen() || !ch.isActive())
            return null;
        return getAttr(ch, attrKey);
    }

    /**
     * 
     * @param attrKey
     *            属性名称,比如"machineID"
     * @return
     */
    public static byte[] getAttr(Channel ch, final String attrKey) {

        AttributeKey<byte[]> ak = AttributeKey.valueOf(attrKey);
        Attribute<byte[]> attr = ch.attr(ak);
        byte[] mydata = attr.get();
        return mydata;
    }

    public static boolean sendMessage(final String sessionId, byte[] messda) {
        Channel channel = channel(sessionId);
        if(channel == null ) {
            logger.error("设备"+sessionId+"未连接或已掉线");
            return false;
        }
        
        if (channel.isActive()) {

//            // 读验证会话
//            AttributeKey<byte[]> attrKey = AttributeKey.valueOf("authId");
//            Attribute<byte[]> attr = channel.attr(attrKey);
//            byte[] mydata = attr.get();
//            logger.debug("get authId=>" + StringUtil.bytesToHex(mydata));
//            if (mydata == null) {
//                logger.error("session id is not exists.");
//                return false;
//            }
            // 已经登录才能发送数据包
            ByteBuf buf = Unpooled.copiedBuffer(messda);
            channel.writeAndFlush(buf);
            logger.info("serv transfer => " + StringUtil.bytesToHex(messda));
            return true;
        }
        return false;
    }
}
