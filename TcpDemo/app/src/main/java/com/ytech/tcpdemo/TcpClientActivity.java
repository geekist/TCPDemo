package com.ytech.tcpdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.Socket;

public class TcpClientActivity extends AppCompatActivity implements View.OnClickListener {

    private final int STATE_DISCONNECTED = 1;
    private final int STATE_CONNECTING = 2;
    private final int STATE_CONNECTED = 3;

    private int mSocketConnectState = STATE_DISCONNECTED;

    private final String TAG = "TcpClientActivity";
    private Button mBtnSet, mBtnConnect, mBtnSend;
    private EditText mEditMsg;
    private TextView mClientState, mTvReceive;
    public Socket mSocket;

    private SharedPreferences mSharedPreferences;
    private final int DEFAULT_PORT = 8086;
    private String mIpAddress;  //服务端ip地址
    private int mClientPort; //端口,默认为8086，可以进行设置
    private static final String IP_ADDRESS = "ip_address";
    private static final String CLIENT_PORT = "client_port";
    private static final String CLIENT_MESSAGETXT = "client_msgtxt";

    TCPClient tcpClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_client);
        mBtnSet = (Button) findViewById(R.id.bt_client_set);
        mBtnConnect = (Button) findViewById(R.id.bt_client_connect);
        mBtnSend = (Button) findViewById(R.id.bt_client_send);
        mEditMsg = (EditText) findViewById(R.id.client_sendMsg);
        mClientState = (TextView) findViewById(R.id.client_state);
        mTvReceive = (TextView) findViewById(R.id.client_receive);
        mBtnSet.setOnClickListener(this);
        mBtnConnect.setOnClickListener(this);
        mBtnSend.setOnClickListener(this);
        mSharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        //获取保存的ip地址、客户端端口号
        mIpAddress = mSharedPreferences.getString(IP_ADDRESS, null);
        mClientPort = mSharedPreferences.getInt(CLIENT_PORT, DEFAULT_PORT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSocketConnectState == STATE_CONNECTED) {
            mBtnConnect.setText(R.string.disconnect);
            mClientState.setText(R.string.state_connected);
        } else if (mSocketConnectState == STATE_DISCONNECTED) {
            mBtnConnect.setText(R.string.connect);
            mClientState.setText(R.string.state_disconected);
        } else if (mSocketConnectState == STATE_CONNECTING) {
            mClientState.setText(R.string.state_connecting);
            mClientState.setText(R.string.state_connected);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tcpClient.disconnect();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_client_set:
                set();
                break;
            case R.id.bt_client_connect:
                if (mSocketConnectState == STATE_CONNECTED) {
                    tcpClient.disconnect();
                } else {
                    startConnect();
                }
                break;
            case R.id.bt_client_send:
                sendTxt();
                break;
            default:
                break;
        }
    }

    private void set() {
        View setview = LayoutInflater.from(this).inflate(R.layout.dialog_clientset, null);
        final EditText ipAddress = (EditText) setview.findViewById(R.id.edtt_ipaddress);
        final EditText editport = (EditText) setview.findViewById(R.id.client_port);
        Button ensureBtn = (Button) setview.findViewById(R.id.client_ok);

        ipAddress.setText(mSharedPreferences.getString(IP_ADDRESS, null));
        editport.setText(mSharedPreferences.getInt(CLIENT_PORT, 8086) + "");

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(setview); //设置dialog显示一个view
        final AlertDialog dialog = builder.show(); //dialog显示
        ensureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String port = editport.getText().toString();
                mIpAddress = ipAddress.getText().toString();
                if (port != null && port.length() > 0) {
                    mClientPort = Integer.parseInt(port);
                }
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(IP_ADDRESS, mIpAddress);
                editor.putInt(CLIENT_PORT, mClientPort);
                editor.commit();
                dialog.dismiss(); //dialog消失
            }
        });

    }

    private void startConnect() {
        Log.i(TAG, "startConnect");
        if (mIpAddress == null || mIpAddress.length() == 0) {
            Toast.makeText(this, "请设置ip地址", Toast.LENGTH_LONG).show();
            return;
        }

        initData();
        tcpClient.connect();
    }

    private void sendTxt() {
        String str = mEditMsg.getText().toString();
        if (str.length() == 0)
            return;

        tcpClient.sendData(str.getBytes());
    }

    private void initData() {
        tcpClient = TCPClient.getInstance().init(mIpAddress, mClientPort, 2000)
                .setConnectCallback(new TCPClient.OnConnectCallback() {
                    @Override
                    public void onConnectSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mClientState.setText(R.string.state_connected);
                                mBtnConnect.setText(R.string.disconnect);
                            }
                        });
                    }

                    @Override
                    public void onConnectFailure() {
                        Log.d(TAG, "onnected failed!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSocketConnectState = STATE_DISCONNECTED;
                                mBtnConnect.setText(R.string.connect);
                                mClientState.setText(R.string.state_connect_fail);
                            }
                        });
                    }
                })
                .setReceiveDataCallback(new TCPClient.OnReceiveCallback() {
                    @Override
                    public void onReceiveSuccess(String recData) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mClientState.setText("data coming!!!");
                                String text = mTvReceive.getText().toString() + "\r\n" + recData;
                                mTvReceive.setText(text);
                            }
                        });
                    }

                    @Override
                    public void onReceiveFailure() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mClientState.setText("read data failed");
                            }
                        });
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mClientState.setText(R.string.state_disconected);
                                mBtnConnect.setText(R.string.connect);
                                mSocketConnectState = STATE_DISCONNECTED;
                                tcpClient.disconnect();
                            }
                        });
                    }
                })
                .setSendDataCallback(new TCPClient.OnSendCallback() {
                    @Override
                    public void onSendSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mClientState.setText("write data success");
                            }
                        });
                    }

                    @Override
                    public void onSendFailure() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mClientState.setText("write data failure");
                            }
                        });
                    }
                });
    }
}