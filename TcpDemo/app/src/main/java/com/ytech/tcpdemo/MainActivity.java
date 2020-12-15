package com.ytech.tcpdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mBtnClient = (Button) findViewById(R.id.bt_client);
        Button mBtnServer = (Button) findViewById(R.id.bt_server);
        mBtnClient.setOnClickListener(this);
        mBtnServer.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.bt_client:
                //做Tcp客户端
                Intent client = new Intent(this, TcpClientActivity.class);
                startActivity(client);
                break;
            case R.id.bt_server:
                //做Tcp服务端
                Intent server = new Intent(this,TcpServerActivity.class);
                startActivity(server);
                break;
            default:
                break;
        }
    }
}