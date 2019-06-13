package cn.wishhust.muxin.netty;

import cn.wishhust.muxin.SpringUtil;
import cn.wishhust.muxin.enums.MsgActionEnum;
import cn.wishhust.muxin.service.UserService;
import cn.wishhust.muxin.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static ChannelGroup users =
            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String content = msg.text();
        Channel currentChannel = ctx.channel();

        // 1. 获取客户端发来的消息
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();
        // 2. 判断消息的类型，不同类型处理不同
        if (action == MsgActionEnum.CONNECT.type) {
            //  2.1 当websocket 第一次open时，初始化channel，把用的channel和userid关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, currentChannel);

            // 测试
            for (Channel c : users) {
                System.out.println(c.id().asLongText());
            }
            UserChannelRel.output();
        } else if (action == MsgActionEnum.CHAT.type) {
            //  2.2 聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态【未签收】
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getMsg();
            String receiverId = chatMsg.getReceiverId();
            String senderId = chatMsg.getSenderId();

            // 消息保存到数据库
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

            Channel receiverChannel = UserChannelRel.get(receiverId);
            if (null == receiverChannel) {
               // todo channel为空代表用户离线，推送消息（JPush）
            } else {
                // 当receiverChannel不为空时，从ChannelGroup去查询对应channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (null != findChannel) {
                    receiverChannel.writeAndFlush(
                            new TextWebSocketFrame(
                                    JsonUtils.objectToJson(dataContentMsg)));
                } else {
                    // todo 用户离线，推送消息
                }
            }
        } else if (action == MsgActionEnum.SIGNED.type) {
            //  2.3 签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态【已签收】
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            // 扩展字段在signed类型的消息中，代表需要去签收的消息id，逗号间隔
            String msgIdsStr = dataContent.getExtend();
            String[] msgIds = msgIdsStr.split(",");
            ArrayList<String> msgIdList = new ArrayList<>();
            for (String mid : msgIds) {
                if(StringUtils.isNotBlank(mid)) {
                    msgIdList.add(mid);
                }
            }
            System.out.println(msgIdList.toString());
            if (null != msgIdList && !msgIdList.isEmpty() && msgIdList.size() > 0) {
                // 批量签收
                userService.updateMsgSigned(msgIdList);
            }

        } else if (action == MsgActionEnum.KEEPALIVE.type) {
            //  2.4 心跳类型消息
            System.out.println("收到来自channel为[" + currentChannel + "]的心跳包...");
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        // 发生异常之后关闭连接（关闭channel），随后从ChannelGroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        System.out.println("客户端被移除，channelId为："+channelId);
        // ChannelGroup自动移除对应的客户端channel
        users.remove(ctx.channel());
    }
}
