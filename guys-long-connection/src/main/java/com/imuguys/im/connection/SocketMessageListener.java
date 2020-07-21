package com.imuguys.im.connection;

/**
 * Socket消息回调
 */
public interface SocketMessageListener<Message> {

    /**
     * 回调发生在Netty线程
     */
    void handleMessage(Message message);
}
