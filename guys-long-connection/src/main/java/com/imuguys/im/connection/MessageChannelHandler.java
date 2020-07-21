package com.imuguys.im.connection;

import android.util.Log;
import com.imuguys.im.connection.message.SocketMessageWrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Json处理
 * 处理收到的Json，通知MessageHandler
 */
class MessageChannelHandler extends SimpleChannelInboundHandler<SocketMessageWrapper> {

  private static final String TAG = "MessageChannelHandler";
  private LongConnectionContextV2 mLongConnectionContextV2;
  private Map<String, SocketMessageListener<Object>> mSocketMessageHandlers =
      new ConcurrentHashMap<>();

  /**
   * 收到读事件后的处理
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, SocketMessageWrapper msg)
      throws Exception {
    SocketMessageListener targetSocketJsonMessage = mSocketMessageHandlers.get(msg.getClassName());
    if (targetSocketJsonMessage != null) {
      targetSocketJsonMessage.handleMessage(msg.getMessage());
    } else {
      Log.w(TAG, "no compatible listener for message type: " + msg.getClassName());
    }
  }

  /**
   * 添加消息观察者
   */
  @SuppressWarnings("unchecked")
  public <Message> void addMessageListener(
      String messageClassName,
      SocketMessageListener<Message> messageListener) {
    mSocketMessageHandlers.put(messageClassName, (SocketMessageListener<Object>) messageListener);
  }

  public void removeMessageListener(String messageClassName) {
    mSocketMessageHandlers.remove(messageClassName);
  }

  public void setLongConnectionContext(LongConnectionContextV2 longConnectionContextV2) {
    mLongConnectionContextV2 = longConnectionContextV2;
  }
}
