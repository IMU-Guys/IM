package com.gkpoter.protocol.test;

import com.gkpoter.protocol.ProtocolManager;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 */
public class Main {

    public static void main(String[] args) {
        ProtocolManager manager = ProtocolManager.getInstance();
        manager.registerProtocol(new MessageProtocol());

        /*test send*/
        new Thread() {
            @Override
            public void run() {
                HeartProtocol.startHeart();
                MessageProtocol.sendMessage("hahaha");
                MessageProtocol.sendRequestForMsg(0, "{\"msg\":\"all\"}");
            }
        }.start();

        /*test close*/
        manager.closeSocket();
    }
}
