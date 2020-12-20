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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Music_Service extends Service implements HeadsetButtonReceiver.onHeadsetListener {
    private static final String TAG = "Music_Service";
    private String notificationId = "serviceid";
    private String notificationName = "servicename";

    HeadsetButtonReceiver headsetButtonReceiver;
    List<Song> list;//持有一个播放列表，copy Activity里的，保证内循环
    MediaPlayer player=new MediaPlayer();
    int number;
    int mode;//0 1 2
    int list_kind;//0 1 2
    private Timer timer;

    public Music_Service() {
    }
    //调用startService()启动时生命流程：onCreate  onStartCommand
    //调用stopService()停止时生命流程：onDestroy
    //********************************************分割线：生命周期**************************************************
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

        //新建耳机线控广播接收器的实例对象
        headsetButtonReceiver=new HeadsetButtonReceiver(this);
        headsetButtonReceiver.setOnHeadsetListener(this);
        this.initplayerListener();

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
        headsetButtonReceiver.unregisterHeadsetReceiver();//注销耳机线控广播
        if(timer!=null) timer.cancel();
        if(player!=null){
            player.stop();
            player.release();
        }
        super.onDestroy();
    }

    //用于活动和服务进行通信
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new MyBinder();
        //throw new UnsupportedOperationException("Not yet implemented");
    }
    class  MyBinder extends Binder{
        public Music_Service getService(){
            return Music_Service.this;
        }
    }

    //********************************************分割线：播放功能函数**************************************************
    //播放音乐函数
    public void play(String path) {
        if(path==null) path="";
        Localmusic.tv2.setText("路径:"+path);//显示路径
        final String thispath=path;
        try {
            Localmusic.testpath=path;//记录一下地址供引用
            // 重置音频文件，防止多次点击会报错
            player.reset();
            // 调用方法传进播放地址
            player.setDataSource(path);
            // 异步准备资源，防止卡顿
            player.prepareAsync();
            // 调用音频的监听方法，音频准备完毕后响应该方法进行音乐播放
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    //2020/9/1添加 start ps:getDuration必须准备好才能获取，否则是-1
                    final int duration = player.getDuration();//获取音乐总时间

                    Localmusic.seekBar.setMax(duration);//将音乐总时间设置为Seekbar的最大值
                    Localmusic.time_now.setText("0:00");
                    Localmusic.time_end.setText(""+getTime(duration/1000));
                    //if(timer!=null) {timer.cancel();timer=null;}
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(!Localmusic.isSeekbarChaning&&player.isPlaying()){
                                int x=player.getCurrentPosition();
                                Localmusic.seekBar.setProgress(x);//获取当前播放到哪的时间点
                            }
                            Localmusic.delay1+=4;
                            if(Localmusic.delay1>1000) Localmusic.delay1=11;
                        }
                    },0,200);
                    //2020/9/1添加 end
                }
            });
            Intent intent = new Intent();
            intent.putExtra("action", "update");
            intent.putExtra("msg", path);
            intent.setAction("location.action");//intent.setAction("location.reportsucc");
            sendBroadcast(intent);
            //发送一个更新UI广播给Activity(在Activity调用之前还得先获得service里更新后的变量)

        } catch (IOException e) {
            e.printStackTrace();
            Intent intent = new Intent();
            intent.putExtra("action", "delete");
            intent.putExtra("msg", path);
            intent.setAction("location.action");
            sendBroadcast(intent);
            //发送一个删除信息广播给Activity(在Activity调用之前还得先获得service里更新后的变量)
        }
    }
    //********************************************分割线：监听器**************************************************
    //监听播放器播放完成 自动切换下一首
    private void initplayerListener(){
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                play_next();
            }
        });
    }
    //********************************************分割线：其他函数**************************************************
    //播放下一首函数
    public void play_next(){
        if(mode==0) {number++;if(number>=list.size()) number=0;}//顺序
        else if(mode==1){if(number>=list.size()) number=0;}//单曲循环
        else if(mode==2) {number=(int)(list.size()*Math.random());}//随机
        ////切换D_bg_SurfaceView中背景布效果
        String p = list.get(number).path;//获得歌曲的地址
        play(p);
    }

    //时间转换函数 用于显示
    public String getTime(int time){
        int min=time/60;
        int sec=time%60;
        if(sec<10) return min+":0"+sec;
        return min+":"+sec;
    }

    //TODO 实现线控广播接口的抽象方法
    //单击：播放/暂停
    public void playOrPause(){
        Log.d(TAG, "playOrPause: 耳机线控-单击，暂停/继续");
        if (player.isPlaying()) player.pause();
        else player.start();
    }
    //双击：下一首
    public void playNext(){
        Log.d(TAG, "playOrPause: 耳机线控-双击，下一首");
        play_next();
    }
    //三击：开启/关闭背景动画
    public void playPrevious(){
        Log.d(TAG, "playOrPause: 耳机线控-三击，???");
        //if(D_bg_SurfaceView.view_switch) D_bg_SurfaceView.view_switch=false;
        //else D_bg_SurfaceView.view_switch=true;
    }

}
