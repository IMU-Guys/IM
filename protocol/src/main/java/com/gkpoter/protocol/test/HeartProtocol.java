package com.gkpoter.protocol.test;

import android.os.Handler;

import com.gkpoter.protocol.BaseProtocol;
import com.gkpoter.protocol.ProtocolManager;
import com.gkpoter.protocol.ReceiveBuffer;
import com.gkpoter.protocol.SendBuffer;


/**
 * Created by "nullpointexception0" on 2020/1/19.
 * 心跳协议测试
 */
public class HeartProtocol extends BaseProtocol {

    private static final int PROTOCOL_ID = 0x201;

    //发心跳包
    private static final byte SUB_SEND_HEART = 0x0;

    public HeartProtocol() {
        super(PROTOCOL_ID);
    }

    private static HeartProtocol mProtocol;
    private static Handler mHandler;
    private static int mTimeOutTimes = 0;
    private static Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            mProtocol.send(SUB_SEND_HEART);
            mTimeOutTimes++;
            //大于3次认为断开，重连
            if (mTimeOutTimes > 3) {
                ProtocolManager.getInstance().reConnectSocket();
            }
            mHandler.postDelayed(mRunnable, 10000);//10s
        }
    };

    public static void startHeart() {
        mProtocol = new HeartProtocol();
        mHandler = new Handler();
        mHandler.post(mRunnable);
    }

    @Override
    protected boolean onSend(byte subId, SendBuffer sendBuffer) {
        return true;
    }

    @Override
    protected void onReceive(byte subId, ReceiveBuffer receiveBuffer) {
        mTimeOutTimes--;
    }
}
