package com.yzzao.mesmid.json.server;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.yzzao.common.utils.JsonUtils;
import com.yzzao.mesmid.json.codec.HttpObjectWrapRequest;
import com.yzzao.mesmid.json.codec.HttpObjectWrapResponse;
import com.yzzao.mesmid.json.pojo.MESOrder;
import com.yzzao.mesmid.json.pojo.MESOrderDetail;
import com.yzzao.mesmid.v2.serv.ChannelSession;
import com.yzzao.mesmid.v2.struct.Message;

/**
 * Created by carl.yu on 2016/12/16.
 */
public class HttpJsonServerHandler extends SimpleChannelInboundHandler<HttpObjectWrapRequest> {
    private final static Logger logger = Logger.getLogger(HttpJsonServerHandler.class);
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, HttpObjectWrapRequest msg) throws Exception {
        HttpRequest request = msg.getRequest();
        MESOrder order = (MESOrder) msg.getBody();
        logger.info("server recv mes http order : " + JsonUtils.objectToJson(order));
        
        dobusiness(order);
        
        ChannelFuture future = ctx.writeAndFlush(new HttpObjectWrapResponse(null, order));
        if (!HttpUtil.isKeepAlive(request)) {
            future.addListener(new GenericFutureListener<Future<? super Void>>() {
                public void operationComplete(Future<? super Void> future) throws Exception {
                    ctx.close();
                }
            });
        }
    }
    /*@Override //netty5.0
    protected void messageReceived(final ChannelHandlerContext ctx, HttpObjectWrapRequest msg) throws Exception {
        HttpRequest request = msg.getRequest();
        Order order = (Order) msg.getBody();
        System.out.println("Http server receive request : " + order);
        dobusiness(order);
        ChannelFuture future = ctx.writeAndFlush(new HttpObjectWrapResponse(null, order));
        if (!HttpHeaders.isKeepAlive(request)) {
            future.addListener(new GenericFutureListener<Future<? super Void>>() {
                public void operationComplete(Future future) throws Exception {
                    ctx.close();
                }
            });
        }
    }*/

    private void dobusiness(MESOrder order) {
        try {
            // 发送订单信息到下位机 0x20 0x02
            MESOrderDetail[] data = order.getData();
            for(final MESOrderDetail orderDtl:data){
                Channel ch = ChannelSession.channel(orderDtl.getMachineid());
                if(ch!=null &&ch.isActive() ) {
                    byte[] orddat = orderDtl.getBillno().getBytes("ascii");
                    byte[] neworddat = new byte[10];
                    if(orddat.length<neworddat.length) {
                        for(int i=0; i<neworddat.length-orddat.length;i++) {
                            neworddat[i]=0x00;
                        }
                        synchronized(this) {
                            System.arraycopy(orddat, 0, neworddat, neworddat.length-orddat.length, orddat.length);
                        }
                    }    
                    ByteBuf buf = Unpooled.copiedBuffer(new Message((byte)0x20,(byte)0x02, neworddat).composeFull());
                    ChannelFuture future= ch.writeAndFlush(buf);
                    future.addListener(new GenericFutureListener<Future<? super Void>>() {
                        public void operationComplete(Future<? super Void> future) throws Exception {
//                            orderDtl.setSendSuccess("1");
//                            orderDtl.setSendMessage("ok");
                        }
                    });
                    //TODO 发送 订单进度 & 工厂消息通知
                    //0x20 0x05 订单进度 OrderSchedule
                    //0x12 0x05 工厂消息通知 Notice

                }else{
//                    orderDtl.setSendSuccess("0");
//                    orderDtl.setSendMessage("machine offline, please tray again later. order detail: "+JsonUtils.objectToJson(orderDtl));
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("order data billNo -> ascii decode fail : " + JsonUtils.objectToJson(order));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private static void sendError(ChannelHandlerContext ctx,
                                  HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status, Unpooled.copiedBuffer("失败: " + status.toString()
                + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}