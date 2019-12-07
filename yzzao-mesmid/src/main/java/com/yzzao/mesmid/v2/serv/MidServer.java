package com.yzzao.mesmid.v2.serv;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.json.JSONObject;

import com.yzzao.mesmid.Constants;
import com.yzzao.mesmid.v2.codec.MessageDecoder;
import com.yzzao.mesmid.v2.codec.MessageEncoder;
import com.yzzao.mesmid.v2.handler.CmdHandler;
import com.yzzao.mesmid.v2.handler.HandshakeHandler;
import com.yzzao.mesmid.v2.handler.PerioddatapacketHandler;

public class MidServer {
  /** 并发队列，用于存放数据包转成的Json对象 */
  // private final ConcurrentLinkedQueue<JSONObject> storage;
  private final BlockingQueue<JSONObject> storage;
  private final ConcurrentLinkedQueue<String> mobileScanToFileQueue;
  private final Map<Integer, String> barcodeCardNo;
  /** 机台-crc 映射*/
  private final Map<Integer, Short> machineCrcMap;
  public MidServer(BlockingQueue<JSONObject> storage, /*Map<String, Long> lastPacktimeMap,*/ ConcurrentLinkedQueue<String> mobileScanToFileQueue, Map<Integer, String> barcodeCardNo,  Map<Integer, Short> machineCrcMap) {
    this.storage = storage;
    this.mobileScanToFileQueue = mobileScanToFileQueue;
    this.barcodeCardNo = barcodeCardNo;
    this.machineCrcMap = machineCrcMap;
  }

  public void bind() throws Exception {
    // 配置服务端的NIO线程组
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws IOException {
            ch.pipeline().addLast("messageDecoder", new MessageDecoder(0x7FFF, 1, 2, 4, 0));
            ch.pipeline().addLast("messageEecoder", new MessageEncoder());
            ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(15, 0, 0));// 15秒没读取到,
                                                                                      // 认为超时触发一次心跳
            ch.pipeline().addLast("handshakeHandler", new HandshakeHandler(mobileScanToFileQueue, barcodeCardNo));// 握手指令,可以认为是心跳检测
            ch.pipeline().addLast("perioddatapacketHandler", new PerioddatapacketHandler(storage, machineCrcMap));// 接收周期性二代长数据包
            ch.pipeline().addLast("cmdHandler", new CmdHandler());

          }
        });

    // 绑定端口，同步等待成功
    bootstrap.bind(Constants.ServIP, Constants.TCPServPort).sync();
  }
}
