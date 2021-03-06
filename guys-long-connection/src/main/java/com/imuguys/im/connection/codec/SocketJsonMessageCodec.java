package com.imuguys.im.connection.codec;

import com.imuguys.im.connection.message.SocketJsonMessage;
import com.imuguys.im.connection.message.SocketMessageWrapper;
import com.imuguys.im.connection.utils.Gsons;

import java.io.Serializable;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;

/**
 * encode: 将给定的消息添加到 {@link SocketJsonMessage} 内部，然后转换成Json String
 * decode: 将Json格式的ByteBuf 转换为 {@link SocketMessageWrapper}, 上层从内部取出具体消息内容
 */
@ChannelHandler.Sharable
public class SocketJsonMessageCodec extends MessageToMessageCodec<ByteBuf, Serializable> {
  @Override
  protected void encode(ChannelHandlerContext ctx, Serializable msg, List<Object> out)
      throws Exception {
    out.add(Gsons.GUYS_GSON.toJson(new SocketJsonMessage(msg.getClass().getName(),
        Gsons.GUYS_GSON.toJson(msg))));
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    SocketJsonMessage socketJsonMessage = Gsons.GUYS_GSON
        .fromJson(msg.toString(CharsetUtil.UTF_8), SocketJsonMessage.class);
    SocketMessageWrapper socketMessageWrapper =
        new SocketMessageWrapper(socketJsonMessage.getClassName(), Gsons.GUYS_GSON.fromJson(
            socketJsonMessage.getClassBytes(), Class.forName(socketJsonMessage.getClassName())));
    out.add(socketMessageWrapper);
  }
}
