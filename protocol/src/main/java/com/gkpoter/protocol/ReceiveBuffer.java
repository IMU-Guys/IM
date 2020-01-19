package com.gkpoter.protocol;

import java.nio.charset.StandardCharsets;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 */
public class ReceiveBuffer {

    private byte[] data;
    private int position;

    ReceiveBuffer(byte[] bytes) {
        position = 0;
        data = new byte[bytes.length];
        System.arraycopy(bytes, 0, data, 0, data.length);
    }

    int getProtocolId(){
        return getInt();
    }

    byte getSubId(){
        return getByte();
    }

    public byte getByte() {
        byte[] dest = new byte[1];
        System.arraycopy(data, position, dest, 0, dest.length);
        position += 1;
        return dest[0];
    }

    public int getInt() {
        byte[] dest = new byte[4];
        System.arraycopy(data, position, dest, 0, dest.length);
        int value = (dest[0] & 0xFF)
                | ((dest[1] & 0xFF) << 8)
                | ((dest[2] & 0xFF) << 16)
                | ((dest[3] & 0xFF) << 24);
        position += 4;
        return value;
    }

    public String getString() {
        int len = getInt();
        byte[] dest = new byte[len];
        System.arraycopy(data, position, dest, 0, dest.length);
        return new String(dest, StandardCharsets.UTF_8);
    }

    public void reset() {
        data = new byte[0];
        position = 0;
    }

}
