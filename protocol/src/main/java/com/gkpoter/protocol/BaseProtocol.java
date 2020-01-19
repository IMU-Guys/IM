package com.gkpoter.protocol;

import android.os.Handler;
import android.os.Message;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 */
public abstract class BaseProtocol {

    private int id;
    private Handler handler;

    public BaseProtocol(int id) {
        this.id = id;
    }

    /**
     * 返回false不发送
     */
    protected abstract boolean onSend(byte subId, SendBuffer sendBuffer);

    protected abstract void onReceive(byte subId, ReceiveBuffer receiveBuffer);

    private void beforeSend(SendBuffer sendBuffer, byte subId) {
        sendBuffer.setProtocolId(id);
        sendBuffer.setSubId(subId);
    }

    public void send(byte subId) {
        SendBuffer sendBuffer = new SendBuffer();
        beforeSend(sendBuffer, subId);
        if (onSend(subId, sendBuffer)) {
            afterSend(sendBuffer);
        }
    }

    private void afterSend(SendBuffer sendBuffer) {
        Message message = Message.obtain();
        message.obj = sendBuffer;
        handler.sendMessage(message);
    }

    int getId() {
        return id;
    }

    void setHandler(Handler handler) {
        this.handler = handler;
    }
}
