package com.ytech.tcpdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.ServerSocket;
import java.net.Socket;

public class TcpServerActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "TcpServerActivity";
    private TextView mServerState, mTvReceive;
    private Button mBtnSet, mBtnStrat, mBtnSend;
    private EditText mEditMsg;
    private ServerSocket mServerSocket;
    public Socket mSocket;

    private SharedPreferences mSharedPreferences;
    private final int DEFAULT_PORT = 8086;
    private TCPServer tcpServer = null;
    private int mServerPort; //服务端端口

    private static final String SERVER_PORT = "server_port";
    private static final String SERVER_MESSAGETXT = "server_msgtxt";

    private final int STATE_CLOSED = 1;
    private final int STATE_ACCEPTING= 2;
    private final int STATE_CONNECTED = 3;
    private final int STATE_DISCONNECTED = 4;
    private int mSocketConnectState = STATE_CLOSED;

    private String mRecycleMsg;
    private static final int MSG_TIME_SEND = 1;
    private static final int MSG_SOCKET_CONNECT = 2;
    private static final int MSG_SOCKET_DISCONNECT = 3;
    private static final int MSG_SOCKET_ACCEPTFAIL = 4;
    private static final int MSG_RECEIVE_DATA = 5;
    private static final int MSG_SEND_DATA = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_server);
        mServerState = (TextView) findViewById(R.id.serverState);
        mBtnSet = (Button)findViewById(R.id.bt_server_set);
        mBtnStrat = (Button)findViewById(R.id.bt_server_start);
        mBtnSend = (Button)findViewById(R.id.bt_server_send);
        mEditMsg = (EditText)findViewById(R.id.server_sendMsg);
        mTvReceive = (TextView) findViewById(R.id.server_receive);
        mBtnSet.setOnClickListener(this);
        mBtnStrat.setOnClickListener(this);
        mBtnSend.setOnClickListener(this);
        mSharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        mServerPort = mSharedPreferences.getInt(SERVER_PORT, DEFAULT_PORT);

        tcpServer = TCPServer.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSocketConnectState == STATE_CLOSED)
            mServerState.setText(R.string.state_closed);
        else if(mSocketConnectState == STATE_CONNECTED)
            mServerState.setText(R.string.state_connected);
        else if(mSocketConnectState == STATE_DISCONNECTED || mSocketConnectState == STATE_ACCEPTING)
            mServerState.setText(R.string.state_disconect_accept);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tcpServer.stopServer();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.bt_server_set:
                initView();
                break;
            case R.id.bt_server_start:
                initData();
                startServer();
                break;
            case R.id.bt_server_send:
                sendData();
                break;
            default:
                break;
        }
    }

    private void initView(){
        View setview = LayoutInflater.from(this).inflate(R.layout.dialog_serverset, null);
        final EditText editport = (EditText)setview.findViewById(R.id.server_port);
        Button ensureBtn = (Button)setview.findViewById(R.id.server_ok);

        editport.setText(mSharedPreferences.getInt(SERVER_PORT, 8086) + "");

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(setview); //设置dialog显示一个view
        final AlertDialog dialog = builder.show(); //dialog显示
        ensureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String port = editport.getText().toString();
                if(port != null && port.length() >0){
                    mServerPort = Integer.parseInt(port);
                }
                SharedPreferences.Editor editor=mSharedPreferences.edit();
                editor.putInt(SERVER_PORT, mServerPort);
                editor.commit();
                dialog.dismiss(); //dialog消失
            }
        });

    }
    private void startServer() {
        tcpServer.startServer();
    }

    private void sendData(){
        String str = mEditMsg.getText().toString();
        if(str.length() == 0)
            return;
       tcpServer.sendData(str.getBytes());
    }

    private void initData() {
        tcpServer = TCPServer.getInstance().init(mServerPort, 2000)
                .setStartCallback(new TCPServer.OnStartCallback() {
                    @Override
                    public void onStartSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerState.setText(R.string.state_connected);
                               // mBtnConnect.setText(R.string.disconnect);
                            }
                        });
                    }

                    @Override
                    public void onStartFailure() {
                        Log.d(TAG, "onnected failed!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSocketConnectState = STATE_DISCONNECTED;
                                mServerState.setText(R.string.connect);
                               // mClientState.setText(R.string.state_connect_fail);
                            }
                        });
                    }
                })
                .setAcceptCallback(new TCPServer.OnAcceptCallback() {
                    @Override
                    public void onAcceptSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerState.setText(R.string.state_connected);
                                //mBtnConnect.setText(R.string.disconnect);
                            }
                        });
                    }

                    @Override
                    public void onAcceptFailure() {
                        Log.d(TAG, "onnected failed!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSocketConnectState = STATE_DISCONNECTED;
                                mServerState.setText(R.string.connect);
                                //mClientState.setText(R.string.state_connect_fail);
                            }
                        });
                    }
                })
                .setReceiveCallback(new TCPServer.OnReceiveCallback() {
                    @Override
                    public void onReceiveSuccess(String recData) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerState.setText("data coming!!!");
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
                                mServerState.setText("read data failed");
                            }
                        });
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerState.setText(R.string.state_disconected);
                               // mBtnConnect.setText(R.string.connect);
                                mSocketConnectState = STATE_DISCONNECTED;
                                tcpServer.disconnect();
                            }
                        });
                    }
                })
                .setSendCallback(new TCPServer.OnSendCallback() {
                    @Override
                    public void onSendSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerState.setText("write data success");
                            }
                        });
                    }

                    @Override
                    public void onSendFailure() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerState.setText("write data failure");
                            }
                        });
                    }
                });
    }
}