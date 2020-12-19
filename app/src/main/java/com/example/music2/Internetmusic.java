package com.example.music2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//2020/9/6
//实现主要参考https://blog.csdn.net/qq_42813491/article/details/88544975?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-9.channel_param&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-9.channel_param

public class Internetmusic extends AppCompatActivity implements View.OnClickListener,HeadsetButtonReceiver.onHeadsetListener {
    //附加搜素功能，一次返回最多20个类似项目，同时保留最近20首歌的信息且这20里又听过后置为最新 用Vector?
    //TODO 已暂存的歌曲播放可以直接使用ID搜索
    //后续追加下载功能？

    //当前任务 1、联网(权限)获取信息，使用信息 2、解析迭代器并存储
    private static final String TAG = "Internetmusic";
    HeadsetButtonReceiver headsetButtonReceiver;
    private Music_Datebase helper;
    private SQLiteDatabase mydb;
    private EditText edit1;
    private Timer timer;//定时器
    ListView mylist;
    List<Song> list;
    TextView tv1,tv1_1,tv2,time_now,time_end;
    SeekBar seekBar;
    Button search,
            play,pause,next,like,Xlike,
            all_or_recently,play_mode,search_mode,exit;
    String testpath;//记录地址
    boolean isSeekbarChaning;
    int id;//和本地播放的number不同，此处用id作为唯一标识
    String path;
    String song;
    String singer;
    int time;//用于暂存当前播放歌曲的信息，用于收藏时的信息

    int mode;//0 1 2
    String modes[]={"模式:顺序","模式:循环","模式:随机"};
    int auto;//0手动 1 自动直到搜到歌为止
    int list_kind;//0 1
    String list_kinds[]={"列表:收藏","列表:历史"};
    int delay1;//用于控制切歌间隔，防止点击过快出错

    //备注：Sharepreference保存了id和mode值 + 进度条?
    private SharedPreferences sp;//

    //联网功能
    private Handler handler1,handler2;
    String address="";//记录有效地址
    private static final String URL="https://api.imjad.cn/cloudmusic/?type=song&id=";//  返回指定ID歌曲信息 包含链接
    private static final String URL2="https://api.imjad.cn/cloudmusic/?type=detail&id=";// 返回对应ID歌曲名称等信息
    private static final String JSON_MESSAGE = "jsonResult";
    private static final String FINAL_MESSAGE = "finalResult";
    private static final String DETAIL_MESSAGE = "detailResult";

    private MediaPlayer player=new MediaPlayer();
    private D_bg_SurfaceView Dbg=null;

    /**************************************************分割线*********************************************/
    //初始化部分

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internetmusic);
        Dbg=(D_bg_SurfaceView)findViewById(R.id.dbg);

        //新建耳机线控广播接收器的实例对象
        headsetButtonReceiver=new HeadsetButtonReceiver(this);
        headsetButtonReceiver.setOnHeadsetListener(this);

        this.initDB();
        this.initValue();
        this.initHandler();
        this.initplayerListener();
        this.initSeekBarListener();

        //从数据库里导出已收藏歌曲信息
        mylist = (ListView) findViewById(R.id.mylist);
        list = new ArrayList<>();
        //list = Utils.getmusic(this);
        list=getDatebase();
        //实现关键：将数据库内容导出到List里即可
        if(list==null){
            /*
            Song empty=new Song();
            empty.init_Song("没有数据");
            list = new ArrayList<>();
            list.add(empty);*/

            /*此处改成把初始数据入库并重新扫描*/
            Add_InitSong();
            list=getDatebase();
        }
        Internetmusic.MyAdapter myAdapter = new Internetmusic.MyAdapter(Internetmusic.this, list);
        mylist.setAdapter(myAdapter);
        //
        //给ListView添加点击事件，实现点击哪首音乐就进行播放
        mylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //此处和本地大同小异
                id=list.get(i).ID_Like;
                mythread();
                //String p = list.get(i).path;//获得歌曲的地址
                //play(p);
            }
        });

        Log.d(TAG,"网络音乐界面Oncreate");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        SharedPreferences.Editor editor=sp.edit();
        editor.putInt("id2",id);
        editor.putInt("mode2",mode);
        editor.commit();
        headsetButtonReceiver.unregisterHeadsetReceiver();//注销耳机线控广播
        if(timer!=null) timer.cancel();
        if(player!=null){
            player.stop();
            player.release();
        }
        Log.d(TAG,"网络音乐界面Destroy");
    }
    //Log.d(TAG,"网络音乐界面Oncreate");

    //创建OptionMenu菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //加载资源菜单
        Log.d(TAG,"menu功能加载");
        getMenuInflater ().inflate (R.menu.memu1,menu);  //第一个传入的参数是你创建的menu的名字
        return true;  //一定要return true 才会显示出来
    }

    /**************************************************分割线*********************************************/
    //监听器部分

    //监听播放器播放完成 自动切换下一首
    private void initplayerListener(){
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Next_id();
                //String p = list.get(id).path;//获得歌曲的地址
                mythread();//开启联网线程
            }
        });
    }

    //进度条监听器
    private void initSeekBarListener(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //进度发生改变时会触发
                int time=progress/1000;
                time_now.setText(getTime(time));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //按住SeekBar时会触发
                isSeekbarChaning=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //放开SeekBar时触发
                //跳到此处播放
                isSeekbarChaning=false;
                player.seekTo(seekBar.getProgress());
            }
        });
    }

    //按键监听处理函数
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search://联网搜歌曲
                String ID=edit1.getText().toString();//输入ID
                if(ID.equals(""))Toast.makeText(Internetmusic.this, "请输入歌曲ID", Toast.LENGTH_SHORT).show();
                else {
                    id=Integer.valueOf(ID);
                    mythread();//开启联网线程
                }
                break;
            case R.id.play://改
                if (!player.isPlaying()) {
                    //String p = list.get(id).path;//获得歌曲的地址
                    //play(p);
                    mythread();//开启联网线程
                }
                break;
            case R.id.pause://暂停/继续
                if (player.isPlaying()) player.pause();
                else player.start();
                break;
            case R.id.next://下一首
                Next_id();
                //String p = list.get(id).path;//获得歌曲的地址
                //play(p);
                mythread();//开启联网线程
                break;
            case R.id.like://收藏(入数据库)
                //前置条件：需正在播放有效歌曲
                //需要 path ID 歌名 歌手 时间
                if(player.isPlaying()){
                    String sql1 ="INSERT OR IGNORE INTO " + helper.TABLE_NAME2 + " (path,id,song,singer,time) VALUES ('" + path + "',"+id+" ,'" + song + "','" + singer + "',"+time+")";
                    mydb.execSQL(sql1);
                    Toast.makeText(this,"已收藏,重启或者换列表后刷新",Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(this,"当前曲目无效",Toast.LENGTH_SHORT).show();
                break;
            case R.id.Xlike://取消收藏(出数据库)
                //
                String sql2 ="DELETE FROM " + helper.TABLE_NAME2 +" WHERE id="+ id +" ";  //TODO:  VALUES (xx,xx) 插入所有列数据 需保证格式
                mydb.execSQL(sql2);
                Toast.makeText(this,"已取消收藏,重启或者换列表后刷新",Toast.LENGTH_SHORT).show();
                break;
            case R.id.all_or_recently://播放模式切换 TODO 暂时改成刷新列表/重新扫描
                list = new ArrayList<>();
                list=getDatebase();
                //实现关键：将数据库内容导出到List里即可
                if(list==null){
                    Song empty=new Song();
                    empty.init_Song("没有数据");
                    list = new ArrayList<>();
                    list.add(empty);
                }
                Internetmusic.MyAdapter myAdapter = new Internetmusic.MyAdapter(Internetmusic.this, list);
                mylist.setAdapter(myAdapter);
                break;
            case R.id.play_mode://播放模式切换
                mode++;if(mode>2) mode=0;//0 1 2
                play_mode.setText(modes[mode]);
                break;
            case R.id.search_mode://手动/自动搜索
                auto=1-auto;
                if(auto==0) search_mode.setText("搜索:网上的");
                if(auto==1) search_mode.setText("搜索:收藏的");
                break;
            case R.id.exit://保存退出
                //改成关闭所有活动的退出
                finish();
                break;
            default:break;
        }
    }

    /**************************************************分割线*********************************************/
    //函数部分

    private void initDB() {
        helper = new Music_Datebase(Internetmusic.this);
        mydb = helper.getWritableDatabase();
    }
    //变量初始化
    private void initValue(){
        search=(Button) findViewById(R.id.search);
        play=(Button) findViewById(R.id.play);
        pause=(Button) findViewById(R.id.pause);
        next=(Button) findViewById(R.id.next);
        like=(Button) findViewById(R.id.like);
        Xlike=(Button) findViewById(R.id.Xlike);
        all_or_recently=(Button) findViewById(R.id.all_or_recently);
        play_mode=(Button) findViewById(R.id.play_mode);
        search_mode=(Button) findViewById(R.id.search_mode);
        exit=(Button) findViewById(R.id.exit);

        search.setOnClickListener(this);
        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        next.setOnClickListener(this);
        like.setOnClickListener(this);
        Xlike.setOnClickListener(this);
        all_or_recently.setOnClickListener(this);
        play_mode.setOnClickListener(this);
        search_mode.setOnClickListener(this);
        exit.setOnClickListener(this);

        edit1=(EditText) findViewById(R.id.edit1);
        tv1=(TextView)findViewById(R.id.text01);
        tv1_1=(TextView)findViewById(R.id.text01_1);
        tv2=(TextView)findViewById(R.id.text02);
        time_now=(TextView)findViewById(R.id.time_now);
        time_end=(TextView)findViewById(R.id.time_end);
        seekBar=(SeekBar) findViewById(R.id.seekBar1);
        //变量初始化
        delay1=51;
        sp = getSharedPreferences("zks", MODE_PRIVATE);
        id=sp.getInt("id2",0);//与本地播放区分开来
        mode=sp.getInt("mode2",0);//
        tv1.setText(String.valueOf(id));
        play_mode.setText(modes[mode]);
    }

    //Handler TODO 作用1：传递消息Message  2：子线程通知主线程更新UI
    private void initHandler() {
        handler1 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                //String jsonResult = bundle.getString(JSON_MESSAGE);
                String url = bundle.getString(FINAL_MESSAGE);
                //tv2.setText(jsonResult);
                tv1.setText(""+id);
                if(url.equals("")) {
                    tv1_1.setText("-该ID无相应资源");
                    /*找不到则继续自动搜索 TODO 暂时取消这个功能
                    if(mode!=1&&auto==1){//防止mode1自循环
                        tv1_1.setText("-该ID无相应资源,自动搜索中...");
                        Next_id();
                        //String p = list.get(id).path;//获得歌曲的地址
                        mythread();//开启联网线程
                    }
                    */
                }
                else {
                    play(url);
                    tv1_1.setText("-GET!");
                    tv2.setText("当前播放:");
                    mythread2();
                }
            }
        };
        handler2 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String detail=bundle.getString(DETAIL_MESSAGE);
                //tv2.setText(jsonResult);
                //if(detail.equals("")) ;
                if(tv2.getText().equals("当前播放:")) tv2.setText("当前播放:"+detail);
            }
        };
    }

    //开启OkHttp线程函数
    private void mythread(){
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                try {
                    //String ID=id;//输入ID
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url( URL+id)
                            .build();
                    Response response = client.newCall( request). execute();
                    String responseData = response.body().string();
                    String result = parseJsonObject(responseData);

                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString(JSON_MESSAGE, responseData);
                    bundle.putString(FINAL_MESSAGE, result);
                    msg.setData(bundle);
                    handler1.sendMessage(msg);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    String result = ex.toString();
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString(JSON_MESSAGE, result);
                    msg.setData(bundle);
                    handler1.sendMessage(msg);
                }
            }
        };
        thread1.start();
    }
    private void mythread2(){
        Thread thread2 = new Thread() {
            @Override
            public void run() {
                try {
                    //String ID=edit1.getText().toString();//输入ID
                    OkHttpClient client = new OkHttpClient();
                    Request request2 = new Request.Builder()
                            .url( URL2+id)
                            .build();
                    Response response2 = client.newCall( request2). execute();
                    String responseData2 = response2.body().string();
                    String result2 = parseJsonObject_detail(responseData2);

                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString(DETAIL_MESSAGE, result2);
                    msg.setData(bundle);
                    handler2.sendMessage(msg);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    String result = ex.toString();
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString(DETAIL_MESSAGE, result);
                    msg.setData(bundle);
                    handler2.sendMessage(msg);
                }
            }
        };
        thread2.start();
    }

    //播放音乐函数
    public void play(String path) {
        //tv2.setText("路径:"+path);//显示路径
        this.path=path;//TODO 获取信息到全局
        try {
            // 重置音频文件，防止多次点击会报错
            player.reset();
            // 调用方法传进播放地址
            Uri uri=Uri.parse(path);
            player.setDataSource(this,uri);
            // 异步准备资源，防止卡顿
            player.prepareAsync();
            // 调用音频的监听方法，音频准备完毕后响应该方法进行音乐播放
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    //2020/9/1添加 start ps:getDuration必须准备好才能获取，否则是-1
                    final int duration = player.getDuration();//获取音乐总时间
                    time=duration;//TODO 获取信息到全局
                    seekBar.setMax(duration);//将音乐总时间设置为Seekbar的最大值
                    time_now.setText("0:00");
                    time_end.setText(""+getTime(duration/1000));
                    //if(timer!=null) {timer.cancel();timer=null;}
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(!isSeekbarChaning&&player.isPlaying()){
                                int x=player.getCurrentPosition();
                                seekBar.setProgress(x);//获取当前播放到哪的时间点
                            }
                            delay1+=4;
                            if(delay1>1000) delay1=11;
                        }
                    },0,200);
                    //2020/9/1添加 end
                }
            });
            tv1.setText(String.valueOf(id));//TODO刷新显示

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //时间 分：秒 处理函数
    public String getTime(int time){
        int min=time/60;
        int sec=time%60;
        if(sec<10) return min+":0"+sec;
        return min+":"+sec;
    }

    //处理JSON格式信息函数 获取播放链接信息
    private String parseJsonObject(String jsonStr) throws JSONException {
        /*处理网易云音乐外链接口信息*/
        //复杂JSON处理 参考例子5_2_1
        JSONObject jsonObject1 = new JSONObject(jsonStr);//处理第一个对象{ 符号
        JSONArray jsonArray1 = jsonObject1.optJSONArray("data");//处理数组 [ 符号
        for (int i=0; i<jsonArray1.length(); i++) { //虽然数组大小只有1
            JSONObject jsonObject2 = jsonArray1.optJSONObject(i);

            String id = jsonObject2.optString("id");
            String url = jsonObject2.optString("url");
            String br = jsonObject2.optString("br");
            String size = jsonObject2.optString("size");
            String md5 = jsonObject2.optString("md5");
            String type = jsonObject2.optString("type");
            //.......等信息
            //String result = "id：" + id + "\nurl：" + url +  "\nbr：" + br+  "\nsize：" + size+  "\nmd5：" + md5+  "\ntype：" + type;
            return url;
        }
        return null;
    }

    //处理JSON格式 获取歌名等信息
    private String parseJsonObject_detail(String jsonStr) throws JSONException {
        /*处理网易云音乐外链接口信息*/
        //复杂JSON处理 参考例子5_2_1
        Log.i(TAG, "parseJsonObject_detail: "+jsonStr);
        JSONObject jsonObject1 = new JSONObject(jsonStr);//处理第一个对象{ 符号

        JSONArray jsonArray1 = jsonObject1.optJSONArray("songs");//处理数组 [ 符号
        if(jsonArray1.isNull(0)) return "";
        else{
            JSONObject jsonObject2 = jsonArray1.optJSONObject(0);//进入数组[0] 处理{
            String name = jsonObject2.optString("name");
            String id = jsonObject2.optString("id");

            JSONArray jsonArray2 = jsonObject2.optJSONArray("ar");//处理数组 [ 符号
            JSONObject jsonObject3 = jsonArray2.optJSONObject(0);//进入数组[0] 处理{
            String singer=jsonObject3.optString("name");

            //.......等信息
            String result = id+"-"+name+"-"+singer;
            this.song=name;//TODO 获取信息到全局
            this.singer=singer;//TODO 获取信息到全局
            return result;
        }
    }

    //播放下一首，按模式更改id的值
    public void Next_id(){
        if(mode==0) {id++;if(id>=2147483647) id=0;}//顺序 防溢出
        else if(mode==1){}//单曲循环
        else if(mode==2) {//随机 三分
            id=(int)(2147483647*(1-Math.random()));
            int a=(int)(Math.random()*100);
            if(a>33) id=id%100000000;
            if(a>66) id=id%1000000;
        }
        ////切换D_bg_SurfaceView中背景布效果
        Dbg.item_change();//切换背景布效果
    }

    //数据库内无数据则导入初始数据函数
    public void Add_InitSong(){
        String sql1 = "INSERT OR IGNORE INTO " + helper.TABLE_NAME2 +
                " (id,song,singer,time) SELECT 5271858,'难念的经','周华健',288000" +
                " UNION ALL SELECT 170749,'沧海一声笑','许冠杰',175000"+
                " UNION ALL SELECT 112678,'铁血丹心','罗文 / 甄妮',174000"+
                " UNION ALL SELECT 26134238,'Little Fragments','折戸伸治',124000"+
                " UNION ALL SELECT 792449,'绯色の雪/绯想天','N-tone',295000";
        /*\n
        ID举例:
        \nid=5271858    难念的经(天龙八部)
        \nid=170749     沧海一声笑(笑傲江湖)
        \nid=555984413  神话情话(神雕侠侣)
        \nid=1444652190 心爱醉春风(倚天屠龙记)挂了
        \nid=112678     铁血丹心(射雕英雄传)VIP了
        \nid=26134238   Little Fragments
        \nid=792449     绯色の雪/绯想天
        */
        mydb.execSQL(sql1);
    }

    //扫描获取数据库里的歌曲信息函数 返回List
    public List<Song> getDatebase(){
        List<Song> list;
        list = new ArrayList<>();
        String sql="";
        //if(list_kind==2)
            sql="SELECT * FROM " + helper.TABLE_NAME2;//网络歌单

        Cursor cursor = mydb.rawQuery(sql, null);   //cuisor 光标，也称游标 https://blog.csdn.net/android_zyf/article/details/53420267
        if (cursor.getCount() == 0) {
            return null;
        }
        else {
            while (cursor.moveToNext()) {
                Song song=new Song();
                //path VARCHAR PRIMARY KEY,uid INTEGER,favourite INTEGER, name VARCHAR, singer VARCHAR,time INTEGER
                //columnindex0是UID
                song.path= cursor.getString(1);
                song.ID_Like = cursor.getInt(2);
                song.song = cursor.getString(3);//TODO 改成0可以查看uid的情况
                song.singer=cursor.getString(4);
                song.duration=cursor.getInt(5);
                list.add(song);
            }
        }
        //if(list_kind==2) Collections.reverse(list);//倒序输出List以保证最前面的是最新 TODO Collections.reverse(list)
        return list;
        //需处理空的情况?
    }

    /**************************************************分割线*********************************************/
    //其他部分

     //TODO  写一个适配器把内容映射到ListView中
    class MyAdapter extends BaseAdapter {

        Context context;
        List<Song> list;

        public MyAdapter(Internetmusic internetmusic, List<Song> list) {
            this.context = internetmusic;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            Myholder myholder;

            if (view == null) {
                myholder = new Myholder();
                view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.text, null);

                //自加一条 喜爱
                myholder.t_favourite=view.findViewById(R.id.t_favourite);
                myholder.t_position = view.findViewById(R.id.t_postion);
                myholder.t_song = view.findViewById(R.id.t_song);
                myholder.t_singer = view.findViewById(R.id.t_singer);
                myholder.t_duration = view.findViewById(R.id.t_duration);

                view.setTag(myholder);

            } else {
                myholder = (Myholder) view.getTag();
            }
            myholder.t_position.setText(i + 1 + "");
            //改
            //if((i+1)%5==0) myholder.t_favourite.setText("♥");
            //else myholder.t_favourite.setText("");
            myholder.t_song.setText( (""+list.get(i).song).toString()+("-"+list.get(i).singer).toString());//这里把歌和歌手放一起
            myholder.t_singer.setText( "id="+list.get(i).ID_Like);//这里改成ID
            String time = Utils.formatTime(list.get(i).duration);
            myholder.t_duration.setText(time);



            return view;
        }

        class Myholder {
            //改
            TextView t_favourite,t_position, t_song, t_singer, t_duration;
        }


    }

    //TODO 实现线控广播接口的抽象方法
    //单击：播放/暂停
    public void playOrPause(){
        Toast.makeText(this,"单击，暂停/继续",Toast.LENGTH_SHORT).show();
        if (player.isPlaying()) player.pause();
        else player.start();
    }
    //双击：下一首
    public void playNext(){
        Toast.makeText(this,"双击，下一首",Toast.LENGTH_SHORT).show();
        Next_id();
        mythread();//开启联网线程
    }
    //三击：开启/关闭背景动画
    public void playPrevious(){
        if(D_bg_SurfaceView.view_switch) D_bg_SurfaceView.view_switch=false;
        else D_bg_SurfaceView.view_switch=true;
        Toast.makeText(this,"三击，背景动画开/关",Toast.LENGTH_SHORT).show();

    }

}