package com.example.music2;

import androidx.appcompat.app.AppCompatActivity;
        import androidx.core.app.ActivityCompat;
        import androidx.core.content.ContextCompat;

        import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
        import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
        import android.os.Bundle;
        import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
        import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
        import android.widget.BaseAdapter;
        import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
        import android.widget.SeekBar;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.io.File;
        import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
        import java.util.List;
        import java.util.Timer;
        import java.util.TimerTask;

//implements 监听类，可以更简洁导用不同按键控件？ 需实现onClick(View v)
public class Localmusic extends AppCompatActivity implements View.OnClickListener {
    //
    private static final String TAG = "Localmusic";
    //HeadsetButtonReceiver headsetButtonReceiver;
    private Music_Datebase helper;
    private SQLiteDatabase mydb;
    private Music_Service music_service;//绑定的service的对象
    LocationReceiver locationReceiver;//本地广播接收器
    //private Timer timer;//定时器
    ListView mylist;
    List<Song> list;
    static TextView tv1,tv2,time_now,time_end;
    static SeekBar seekBar;
    Button play,pause,next,like,Xlike,
            all_like_recently,play_mode,scan,exit;
    static String testpath;//记录地址 用于添加喜爱时作为关键字表示
    static boolean isSeekbarChaning;
    int number;
    int mode;//0 1 2
    String modes[]={"模式:顺序","模式:循环","模式:随机"};
    int list_kind;//0 1 2
    String list_kinds[]={"列表:全部","列表:喜爱","列表:历史"};

    static int delay1;//用于控制切歌间隔，防止点击过快出错

    //备注：Sharepreference保存了number和mode值 + 进度条?
    private SharedPreferences sp;//
    //private MediaPlayer player=new MediaPlayer();
    private D_bg_SurfaceView Dbg=null;

    /**************************************************分割线：初始化部分*********************************************/

    //连接Activity和Service
    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            music_service=((Music_Service.MyBinder)service).getService();//返回一个Music_Service对象
            init_ServiceValue();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //https://blog.csdn.net/tnt2011cpp/article/details/35557631?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-2.channel_param&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-2.channel_param
        setContentView(R.layout.activity_localmusic);
        Dbg=(D_bg_SurfaceView)findViewById(R.id.dbg);

        /*注册本地广播接收器*/
        locationReceiver=new LocationReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("location.action");
        registerReceiver(locationReceiver,filter);

        /*新建耳机线控广播接收器的实例对象
        headsetButtonReceiver=new HeadsetButtonReceiver(this);
        headsetButtonReceiver.setOnHeadsetListener(this);
        */
        this.initDB();
        this.initValue();
        //this.initplayerListener();
        this.initSeekBarListener();

        mylist = (ListView) findViewById(R.id.mylist);
        list = new ArrayList<>();
        //list = Utils.getmusic(this);
        list=getDatebase();

        //实现关键：将数据库内容导出到List里即可
        if(list==null){
            Song empty=new Song();
            empty.init_Song("没有数据");
            list = new ArrayList<>();
            list.add(empty);
        }
        MyAdapter myAdapter = new MyAdapter(Localmusic.this, list);
        mylist.setAdapter(myAdapter);

        //绑定service
        Intent bingIntent=new Intent(Localmusic.this,Music_Service.class);
        bindService(bingIntent,connection,BIND_AUTO_CREATE);
        Log.d(TAG, "onCreate: 绑定Music_Service");

        //给ListView添加点击事件，实现点击哪首音乐就进行播放
        mylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                number=i;
                music_service.number=number;
                String p = list.get(i).path;//获得歌曲的地址
                music_service.play(p);
            }
        });

        Log.d(TAG,"本地音乐界面启动");
    }

    @Override
    protected void onDestroy(){
        unbindService(connection);//取消绑定service
        unregisterReceiver(locationReceiver);//卸载广播接收器
        //记录数值到SharePreferences
        SharedPreferences.Editor editor=sp.edit();
        editor.putInt("number",number);
        editor.putInt("mode",mode);
        editor.putInt("list_kind",list_kind);
        editor.commit();
        //
        //headsetButtonReceiver.unregisterHeadsetReceiver();//注销耳机线控广播

        /*内存释放
        if(timer!=null) timer.cancel();
        if(player!=null){
            player.stop();
            player.release();
        }*/
        Log.d(TAG,"本地音乐界面Destroy");
        super.onDestroy();
    }

    //创建OptionMenu菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //加载资源菜单
        Log.d(TAG,"menu功能加载");
        getMenuInflater ().inflate (R.menu.memu1,menu);  //第一个传入的参数是你创建的menu的名字
        return true;  //一定要return true 才会显示出来
    }
    /**********************************************************************分割线：播放音乐更新UI*****************************************************************/
    //Activity更新UI  信号通过定时器检测来刷新？
    public void play_UI_update(String path){
        number=music_service.number;
        Dbg.item_change();//切换背景布效果
        tv1.setText(String.valueOf(number+1));//TODO刷新显示
        //列表滚动到指定位置
        //mylist.setSelection(number);//瞬间跳到指定位置  方法1:listview.setSelection(position);跳到指定位置并置顶
        mylist.smoothScrollToPosition(number);// 方法2:listview.smoothScrollToPosition(position);自动滚动,当指定位置出现即停止 加入第二个参数指定距top的距离?  距离过大时会中途停止?   TODO 若要改变速度，需要查看源码并重新自定义一个方法

        //更新最近播放 数据库表   先删除(若已有)，再插入最后，最后删除超过20条以上时的前几条记录
        String sql1="DELETE FROM " + helper.TABLE_NAME3 + " WHERE path='"+ path +"' ";
        String sql2="INSERT into " + helper.TABLE_NAME3 + "(path,favourite, song, singer,time) select path,favourite, song, singer,time from " + helper.TABLE_NAME+" WHERE path='"+ path +"'";//uid不能复制
        mydb.execSQL(sql1);
        mydb.execSQL(sql2);
        //刷新显示 （仅当显示历史表的情况下）
        if(list_kind==2){
            list = new ArrayList<>();
            list=getDatebase();
            if(list==null){
                Song empty=new Song();
                empty.init_Song("没有数据");
                list = new ArrayList<>();
                list.add(empty);
            }
            Localmusic.MyAdapter myAdapter = new Localmusic.MyAdapter(Localmusic.this, list);
            mylist.setAdapter(myAdapter);
            tv1.setText("1");//历史表总是播放序号1 的歌曲
        }
    }
    //Activity删除无效数据库数据  信号通过定时器检测来刷新？
    public void play_error_delete(String path){
        //路径无效的情况下 删除数据库中该数据
        if(path.equals("")||path.equals("没有数据")){
            Toast.makeText(this, "无可播放", Toast.LENGTH_SHORT).show();
        }
        else {
            String sql ="DELETE FROM " + helper.TABLE_NAME + " WHERE path='"+ path +"' ";  //TODO:  VALUES (xx,xx) 插入所有列数据 需保证格式
            mydb.execSQL(sql);
            Toast.makeText(this, "无效路径已删除,重启或者换列表后刷新", Toast.LENGTH_SHORT).show();
            //play_next();//危！可能死循环
        }
    }
    /**********************************************************************分割线：监听器*****************************************************************/

    /*监听播放器播放完成 自动切换下一首
    private void initplayerListener(){
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                play_next();
            }
        });
    }*/

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
                music_service.player.seekTo(seekBar.getProgress());
            }
        });
    }

    //按键事件监听
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                if (!music_service.player.isPlaying()) {
                    String p = list.get(number).path;//获得歌曲的地址
                    music_service.play(p);
                }
                break;
            case R.id.pause://暂停/继续
                if (music_service.player.isPlaying()) music_service.player.pause();
                else music_service.player.start();
                break;
            case R.id.next://下一首
                if(delay1>10){//防止按的太快出错？
                    delay1=0;
                    music_service.play_next();
                }
                break;
            case R.id.like://添加喜爱
                String sql1 ="UPDATE " + helper.TABLE_NAME +" SET favourite=1"+ " WHERE path='"+ testpath +"' ";  //TODO:  VALUES (xx,xx) 插入所有列数据 需保证格式
                mydb.execSQL(sql1);
                Toast.makeText(this,"添加喜爱,重启或者换列表后刷新",Toast.LENGTH_SHORT).show();
                break;
            case R.id.Xlike://取消喜爱
                String sql2 ="UPDATE " + helper.TABLE_NAME +" SET favourite=0"+ " WHERE path='"+ testpath +"' ";  //TODO:  VALUES (xx,xx) 插入所有列数据 需保证格式
                mydb.execSQL(sql2);
                Toast.makeText(this,"取消喜爱,重启或者换列表后刷新",Toast.LENGTH_SHORT).show();
                break;
            case R.id.all_like_recently://播放列表切换：全部/喜爱/最近
                list_kind++;if(list_kind>2) list_kind=0;//0 1 2
                all_like_recently.setText(list_kinds[list_kind]);

                list = new ArrayList<>();
                list=getDatebase();
                if(list==null){
                    Song empty=new Song();
                    empty.init_Song("没有数据");
                    list = new ArrayList<>();
                    list.add(empty);
                }
                music_service.list=list;//TODO 注：服务里不用list_kind值，相对的每次改变列表都要刷新list
                MyAdapter myAdapter = new MyAdapter(Localmusic.this, list);
                mylist.setAdapter(myAdapter);

                break;
            case R.id.play_mode://播放模式切换：顺序/随机/循环
                /*
                if (player.isPlaying()) {
                    player.reset(); //停止播放
                    initMediaPlayer();
                }*/
                mode++;if(mode>2) mode=0;//0 1 2
                music_service.mode=mode;
                play_mode.setText(modes[mode]);
                break;
            case R.id.scan://扫描本地 并更新数据库(耗时吗?)
                //Toast.makeText(this,"更新中，更新完请重启或切换列表",Toast.LENGTH_SHORT).show();
                LocalMusic_scan_update();
                break;
            case R.id.exit://保存退出
                //删除数据库历史播放超过20的条目  1、先获取条目总数 2、判断并删除多余数据 3、重置自增序号
                String sql="SELECT * FROM " + helper.TABLE_NAME3;//最近歌单
                Cursor cursor = mydb.rawQuery(sql, null);
                int all=cursor.getCount()-20;
                if(all>0){
                    String sql11="DELETE FROM " + helper.TABLE_NAME3 +
                            " WHERE uid in (SELECT uid from " + helper.TABLE_NAME3 + " order by uid asc limit 0,"+all+")";
                    mydb.execSQL(sql11);
                    //重置自增序号(防溢出)
                    String sql12="DELETE FROM " + helper.TABLE_NAME4;//清空备用表
                    String sql13="INSERT into " + helper.TABLE_NAME4 + "(path,AorB, song, singer,time) select path,favourite, song, singer,time from " + helper.TABLE_NAME3;//复制历史表到备用表
                    String sql14="DELETE FROM " + helper.TABLE_NAME3;//清空历史表
                    String sql15="UPDATE sqlite_sequence SET seq = 0 WHERE name ='"+helper.TABLE_NAME3+"'";//重置历史表自增值
                    String sql16="INSERT into " + helper.TABLE_NAME3 + "(path,favourite, song, singer,time) select path,AorB, song, singer,time from " + helper.TABLE_NAME4;//复制备用表到历史表
                    mydb.execSQL(sql12);mydb.execSQL(sql13);mydb.execSQL(sql14);mydb.execSQL(sql15);mydb.execSQL(sql16);
                }
                //改成关闭所有活动的退出
                finish();
                break;

            default:break;
        }
    }

    //当你选中某个Menu时触发的事件监听 TODO menu的设定在res-menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG,"监听到了Menu事件,对应ID为:"+item.getItemId ());
        switch (item.getItemId ()){            //获取Id
            case R.id.test:
                Toast.makeText (this,"功能测试",Toast.LENGTH_SHORT).show ();
                break;
            case R.id.view_switch:
                if(D_bg_SurfaceView.view_switch) {
                    D_bg_SurfaceView.view_switch=false;
                    Toast.makeText (this,"背景动画关闭",Toast.LENGTH_SHORT).show ();
                }
                else {
                    D_bg_SurfaceView.view_switch=true;
                    D_bg_SurfaceView.flag=true;
                    Toast.makeText (this,"背景动画开启",Toast.LENGTH_SHORT).show ();
                }
                break;
            case R.id.exit:
                //删除数据库历史播放超过20的条目  1、先获取条目总数 2、判断并删除多余数据 3、重置自增序号
                String sql="SELECT * FROM " + helper.TABLE_NAME3;//最近歌单
                Cursor cursor = mydb.rawQuery(sql, null);
                int all=cursor.getCount()-20;
                if(all>0){
                    String sql11="DELETE FROM " + helper.TABLE_NAME3 +
                            " WHERE uid in (SELECT uid from " + helper.TABLE_NAME3 + " order by uid asc limit 0,"+all+")";
                    mydb.execSQL(sql11);
                    //重置自增序号(防溢出)
                    String sql12="DELETE FROM " + helper.TABLE_NAME4;//清空备用表
                    String sql13="INSERT into " + helper.TABLE_NAME4 + "(path,AorB, song, singer,time) select path,favourite, song, singer,time from " + helper.TABLE_NAME3;//复制历史表到备用表
                    String sql14="DELETE FROM " + helper.TABLE_NAME3;//清空历史表
                    String sql15="UPDATE sqlite_sequence SET seq = 0 WHERE name ='"+helper.TABLE_NAME3+"'";//重置历史表自增值
                    String sql16="INSERT into " + helper.TABLE_NAME3 + "(path,favourite, song, singer,time) select path,AorB, song, singer,time from " + helper.TABLE_NAME4;//复制备用表到历史表
                    mydb.execSQL(sql12);mydb.execSQL(sql13);mydb.execSQL(sql14);mydb.execSQL(sql15);mydb.execSQL(sql16);
                }
                //改成关闭所有活动的退出
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected (item);
    }

    /**************************************************分割线：函数*********************************************/
    //数据库初始化
    private void initDB() {
        helper = new Music_Datebase(Localmusic.this);
        mydb = helper.getWritableDatabase();
    }

    //变量初始化
    private void initValue(){
        play=(Button) findViewById(R.id.play);
        play_mode=(Button) findViewById(R.id.play_mode);
        pause=(Button) findViewById(R.id.pause);
        next=(Button) findViewById(R.id.next);
        like=(Button) findViewById(R.id.like);
        Xlike=(Button) findViewById(R.id.Xlike);
        exit=(Button) findViewById(R.id.exit);
        scan=(Button) findViewById(R.id.scan);
        all_like_recently=(Button) findViewById(R.id.all_like_recently);

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        play_mode.setOnClickListener(this);
        next.setOnClickListener(this);
        like.setOnClickListener(this);
        Xlike.setOnClickListener(this);
        exit.setOnClickListener(this);
        scan.setOnClickListener(this);
        all_like_recently.setOnClickListener(this);

        tv1=(TextView)findViewById(R.id.text01);
        tv2=(TextView)findViewById(R.id.textView1);
        time_now=(TextView)findViewById(R.id.time_now);
        time_end=(TextView)findViewById(R.id.time_end);
        seekBar=(SeekBar) findViewById(R.id.seekBar1);
        //变量初始化
        delay1=51;
        sp = getSharedPreferences("zks", MODE_PRIVATE);
        number=sp.getInt("number",0);//
        mode=sp.getInt("mode",0);//
        list_kind=sp.getInt("list_kind",0);//
        tv1.setText(String.valueOf(number+1));
        play_mode.setText(modes[mode]);
        all_like_recently.setText(list_kinds[list_kind]);
    }

    //服务变量初始化
    private void init_ServiceValue(){
        music_service.number=number;
        music_service.list=list;
        music_service.mode=mode;
        music_service.list_kind=list_kind;//注：这个Service用不到，但是每次改变都要刷新服务的list
    }
    /*暂时不用...... 预加载，防闪退（仅限于zi个人的手机）
    private void initMediaPlayer(){
        try {
            //获取手机本身存储根目录Environment.getExternalStoragePublicDirectory("")
            //sd卡根目录Environment.getExternalStorageDirectory() 路径是/storage/emulated/0

            //初始化 本地歌曲-采薇
            File file=new File(Environment.getExternalStorageDirectory(),"/storage/emulated/0/music/music.mp3");  //实际使用的有效路径名为/storage/emulated/0/storage/emulated/0/music/music.mp3
            player.setDataSource(file.getPath());

            //初始化 网络歌曲-难念的经  备注，此地址是会变动的
            //参考：https://blog.csdn.net/qq_42813491/article/details/88544975?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-9.channel_param&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-9.channel_param
            //Uri uri=Uri.parse("https://m8.music.126.net/20200904184148/1ce5a0e6376b0784b831aaabe5797118/ymusic/obj/w5zDlMODwrDDiGjCn8Ky/3058370828/d73a/d8a5/af8a/3c9cea80c1dd2757f5b234d5ea47918e.mp3");
            //player.setDataSource(this,uri);

            player.prepare();

        }catch (Exception e){
            e.printStackTrace();
        }
    }*/

    //权限申请函数
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //initMediaPlayer();
                }
                else{
                    Toast.makeText(this,"拒绝权限将无法使用程序",Toast.LENGTH_SHORT).show();
                    finish();//关闭活动(程序)
                }
                break;
            default:
        }
    }



    //时间转换函数 用于显示
    public String getTime(int time){
        int min=time/60;
        int sec=time%60;
        if(sec<10) return min+":0"+sec;
        return min+":"+sec;
    }

    /*播放下一首函数
    public void play_next(){
        if(mode==0) {number++;if(number>=list.size()) number=0;}//顺序
        else if(mode==1){if(number>=list.size()) number=0;}//单曲循环
        else if(mode==2) {number=(int)(list.size()*Math.random());}//随机
        ////切换D_bg_SurfaceView中背景布效果
        Dbg.item_change();//切换背景布效果

        String p = list.get(number).path;//获得歌曲的地址
        play(p);
    }*/

    //扫描获取数据库里的歌曲信息 返回List
    public List<Song> getDatebase(){
        List<Song> list;
        list = new ArrayList<>();
        String sql="";
        if(list_kind==2) sql="SELECT * FROM " + helper.TABLE_NAME3;//历史歌单
        else if(list_kind==1) sql="SELECT * FROM " + helper.TABLE_NAME+" WHERE favourite=1";//喜爱歌单
        else sql="SELECT * FROM " + helper.TABLE_NAME;//全部歌单

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
            /*以下会引起listview里多一条无效数据
            if(list_kind==2){
                Song song=new Song();
                song.song = "注:最下方为最近播放";
                list.add(song);
            }
            */
        }
        if(list_kind==2) Collections.reverse(list);//倒序输出List以保证最前面的是最新 TODO Collections.reverse(list)
        return list;
        //需处理空的情况?
    }

    //扫描本地歌曲并导入数据库(不重复)
    public void LocalMusic_scan_update(){
        //动态申请权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions (this, new String[]{
                    Manifest.permission. WRITE_EXTERNAL_STORAGE }, 1);
        }
        else {
            List<Song> list = Utils.getmusic(this);
            for(int i=0;i<list.size();i++){
                String path=list.get(i).path;
                String song=list.get(i).song;
                String singer=list.get(i).singer;
                int time=list.get(i).duration;
                /*
                还有一条favourite默认为0
                数据库添加数据，条件：当该path数据库里没有时
                String sql = "INSERT INTO " + helper.TABLE_NAME + " (id,name,major) VALUES (" + path + ",'" + name + "','" + major + "')";        //字符串需加单引号    TODO:(xx,xx) VALUES (xx,xx) 插入部分列数据
                insert or replace  每次执行时，如果不存在，则添加，如果存在，则更新。
                insert or ignore  每次执行时，如果不存在，则添加，如果存在，则不操作。
                */
                String sql ="INSERT OR IGNORE INTO " + helper.TABLE_NAME + " (path,favourite,song,singer,time) VALUES ('" + path + "',0 ,'" + song + "','" + singer + "',"+time+")";  //TODO:  VALUES (xx,xx) 插入所有列数据 需保证格式
                //" WHERE not exists (select * from " + helper.TABLE_NAME + " where " + helper.TABLE_NAME + ".path ="+ path + ")";          //TODO:  注-Insert语句中不能加where判断！
                mydb.execSQL(sql);
                //String test = "INSERT INTO " + TABLE_NAME + " (path,name,singer,time) VALUES ('"+path+"','采薇','HITA',100)";//传入参数需这种特殊格式
                //db.execSQL(test);
            }
            //重置自增序号(防溢出)
            String sql12="DELETE FROM " + helper.TABLE_NAME4;//清空备用表
            String sql13="INSERT into " + helper.TABLE_NAME4 + "(path,AorB, song, singer,time) select path,favourite, song, singer,time from " + helper.TABLE_NAME;//复制主表到备用表
            String sql14="DELETE FROM " + helper.TABLE_NAME;//清空主表
            String sql15="UPDATE sqlite_sequence SET seq = 0 WHERE name ='"+helper.TABLE_NAME+"'";//重置主表自增值
            String sql16="INSERT into " + helper.TABLE_NAME + "(path,favourite, song, singer,time) select path,AorB, song, singer,time from " + helper.TABLE_NAME4;//复制备用表到主表
            mydb.execSQL(sql12);mydb.execSQL(sql13);mydb.execSQL(sql14);mydb.execSQL(sql15);mydb.execSQL(sql16);
            Toast.makeText(this,"更新完毕,重启或者换列表后刷新",Toast.LENGTH_SHORT).show();
            //initMediaPlayer(); // 初始化MediaPlayer
        }

    }
    ;
    /**************************************************分割线：其他*********************************************/

    // TODO 创建内部类做为广播接收器接收服务发来的消息并立即处理 注：需要在onCreate里注册
    public class LocationReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context,Intent intent){
            String intentAction=intent.getAction();
            if(intentAction.equals("location.action")) {
                Bundle bundle=intent.getExtras();
                String action=bundle.getString("action");
                String path=bundle.getString("msg");
                if(action.equals("update")) play_UI_update(path);
                else if(action.equals("delete")) play_error_delete(path);
            }
        }
    }
    // TODO  写一个适配器把内容映射到ListView中
    class MyAdapter extends BaseAdapter {

        Context context;
        List<Song> list;

        public MyAdapter(Localmusic localmusic, List<Song> list) {
            this.context = localmusic;
            this.list = list;
        }

        @Override
        //得到数据的行数
        public int getCount() {
            return list.size();
        }

        @Override
        //根据position得到某一行的记录
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        //得到某一条记录的ID
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
            if(list.get(i).ID_Like==1) myholder.t_favourite.setText("♥");
            else myholder.t_favourite.setText("");
            myholder.t_song.setText( (""+list.get(i).song).toString());
            myholder.t_singer.setText( (""+list.get(i).singer).toString());
            String time = Utils.formatTime(list.get(i).duration);//时间数据 需处理格式后显示 调用utils的方法
            myholder.t_duration.setText(time);

            return view;
        }

        class Myholder {
            //改
            TextView t_favourite,t_position, t_song, t_singer, t_duration;
        }

    }

    /*TODO 实现线控广播接口的抽象方法
    //单击：播放/暂停
    public void playOrPause(){
        Toast.makeText(this,"单击，暂停/继续",Toast.LENGTH_SHORT).show();
        if (player.isPlaying()) player.pause();
        else player.start();
    }
    //双击：下一首
    public void playNext(){
        Toast.makeText(this,"双击，下一首",Toast.LENGTH_SHORT).show();
        play_next();
    }
    //三击：开启/关闭背景动画
    public void playPrevious(){
        if(D_bg_SurfaceView.view_switch) D_bg_SurfaceView.view_switch=false;
        else D_bg_SurfaceView.view_switch=true;
        Toast.makeText(this,"三击，背景动画开/关",Toast.LENGTH_SHORT).show();
    }
    */

}
