package com.yzzao.client.spec.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.yzzao.client.Constants;
import com.yzzao.client.SenderTask;
import com.yzzao.client.spec.codec.MessageDecoder;
import com.yzzao.client.spec.codec.MessageEncoder;
import com.yzzao.client.spec.handler.DataQryHandler;
import com.yzzao.client.spec.handler.DataSubscriptHandler;
import com.yzzao.client.spec.handler.DataUpHandler;
import com.yzzao.client.spec.handler.HeartBeatHandler;
import com.yzzao.client.spec.handler.LoginAuthHandler;
import com.yzzao.client.spec.handler.LoginHandler;
import com.yzzao.client.spec.handler.SessionHandler;
import com.yzzao.client.spec.struct.Message;
import com.yzzao.client.spec.util.OriknitUtils;
import com.yzzao.common.utils.StringUtil;

public class OriknitClient {
    private final static Logger logger = Logger.getLogger(OriknitClient.class);
    
    volatile ChannelFuture future;
    
    public ChannelFuture getFuture() {
        return future;
    }
/*
    private final int gauge;
    public OriknitClient(int gauge) {this.gauge=gauge;}*/
//    
//    private ScheduledExecutorService executor = Executors
//            .newScheduledThreadPool(1);
    
    EventLoopGroup group = new NioEventLoopGroup();
/*
 * private volatile Map<Integer, Long> timer;
    private volatile Map<Integer, OriknitClient> specClientMap;
 * */
    public void connect(int port, String host, final int gauge, final  Map<Integer, OriknitClient>  conns, final Map<Integer,Map<String,String>> realMap, Map<Integer, Long> timer)  {
        
        // 配置客户端NIO线程组

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            
                            // 1入站 解码器
                            ch.pipeline().addLast(new MessageDecoder(65535000, 38, 4));

                            //  出站 outbound 编码器
                            ch.pipeline().addLast(new MessageEncoder());

                            // 2 入站 inbound 超时处理
//                            ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(10, 0, 0));
                            // 2 入站 inbound 超时处理
//                            ch.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler(10));//10秒超时

                            // 3 入站 inbound 会话请求
                            ch.pipeline().addLast("SessionHandler", new SessionHandler(gauge));

                            // 4 入站 inbound 发送连接码
                            ch.pipeline().addLast("LoginHandler", new LoginHandler(gauge));

                            // 5 入站 inbound 连接密码请求
                            ch.pipeline().addLast("LoginAuthHandler", new LoginAuthHandler(gauge));

                            // 6 入站 inbound 发送协议版本信息
                            //ch.pipeline().addLast("ProtocalVerHandler", new ProtocalVerHandler());

                            // 7 入站 inbound 识别数据上报结果
                            ch.pipeline().addLast("DataUpHandler", new DataUpHandler());

                            // 8 入站 inbound 数据查询
                            ch.pipeline().addLast("DataQryHandler", new DataQryHandler(gauge, realMap));
                            
                            // 8 入站 inbound 数据订阅
                            ch.pipeline().addLast("DataSubscriptHandler", new DataSubscriptHandler(gauge));
                            
                            // x 入站 inbound 心跳保持
                            ch.pipeline().addLast("HeartBeatHandler", new HeartBeatHandler(gauge, conns, realMap));
                        }
                    });
            
            // 发起异步连接操作
            //future = bootstrap.connect(new InetSocketAddress(host, port)).sync();
            future = bootstrap.connect(host, port).sync();
            
            if(future.isSuccess()) {
                System.out.println("连接服务成功["+StringUtil.bytesToHex(StringUtil.intToBytes2(gauge))+"]");
            }
            
            //TimeUnit.MILLISECONDS.sleep(2000);
            logger.info("tcp/ip 连接建立");
            byte[] messda = Message.composeFull(SessionHandler.buildSessionReq(gauge));
            ByteBuf buf = Unpooled.copiedBuffer(messda);
            future.channel().writeAndFlush(buf);
            logger.info("client send=>"+StringUtil.bytesToHex(messda));
            
            future.channel().closeFuture().sync();
            
        } catch(Exception e){
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();  //这里不再是优雅关闭了
            logger.info("连接关闭["+StringUtil.bytesToHex(StringUtil.intToBytes2(gauge))+"]");
            conns.remove(gauge);//20181205
            timer.remove(gauge);//20181205
//            future = null;
//            // 所有资源释放完成之后，清空资源，再次发起重连操作
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        TimeUnit.SECONDS.sleep(5);
//                        try {
//                            connect(Constants.RemotePort, Constants.RemoteIP, gauge, conns, realMap);// 发起重连操作
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
            
        }
    }

    public boolean sendMessage(Message message) throws Exception {
        
        if(future != null) {
            Channel channel = future.channel();
            if(channel.isActive() ) {
                
                // 读验证会话
                AttributeKey<byte[]> attrKey = AttributeKey.valueOf("authId");
                Attribute<byte[]> attr = channel.attr(attrKey);
                byte[] mydata= attr.get();
                logger.debug("get authId about gauge =>"+StringUtil.bytesToHex(mydata));
                // 已经登录才能发送数据包
                byte[] messda = Message.composeFull(message);
                if(mydata != null) {
                    ByteBuf buf = Unpooled.copiedBuffer(messda);
                    channel.writeAndFlush(buf);
                    //ReferenceCountUtil.release(buf);
                    if(logger.isInfoEnabled()) {
                        logger.info("client transfer => " + StringUtil.bytesToHex(messda));
                    }
                    return true;
                }else{
                    if(logger.isInfoEnabled()) {
                        logger.error("session id is not exists. =>" + StringUtil.bytesToHex(messda));
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //OriknitClient cli = new OriknitClient();
        //cli.connect(Constants.RemotePort, Constants.RemoteIP, 0x1004, new ConcurrentHashMap<Integer, OriknitClient>(),new ConcurrentHashMap<Integer, Map<String, String>>());
        //System.exit(0);
        //100300010012e5a8b4e6ac90e79d99e9909ee59797e4bc90
        String hexval = OriknitUtils.str2lenHexstr("浙江理工", "utf8");
        String s= new String(StringUtil.hexToBytes("e6b599e6b19fe79086e5b7a5"),"utf8");
        System.out.println(s);
    }
    
}
