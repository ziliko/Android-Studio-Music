package com.example.music2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//implements 监听类，可以更简洁导用不同按键控件？ 需实现onClick(View v)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //
    private static final String TAG = "MainActivity";//外面输入logt按TAB键，自动补全
    TextView tv1;
    Button Local,Internet;

    //private SharedPreferences sp;//存放3个装备，以及5个判定？

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Local=(Button) findViewById(R.id.Local);
        Internet=(Button) findViewById(R.id.Internet);
        Local.setOnClickListener(MainActivity.this);
        Internet.setOnClickListener(MainActivity.this);
        tv1=(TextView)findViewById(R.id.text01);

        Log.d(TAG,"主界面启动");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Local:
                Intent intent=new Intent(this,Localmusic.class);
                startActivity(intent);
                finish();
                break;
            case R.id.Internet://暂时改成模式切换
                Intent intent2=new Intent(this,Internetmusic.class);
                startActivity(intent2);
                finish();
                break;
            default:break;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }


}
