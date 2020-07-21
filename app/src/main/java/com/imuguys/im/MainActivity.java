package com.imuguys.im;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.databinding.DataBindingUtil;
import com.imuguys.im.connection.LongConnectionParams;
import com.imuguys.im.connection.LongConnectionV2;
import com.imuguys.im.connection.message.AuthorityMessage;
import com.imuguys.im.connection.message.AuthorityResponseMessage;
import com.imuguys.im.connection.message.IncrementMessage;
import com.imuguys.im.connection.test.Test;
import com.imuguys.im.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

  private Handler mHandler = new Handler();
  private LongConnectionV2 messageLongConnection =
      new LongConnectionV2(new LongConnectionParams("172.17.236.162", 8880));
  private ActivityMainBinding mMainActivityDataBinding;
  private int testNumber = 1;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mMainActivityDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    hotFixTest();
    initView();
    debugLongConnection();
  }

  private void hotFixTest() {
    Test test = new Test();
    Toast.makeText(this, test.getTestString(), Toast.LENGTH_SHORT).show();
  }


  private void initView() {
    mMainActivityDataBinding.sendAuthorityMessage.setOnClickListener(
        v -> messageLongConnection.sendMessage(new AuthorityMessage("user", "pwd")));
    mMainActivityDataBinding.sendTestMessage.setOnClickListener(
        v -> {
          messageLongConnection.sendMessage(new IncrementMessage(testNumber));
          testNumber *= 2;
        });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mHandler.removeCallbacks(null);
    messageLongConnection.release();
  }

  private void debugLongConnection() {
    messageLongConnection.connect();
    messageLongConnection.registerMessageHandler(
        AuthorityResponseMessage.class,
        authorityResponseMessage -> mHandler.post(() -> {
          mMainActivityDataBinding.serverInformation.setText(authorityResponseMessage.toString());
          Toast.makeText(MainActivity.this, authorityResponseMessage.toString(), Toast.LENGTH_SHORT)
              .show();
        }));
    messageLongConnection.registerMessageHandler(IncrementMessage.class, incrementMessage -> {
      mHandler.post(() -> {
        Toast.makeText(MainActivity.this, incrementMessage.toString(), Toast.LENGTH_SHORT)
                .show();
      });
    });
  }
}
