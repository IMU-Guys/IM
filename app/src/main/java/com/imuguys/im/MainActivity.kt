package com.imuguys.im

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.imuguys.im.connection.LongConnection
import com.imuguys.im.connection.SocketMessageListener
import com.imuguys.im.connection.message.AuthorityMessage
import com.imuguys.im.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mHandler = Handler()
    private val messageLongConnection = LongConnection()
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
                AuthorityMessage("abc")
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
            AuthorityMessage::class.java.name,
            object : SocketMessageListener<AuthorityMessage> {
                override fun handleMessage(message: AuthorityMessage) {
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
            messageLongConnection.sendMessage(AuthorityMessage("abc"))
        }
    }
}
