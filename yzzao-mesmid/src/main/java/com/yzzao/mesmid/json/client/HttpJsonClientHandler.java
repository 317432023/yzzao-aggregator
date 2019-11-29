package com.yzzao.mesmid.json.client;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.yzzao.mesmid.json.codec.HttpObjectWrapRequest;
import com.yzzao.mesmid.json.pojo.OrderFactory;


/**
 * Created by carl.yu on 2016/12/16.
 */
public class HttpJsonClientHandler extends ChannelInboundHandlerAdapter/*SimpleChannelInboundHandler<HttpObjectWrapResponse>*/ {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接上服务器...");
        HttpObjectWrapRequest request = new HttpObjectWrapRequest(null, OrderFactory.createmes());
        ctx.writeAndFlush(request);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(msg.getClass().getName());
        System.out.println("接收到了数据..." + msg);
    }

    /*protected void channelRead0(ChannelHandlerContext ctx, HttpObjectWrapResponse msg) throws Exception {
        System.out.println("The client receive response of http header is : "
                + msg.getHttpResponse().headers().names());
        System.out.println("The client receive response of http body is : "
                + msg.getResult());
    }*/


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /*@Override //netty5.0
    protected void messageReceived(ChannelHandlerContext ctx, HttpObjectWrapResponse msg) throws Exception {
        System.out.println("The client receive response of http header is : "
                + msg.getHttpResponse().headers().names());
            HttpHeaders headers = msg.getHttpResponse().headers();
            for(String hn: headers.names()) {
                System.out.println(headers.get(hn));
            }
            System.out.println("The client receive response of http body is : "
                + msg.getResult());
        
    }*/
}