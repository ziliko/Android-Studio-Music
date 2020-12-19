package com.example.music2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zks on 2020/9/20
 */
public class Music_Datebase extends SQLiteOpenHelper {
    public static final int VERSION = 1;
    public static final String DB_NAME = "Music_Datebase";
    public static final String TABLE_NAME = "All_Music";//本地记录总表：仅当重新扫描并且有新的歌曲时  增    FIXME（需处理歌曲无效的情况 删 且后面的序号全都要减一？） 解决方案，取消序号，使用时取出再标号
    //public static final String TABLE_NAME2 = "favourite_Music";//随时  当点击爱心时    改  TODO 可以不需要，只需查询时设置条件即可
    public static final String TABLE_NAME2 = "Internet_Music";//网络记录总表 以ID为
    public static final String TABLE_NAME3 = "recently_Music";//本地历史表：当启动时读取一次，并在正常退出时更新一次 队列

    public static final String TABLE_NAME4 = "copy_Music";//备用数据库，作为复制粘贴的中转站


    String path="/storage/emulated/0/storage/emulated/0/music/music.mp3";

    public Music_Datebase(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    // 数据库创建时调用该方法
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (uid INTEGER primary key AUTOINCREMENT,path VARCHAR UNIQUE,favourite INTEGER, song VARCHAR, singer VARCHAR,time INTEGER)";
        //String test = "INSERT INTO " + TABLE_NAME + " (path,name,singer,time) VALUES ('"+path+"','采薇','HITA',100)";//传入参数需这种特殊格式
        //db.execSQL(test);
        String sql2 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME2 + " (uid INTEGER primary key AUTOINCREMENT,path VARCHAR UNIQUE,id INTEGER UNIQUE, song VARCHAR, singer VARCHAR,time INTEGER)";

        String sql3 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME3 + " (uid INTEGER primary key AUTOINCREMENT,path VARCHAR UNIQUE,favourite INTEGER, song VARCHAR, singer VARCHAR,time INTEGER)";
        String sql4 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME4 +                           " (uid INTEGER,path VARCHAR UNIQUE,AorB INTEGER, song VARCHAR, singer VARCHAR,time INTEGER)";

        db.execSQL(sql);
        db.execSQL(sql2);
        db.execSQL(sql3);
        db.execSQL(sql4);
    }

    @Override
    // 数据库版本更新时调用该方法
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //添加索引
        if(oldVersion==2&&newVersion==3){
            String sql1 = "CREATE UNIQUE INDEX IF NOT EXISTS index1 ON " + TABLE_NAME2 + " (path)";
            String sql2 = "CREATE UNIQUE INDEX IF NOT EXISTS index2 ON " + TABLE_NAME2 + " (path)";
            String sql3 = "CREATE UNIQUE INDEX IF NOT EXISTS index3 ON " + TABLE_NAME3 + " (path)";
            String sql4 = "CREATE UNIQUE INDEX IF NOT EXISTS index4 ON " + TABLE_NAME4 + " (path)";
            db.execSQL(sql1);
            db.execSQL(sql2);
            db.execSQL(sql3);
            db.execSQL(sql4);
        }
    }
}