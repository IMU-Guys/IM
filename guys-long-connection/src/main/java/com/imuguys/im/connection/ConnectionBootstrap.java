package com.imuguys.im.connection;

import androidx.annotation.Nullable;

import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * 包装 Netty {@link Bootstrap}
 */
public class ConnectionBootstrap {

  private Bootstrap mBootstrap;
  private EventLoopGroup mEventLoopGroup;
  @Nullable
  private ConnectionClient mConnectionClient;

  public ConnectionBootstrap(int connectTimeOutMs) {
    mBootstrap = new Bootstrap();
    mEventLoopGroup = new NioEventLoopGroup();
    mBootstrap.group(mEventLoopGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOutMs)
        .handler(new MessageChannelHandlerInitializer());

  }

  public Observable<ConnectionClient> connect(SocketAddress socketAddress) {
    ChannelFuture channelFuture = mBootstrap.connect(socketAddress);
    PublishSubject<Boolean> publishSubject = PublishSubject.create();
    channelFuture.addListener(future -> {
      if (future.isCancelled()) {
        publishSubject.onError(new RuntimeException("Connection attempt cancelled by user"));
      } else if (!future.isSuccess()) {
        publishSubject.onError(future.cause());
      } else {
        publishSubject.onNext(true);
      }
    });
    return publishSubject
        .doOnNext(b -> mConnectionClient = new ConnectionClient(this, channelFuture.channel()))
        .observeOn(AndroidSchedulers.mainThread())
        .map(o -> mConnectionClient);
  }

  @Nullable
  public ConnectionClient getConnectionClient() {
    return mConnectionClient;
  }

  public void shutdown() {
    mEventLoopGroup.shutdownGracefully();
  }
}
