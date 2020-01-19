package com.gkpoter.protocol;

import java.util.Arrays;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 */
public class SendBuffer {

    private byte[] data;

    SendBuffer() {
        data = new byte[0];
    }

    void setProtocolId(int id) {
        setInt(id);
    }

    void setSubId(byte id) {
        setByte(id);
    }

    byte[] getBufferArray() {
        return data;
    }

    public void setByte(byte b) {
        mergeData(new byte[]{b});
    }

    public void setInt(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[3] = (byte) ((value >> 24) & 0xFF);
        mergeData(src);
    }

    public void setString(String str) {
        byte[] bytes = str.getBytes();
        setInt(bytes.length);
        mergeData(bytes);
    }

    private void mergeData(byte[] values) {
        data = Arrays.copyOf(data, data.length + values.length);
        System.arraycopy(values, 0, data, data.length, values.length);
    }
}
