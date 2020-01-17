package com.imuguys.im

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.imuguys.im.connection.MessageLongConnection
import com.imuguys.im.connection.SocketMessageListener
import com.imuguys.im.connection.message.AuthorityMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        debugLongConnection()
    }

    private fun debugLongConnection() {
        val messageLongConnection = MessageLongConnection()
        messageLongConnection.connect()
        messageLongConnection.registerMessageHandler(
            AuthorityMessage::class.java.name,
            object : SocketMessageListener<AuthorityMessage> {
                override fun handleMessage(message: AuthorityMessage) {
                    mHandler.post {
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
