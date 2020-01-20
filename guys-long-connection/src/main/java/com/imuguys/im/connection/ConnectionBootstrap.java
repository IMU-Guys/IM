package com.imuguys.im.connection;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 包装 Netty {@link Bootstrap}
 */
public class ConnectionBootstrap {

  private Bootstrap mBootstrap;
  private EventLoopGroup mEventLoopGroup;

  public void configureBootstrap(int connectTimeOutMs) {
    mBootstrap = new Bootstrap();
    mEventLoopGroup = new NioEventLoopGroup();
    mBootstrap.group(mEventLoopGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOutMs)
        .handler(new MessageChannelHandlerInitializer());
  }

  public ConnectionClient connect(InetSocketAddress inetSocketAddress) {
    ChannelFuture channelFuture = mBootstrap.connect(inetSocketAddress).awaitUninterruptibly();
    if (channelFuture.isCancelled()) {
      channelFuture.cause().printStackTrace();
      throw new RuntimeException("Connection attempt cancelled by user");
    } else if (!channelFuture.isSuccess()) {
      // 连接失败，抛到外层处理
      if (channelFuture.cause() instanceof RuntimeException) {
        throw (RuntimeException) (channelFuture.cause());
      } else {
        throw new RuntimeException(channelFuture.cause());
      }
    } else {
      Channel channel = channelFuture.channel();
      return new ConnectionClient(this, channel);
    }
  }

  public void shutdown() {
    mEventLoopGroup.shutdownGracefully();
  }
}
