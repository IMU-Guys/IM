package com.imuguys.im

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.imuguys.im.connection.LongConnection
import com.imuguys.im.connection.LongConnectionParams
import com.imuguys.im.connection.SocketMessageListener
import com.imuguys.im.connection.message.AuthorityMessage
import com.imuguys.im.connection.message.AuthorityResponseMessage
import com.imuguys.im.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler()
    private val messageLongConnection = LongConnection(LongConnectionParams("192.168.0.103", 8880))
    private lateinit var mMainActivityDataBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainActivityDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initView()
        debugLongConnection()
    }

    private fun initView() {
        mMainActivityDataBinding.sendAuthorityMessage.setOnClickListener {
            messageLongConnection.sendMessage(
                AuthorityMessage("user","pwd")
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(null)
        messageLongConnection.disconnect()
    }

    private fun debugLongConnection() {
        messageLongConnection.connect()
        messageLongConnection.registerMessageHandler(
            AuthorityResponseMessage::class.java.name,
            object : SocketMessageListener<AuthorityResponseMessage> {
                override fun handleMessage(message: AuthorityResponseMessage) {
                    mHandler.post {
                        mMainActivityDataBinding.serverInformation.text = message.toString()
                        Toast.makeText(
                            this@MainActivity,
                            message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        GlobalScope.launch {
            delay(3000)
            messageLongConnection.sendMessage(AuthorityMessage("user","pwd"))
        }
    }
}
