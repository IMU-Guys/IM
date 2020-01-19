package com.gkpoter.protocol;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

/**
 * Created by "nullpointexception0" on 2020/1/19.
 */
public class ProtocolManager {

    private SocketManager mSocketManager;
    private SparseArray<BaseProtocol> mProtocols;
    private static ProtocolManager mProtocolManagerInstance;
    private Thread mThread;

    private ProtocolManager() {
        mProtocols = new SparseArray<>();
        mSocketManager = new SocketManager();
        mThread = new Thread(mRunnable);
        mThread.start();
    }

    public static ProtocolManager getInstance() {
        if (mProtocolManagerInstance == null) {
            synchronized (ProtocolManager.class) {
                if (mProtocolManagerInstance == null) {
                    mProtocolManagerInstance = new ProtocolManager();
                }
            }
        }
        return mProtocolManagerInstance;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            SendBuffer sendBuffer = (SendBuffer) msg.obj;
            mSocketManager.write(sendBuffer);
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mThread.isInterrupted()) {
                ReceiveBuffer receiveBuffer = mSocketManager.read();
                int protocolId = receiveBuffer.getProtocolId();
                BaseProtocol protocol = mProtocols.get(protocolId);
                if (protocol != null) {
                    protocol.onReceive(receiveBuffer.getSubId(), receiveBuffer);
                }
            }
        }
    };

    public void registerProtocol(BaseProtocol protocol) {
        mProtocols.put(protocol.getId(), protocol);
        protocol.setHandler(mHandler);
    }

    public void reConnectSocket(){
        mSocketManager = new SocketManager();
    }

    public void closeSocket() {
        mThread.interrupt();
        mRunnable = null;
    }

}
