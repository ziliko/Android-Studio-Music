package com.example.music2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class Music_Service extends Service {
    private static final String TAG = "Music_Service";
    private String notificationId = "serviceid";
    private String notificationName = "servicename";

    public Music_Service() {
    }
    //调用startService()启动时生命流程：onCreate  onStartCommand
    //调用stopService()停止时生命流程：onDestroy

    //仅当第一次创建该Service时被调用
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        /*创建一个前台服务 TODO 是否会有版本不兼容的问题？待确认*/
        //Intent intent=new Intent(this,MainActivity.class);
        //PendingIntent pi=PendingIntent.getActivities(this,0,intent,0);
        Notification notification=new NotificationCompat.Builder(this)
                .setContentTitle("my音乐播放器")         //用于指定通知的标题内容
                .setContentText("music2")                 //用于指定通知的正文内容
                //.setWhen(System.currentTimeMillis())    //用于指定通知被创建的时间 单位毫秒
                .setSmallIcon(R.drawable.myback)           //用于指定通知的小图标,注意只能用纯alpha图层的图片 默认.ic_launcher
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.myback))  //用于指定通知的大图标
                //.setContentIntent(pi)
                .build();
        startForeground(1,notification);

        super.onCreate();
    }
    //每次启动Service都会调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");

        return super.onStartCommand(intent, flags, startId);
    }
    //服务销毁时调用
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");

        super.onDestroy();
    }

    //用于活动和服务进行通信
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /***********************************************************************************************/





}
