package com.example.music2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

//Android耳机线控-播放/暂停/上一曲/下一曲 https://blog.csdn.net/u010005281/article/details/79550492
//使用方法：
// 1、在oncreate()里声明广播对象
// 2、在ondestory()里注销广播
// 3、实现接口的方法，具体的功能

public class HeadsetButtonReceiver extends BroadcastReceiver {
    private Context context;
    private Timer timer = new Timer();
    private static int clickCount;
    private static onHeadsetListener headsetListener;

    public HeadsetButtonReceiver(){
        super();
    }

    public HeadsetButtonReceiver(Context ctx){
        super();
        context = ctx;
        headsetListener = null;
        registerHeadsetReceiver();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            //Toast.makeText(context,"test",Toast.LENGTH_SHORT).show();
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                clickCount = clickCount + 1;
                if(clickCount == 1){
                    //Toast.makeText(context,"test1",Toast.LENGTH_SHORT).show();
                    HeadsetTimerTask headsetTimerTask = new HeadsetTimerTask();
                    timer.schedule(headsetTimerTask,500);//第一次单击松开后等待1秒
                }
            } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {//音量+键
                //Toast.makeText(context,"test2",Toast.LENGTH_SHORT).show();
                handler.sendEmptyMessage(2);
            } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {//音量-键
                //Toast.makeText(context,"test3",Toast.LENGTH_SHORT).show();
                handler.sendEmptyMessage(3);
            }
        }
    }

    class HeadsetTimerTask extends TimerTask {
        @Override
        public void run() {
            try{
                //Toast.makeText(context,"test4",Toast.LENGTH_SHORT).show(); //这句话放这反而会报异常
                if(clickCount==1){
                    handler.sendEmptyMessage(1);
                }else if(clickCount==2){
                    handler.sendEmptyMessage(2);
                }else if(clickCount>=3){
                    handler.sendEmptyMessage(3);
                }
                clickCount=0;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                //Toast.makeText(context,"test8",Toast.LENGTH_SHORT).show(); //这句话放这反而会报异常
                if (msg.what == 1) {
                    headsetListener.playOrPause();
                }else if(msg.what == 2){
                    headsetListener.playNext();
                }else if(msg.what == 3){
                    headsetListener.playPrevious();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    //音乐播放处，实现接口，完成线控操作
    interface onHeadsetListener{
        void playOrPause();//单击：播放/暂停
        void playNext();//双击：下一首
        void playPrevious();//三击：上一首(暂不实现)
    }

    public void setOnHeadsetListener(onHeadsetListener newHeadsetListener){
        headsetListener = newHeadsetListener;
    }

    public void registerHeadsetReceiver() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        ComponentName name = new ComponentName(context.getPackageName(), HeadsetButtonReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(name);
    }

    public void unregisterHeadsetReceiver(){
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        ComponentName name = new ComponentName(context.getPackageName(), HeadsetButtonReceiver.class.getName());
        audioManager.unregisterMediaButtonEventReceiver(name);
    }
}