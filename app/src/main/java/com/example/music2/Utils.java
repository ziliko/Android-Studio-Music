package com.example.music2;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

//参考 TODO https://www.cnblogs.com/lyd447113735/p/8286598.html
//用于从本地扫描歌曲，返回一个List数据

class Song {
    public String song;//歌曲名
    public String singer;//歌手
    public long size;//歌曲所占空间大小
    public int duration;//歌曲时间长度
    public String path;//歌曲地址

    public int ID_Like;//网络歌曲ID  本地歌曲则是喜爱favourite
    public void init_Song(String a){//获取一个白板对象的初始化
        this.path=a;this.song=a;
    };
}
//工具类
public class Utils {
    //定义一个集合，存放从本地读取到的内容
    public static List<Song> list;

    public static Song song;

    public static List<Song> getmusic(Context context) {

        list = new ArrayList<>();

        //TODO 四大组件之内容提供者  中的内容接收者 ContentProvider和ContentResolve
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                , null, null, null, MediaStore.Audio.AudioColumns.IS_MUSIC);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                // todo   MediaStore.Audio.Media扫描本地文件？
                song = new Song();
                //song.song = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                song.song = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                song.singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                song.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                song.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                song.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                //歌曲名 TITLE
                //歌手 .ARTIST
                //专辑  ALBUM
                //长度  SIZE
                //时长  DURATION
                //路径  DATA
                //显示的文件名  DISPLAY_NAME
                //类型  .MIME_TYPE

                //把歌曲名字和歌手切割开 并规定大于一定时长(0.8MB 和 60秒)的歌曲才导入
                if (song.size > 1000 * 800&&song.duration>60000) {//0.8MB ??
                    /* 该切割方式不稳定
                    if (song.song.contains("-")) {
                        String[] str = song.song.split("-");
                        song.singer = str[0];
                        song.song = str[1];
                    }
                    */
                    list.add(song);
                }

            }
        }
        cursor.close();
        return list;

    }


    //    转换歌曲时间的格式
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            String tt = time / 1000 / 60 + ":0" + time / 1000 % 60;
            return tt;
        } else {
            String tt = time / 1000 / 60 + ":" + time / 1000 % 60;
            return tt;
        }
    }


}