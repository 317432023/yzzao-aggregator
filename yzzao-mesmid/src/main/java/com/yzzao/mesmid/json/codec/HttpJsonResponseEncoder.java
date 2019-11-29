package com.yzzao.mesmid.json.codec;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;

import java.util.List;
/**
 * Created by carl.yu on 2016/12/16.
 */
public class HttpJsonResponseEncoder extends AbstractHttpJsonEncoder<HttpObjectWrapResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, HttpObjectWrapResponse msg, List<Object> out) throws Exception {
        //编码
        ByteBuf body = encode0(ctx, msg.getResult());
        FullHttpResponse response = msg.getHttpResponse();
        if (response == null) {
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, body);
        } else {
            response = new DefaultFullHttpResponse(msg.getHttpResponse().protocolVersion(), msg.getHttpResponse().status(), body);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json");
        HttpUtil.setContentLength(response, body.readableBytes());
        out.add(response);
    }


}