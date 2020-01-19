package com.gkpoter.protocol.test;

import com.gkpoter.protocol.BaseProtocol;
import com.gkpoter.protocol.ReceiveBuffer;
import com.gkpoter.protocol.SendBuffer;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 * 消息协议类测试样例
 */
public class MessageProtocol extends BaseProtocol {

    private static final int PROTOCOL_ID = 0x200;

    //发消息
    private static final byte SUB_SEND_MESSAGE = 0x1;
    //请求全部消息
    private static final byte SUB_REQUEST_ALL_MSG = 0x2;

    public MessageProtocol() {
        super(PROTOCOL_ID);
    }

    public static void sendMessage(Object... objects) {
        MessageProtocol protocol = new MessageProtocol();
        protocol.send(SUB_SEND_MESSAGE);
    }

    public static void sendRequestForMsg(Object... objects) {
        MessageProtocol protocol = new MessageProtocol();
        protocol.send(SUB_REQUEST_ALL_MSG);
    }

    @Override
    protected boolean onSend(byte subId, SendBuffer sendBuffer) {
        switch (subId) {
            case SUB_SEND_MESSAGE: {
                //such as：填充发送数据
                sendBuffer.setByte((byte) 0);
                sendBuffer.setInt(1);
                sendBuffer.setString("text");
                break;
            }
            case SUB_REQUEST_ALL_MSG: {
                sendBuffer.setByte((byte) 0);
                sendBuffer.setInt(1);
                sendBuffer.setString("json");
                break;
            }
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onReceive(byte subId, ReceiveBuffer receiveBuffer) {
        switch (subId) {
            case SUB_SEND_MESSAGE: {
                //such as：处理返回数据
                receiveBuffer.getInt();
                receiveBuffer.getByte();
                receiveBuffer.getString();
                break;
            }
            case SUB_REQUEST_ALL_MSG: {
                receiveBuffer.getInt();
                receiveBuffer.getByte();
                receiveBuffer.getString();
                break;
            }
            default:
                break;
        }
    }
}
