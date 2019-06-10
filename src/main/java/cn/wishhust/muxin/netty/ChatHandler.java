package cn.wishhust.muxin.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;

public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static ChannelGroup clients =
            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String content = msg.text();
        System.out.println("收到数据："+content);

        for (Channel chanel:clients) {
            chanel.writeAndFlush(
                    new TextWebSocketFrame(
                            "[服务器接收到消息：]"+ LocalDateTime.now() +"，消息为"+content
                    ));
        }

//        clients.writeAndFlush(
//                new TextWebSocketFrame(
//                        "[服务器接收到消息：]"+ LocalDateTime.now() +"，消息为"+content
//                ));
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // ChannelGroup自动移除对应的客户端channel
//        clients.remove(ctx.channel());
        System.out.println("客户端断开，长id"+ctx.channel().id().asLongText());
        System.out.println("客户端断开，短id"+ctx.channel().id().asShortText());
    }
}
