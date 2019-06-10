package cn.wishhust.muxin.netty;



import java.io.Serializable;

public class ChatMsg implements Serializable {

    private static final long serialVersionUID = 1863991926194581521L;
    // 发送者用户Id
    private String senderId;
    // 接受者用户Id
    private String receiverId;
    // 聊天内容
    private String msg;
    // 用于消息签收
    private String msgId;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
