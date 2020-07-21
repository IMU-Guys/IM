package com.imuguys.im.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * 长链接客户端
 */
public class ConnectionClient {

  private Channel mChannel;
  private ConnectionBootstrap mConnectionBootstrap;

  public ConnectionClient(ConnectionBootstrap connectionBootstrap, Channel channel) {
    mConnectionBootstrap = connectionBootstrap;
    mChannel = channel;
  }

  public MessageChannelHandler getChannelHandler() {
    return  mChannel.pipeline().get(MessageChannelHandler.class);
  }

  public Channel getChannel() {
    return mChannel;
  }
  
  public ChannelPipeline getPipeline() {
    return mChannel.pipeline();
  }

  public void close() {
    mChannel.close();
    mConnectionBootstrap.shutdown();
  }
}
