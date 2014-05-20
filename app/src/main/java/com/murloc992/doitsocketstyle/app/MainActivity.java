package com.murloc992.doitsocketstyle.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.ArrayList;


public class MainActivity extends Activity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener,SensorEventListener {

    private Socket socket;

    private static final int SERVERPORT = 13337;
    private static String SERVER_IP = "78.60.11.34";

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private static enum packet_type
    {
        PACKET_CONNECT(0x0),
        PACKET_DATA(0x1),
        PACKET_DISCONNECT(0x2),
        PACKET_ACCELEROMETER(0x3),
        PACKET_HEIGHT_CHANGE(0x4);

        private int value;

        packet_type(int i) {
            this.value=i;
        }

        public int getValue() {
            return value;
        }
    }

    class Packet
    {
        private packet_type packetType;
        private String buffer;

        Packet(packet_type type)
        {
            this.packetType=type;
            buffer="";
            appendHeader();
        }

        private void appendHeader()
        {
            buffer+='P';
            buffer+=this.packetType.getValue();
            buffer+=':';
        }

        private void appendEnd()
        {
            buffer+=';';
        }

        public Packet appendData(Object data)
        {
            buffer+=data.toString();
            buffer+=',';
            return this;
        }

        public boolean send(Socket socket)
        {
            //get rid off of the trailing comma
            buffer=buffer.substring(0,buffer.length()-1);
            appendEnd();

            if(socket!=null && socket.isConnected())
            {
                if(buffer.toCharArray().length<2048) {
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())),
                                true
                        );

                        out.println(buffer.toCharArray());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
            return true;
        }
    }

    class ClientThread implements Runnable {
        private MainActivity activity;
        ClientThread(MainActivity act)
        {
            activity=act;
        }

        @Override
        public void run() {
            try{
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                socket = new Socket(serverAddr, SERVERPORT);
            }catch (Exception e)
            {
                e.printStackTrace();
                return;
            }

            Packet p=new Packet(packet_type.PACKET_CONNECT);

            if(!p.send(socket))
                return;
            try{
                runOnUiThread(new Runnable(){
                    public void run()
                    {
                        setContentView(R.layout.activity_gyro);
                        SeekBar sb=(SeekBar) findViewById(R.id.seekBar);
                        sb.setOnSeekBarChangeListener(activity);

                        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

                        mSensorManager.registerListener(activity, mSensor, 1000000);
                    }
                });
            }catch(Exception e){e.printStackTrace();}


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(this);
    }

    @Override
    protected void onDestroy()
    {
        if(socket.isConnected()) {
            new Packet(packet_type.PACKET_DISCONNECT).send(socket);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
        System.exit(0);
    }

    public void onClick(View view) {
        try {
            if(view.getId()==R.id.button)
            {
                EditText et=(EditText) findViewById(R.id.editText);
                SERVER_IP=et.getText().toString();
                new Thread(new ClientThread(this)).start();
            }
            //EditText et = (EditText) findViewById(R.id.editText);
            //new Packet(packet_type.PACKET_DATA).appendData(et.getText().toString()).send(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        new Packet(packet_type.PACKET_HEIGHT_CHANGE).appendData(seekBar.getProgress()).send(socket);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onSensorChanged(SensorEvent event) {
            new Packet(packet_type.PACKET_ACCELEROMETER).appendData(event.values[0]).appendData(event.values[1]).appendData(event.values[2]).send(socket);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}