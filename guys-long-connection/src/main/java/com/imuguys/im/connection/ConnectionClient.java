package com.imuguys.im.connection;

import io.netty.channel.Channel;

/**
 * 长链接客户端
 */
public class ConnectionClient {

  private Channel mChannel;
  private ConnectionBootstrap mConnectionBootstrap;
  private MessageChannelHandler mMessageChannelHandler;

  public ConnectionClient(ConnectionBootstrap connectionBootstrap, Channel channel) {
    mConnectionBootstrap = connectionBootstrap;
    mChannel = channel;
    mMessageChannelHandler = mChannel.pipeline().get(MessageChannelHandler.class);
  }

  public MessageChannelHandler getChannelHandler() {
    return mMessageChannelHandler;
  }

  public Channel getChannel() {
    return mChannel;
  }

  public void close() {
    mChannel.close();
    mConnectionBootstrap.shutdown();
  }
}
