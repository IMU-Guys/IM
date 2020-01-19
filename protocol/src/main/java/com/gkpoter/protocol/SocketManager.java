package com.gkpoter.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 */
class SocketManager {

    private InputStream inputStream;
    private OutputStream outputStream;

    SocketManager() {
        try {
            Socket socket = new Socket(URLConfig.SOCKET_URL, URLConfig.SOCKET_URL_PORT);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void write(SendBuffer buffer) {
        try {
            outputStream.write(buffer.getBufferArray());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ReceiveBuffer read() {
        try {
            byte[] data = new byte[0];
            byte[] bytes = new byte[1024];
            int len;
            while ((len = inputStream.read()) > -1) {
                int bytesLen = inputStream.read(bytes, 0, len);
                data = Arrays.copyOf(data, data.length + bytes.length);
                System.arraycopy(bytes, 0, data, data.length, bytesLen);
            }
            return new ReceiveBuffer(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
