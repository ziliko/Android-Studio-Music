package com.example.music2;

//该界面为
// Dynamic background Surfaceview 动态背景动画界面
// 实现雪花、雨点等效果，不可交互

//变化1：外部控制，背景切换，同时下落物品切换   切换方式：加个按键或者切歌时概率
//变化2：播放同一歌曲途中，可能会变更下坠物密度和频率，即每首歌开始时，随机生成某个变量控制阶段变化？

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class D_bg_SurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable{//父类只能一个,接口可以多个

    public static boolean view_switch=false;//控制动画开关
    //TODO 当前问题(性能优化)：该布尔变量只是阻止了新下坠物的诞生，计数等功能依然在继续，定时器耗电吗?  以及开关控制只能耳机线，需另加按钮

    private static final String TAG = "D_bg_SurfaceView";
    private static D_bg_SurfaceView Stage1;

    private static int bggundong=0;//控制背景滚动
    public static int stop;//控制暂停

    public static int items;//控制场景种类切换 雨/雪/花/叶
    int section=0;//控制场景阶段 稀疏/中雨/暴雪等

    public static boolean flag;//或者用flag控制暂停？
    Thread th;//单独刷屏线程
    SurfaceHolder sfh;
    Canvas canvas;
    Paint paint;
    static final long REFRESH_INTERVAL = 20;//刷新率：如果要60FPS则应设为17s

    static Context context;
    static AttributeSet attrs;

    static Bitmap bg;             //场景要素
    static Bitmap item_kind1,item_kind2,item_kind3;                     //item要素

    private static int screenWidth = 0;
    private static int screenHeight = 0;

    private static Timer timer;//用于控制自机
    private static TimerTask task;

    static List<Bitmap> pics;//※※※※集合※※※※
    static ArrayList<D_bg_item> item_List;//※※※※动态数组与泛型？※※※※

    private static int itemdelay;//物品添加的间隔

    int rain_control=0;//控制雨点飞舞状态/方向 TODO 主要在创建新雨点对象时控制    -1-2向左，0下落，1 2向右 60 75 90 105 120
    int sun_control=0;//控制日出日落动画 多张图片 切换
    int leaf_controd=0;//控制落叶飞舞
    int snow_control=0;//控制雪花飞舞状态/方向 TODO 主要在雪花对象Move函数中控制  -1-2负数即向左，0下落，1 2正数向右


    // 构造函数
    public D_bg_SurfaceView(Context context, AttributeSet attrs){
        super(context,attrs);//XML添加view控件  需将SurfaceView的构造函数修改为两个参数的
        this.context = context;
        this.attrs= attrs;
        sfh = getHolder();      // 获取SurfaceHolder对象
        sfh.addCallback(this);  // 绑定Callback监听器
        paint = new Paint();

        bg = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        item_kind1= BitmapFactory.decodeResource(getResources(), R.drawable.snow_l);
        item_kind2 = BitmapFactory.decodeResource(getResources(), R.drawable.rain_l);
        //item_kind3 = BitmapFactory.decodeResource(getResources(), R.drawable.boss_bullet3);

        //setFocusable(true);//控制键盘是否可以获得这个按钮的焦点?
        this.initValue();//所有变量的初始化

        this.initTimerTask();
        Log.d(TAG,"自定义动画对象构建");
    }

    /**/
    public static D_bg_SurfaceView Init(Context context){
        if(Stage1!=null){
            Stage1.setVisibility(GONE);
            Stage1 = new D_bg_SurfaceView(context,attrs);
            Stage1.setVisibility(VISIBLE);
        }
        else Stage1 = new D_bg_SurfaceView(context,attrs);
        return Stage1;
    }

    //所有变量的初始化
    private void initValue(){

        screenWidth = 0;
        screenHeight = 0;

        timer=null;//
        task=null;

        pics = new ArrayList<Bitmap>();//※※※※集合※※※※
        item_List = new ArrayList<D_bg_item>();//※※※※动态数组与泛型？※※※※

    }

    //定时器函数：控制下坠物的行动
    private void initTimerTask() {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                if(flag){
                    //已有Item移动
                    for(int i=0;i<item_List.size();i++)
                    {
                        D_bg_item bbullet=item_List.get(i);
                        bbullet.Move();                             //移动
                        bbullet.Move(snow_control);                      //移动,方式3、4  雪花飞舞方向

                        //清理无效( 出界) 的item
                        if(item_List.size()>0&&bbullet.end()) {
                            item_List.remove(i);
                            i--;//因为删除一个后 后面的全往前，size也立即-1
                        }
                    }
                    itemdelay+=20;//等于定时器计数频率
                    //每30秒切换阶段
                    if(itemdelay>30000) {
                        section++;
                        if(section>2) section=0;
                        itemdelay=0;
                    }

                    //每隔一段时间，新增item   条件：背景动画开关打开
                    if(view_switch){
                        switch (items){
                            case 0:
                                /*item1 -雪花/花朵，初始角度随机，概率旋转*/
                                //飘摇 左/无/右 by方式4
                                if (itemdelay%1000==0) {
                                    switch (section){
                                        case 0:
                                            item_List.add(new D_bg_item((int)(-screenWidth+screenWidth*3*Math.random()),(int)(-300*Math.random()),screenWidth,screenHeight,(int)(2+3*Math.random()),item_kind1,4));//变子弹只需要改这里
                                            //itemdelay=(int)(400*Math.random());
                                            break;
                                        case 1:
                                            for(int i=0;i<4;i++) item_List.add(new D_bg_item((int)(-screenWidth+screenWidth*3*Math.random()),(int)(-300*Math.random()),screenWidth,screenHeight,(int)(3+4*Math.random()),item_kind1,4));//变子弹只需要改这里
                                            break;
                                        case 2:
                                            for(int i=0;i<10;i++) item_List.add(new D_bg_item((int)(-screenWidth+screenWidth*3*Math.random()),(int)(-300*Math.random()),screenWidth,screenHeight,(int)(5+5*Math.random()),item_kind1,4));//变子弹只需要改这里
                                            break;
                                        default:break;
                                    }
                                    //小概率切换飘摇状态 平均10秒
                                    if(Math.random()>0.9) snow_control=(int) (Math.random()*5)-2;
                                }
                                break;
                            case 1:
                            /*item1 -雨点，初始高度随机，自由落体by方式5/
                            每30秒一个阶段：细雨 中雨 暴雨
                            随机角度，60，90，120度
                            TODO:问题1 因为重力加速影响，导致X轴速度跟不上Y轴速度  解决方案：取消重力加速机制，因为一般雨点快到地面都是恒定速度 且雨点们大都一样速度
                            TODO:问题2 全靠随机数生成，部分场景不符合现实（如雨点分布局部地区较为平均） 待定.......
                            */
                                if (itemdelay%200==0) {
                                    switch (section){
                                        case 0:
                                            //bbtList.add(new bossbullet(bossX+bossWidth/2,bossY+bossHeight,screenWidth,screenHeight,10,bbt_kind2));//变子弹只需要改这里
                                            item_List.add(new D_bg_item((int)(-screenWidth+screenWidth*3*Math.random()),(int)(-300*Math.random()),screenWidth,screenHeight,20,item_kind2,0,90+15*rain_control));//变子弹只需要改这里  速度(int)(10+4*Math.random())
                                            break;
                                        case 1:
                                            int x=(int)(screenWidth/10*Math.random());
                                            for(int i=0;i<20;i++) item_List.add(new D_bg_item((int)(-screenWidth+screenWidth*3*i/20+x),(int)(-300*Math.random()),screenWidth,screenHeight,30,item_kind2,0,90+15*rain_control));//变子弹只需要改这里
                                            break;
                                        case 2:
                                            int y=(int)(screenWidth/20*Math.random());
                                            for(int i=0;i<50;i++) item_List.add(new D_bg_item((int)(-screenWidth+screenWidth*3*i/50+y),(int)(-300*Math.random()),screenWidth,screenHeight,40,item_kind2,0,90+15*rain_control));//变子弹只需要改这里
                                            break;
                                        default:break;
                                    }
                                    //小概率切换飘摇状态 平均20秒
                                    if(Math.random()>0.99) rain_control=(int) (Math.random()*5)-2;
                                }
                                break;
                            case 2:
                                break;

                            default:break;
                        }
                    }

                }

            }
        };
        timer.schedule(task, 1000, 20);
    }


    @Override
    //用于刷新画布
    public void run() {
        while (flag) {
            //if(stop==0){
                long start = System.currentTimeMillis();
                if(!view_switch&&item_List.size()==0) flag=false;
                myDraw();
                long end = System.currentTimeMillis();
                long interval = end - start;
                try {
                    if (interval < REFRESH_INTERVAL) {
                        Thread.sleep(REFRESH_INTERVAL - interval);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            //}
        }
    }

    // SurfaceView被创建时的回调函数
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //myDraw();
        //rec=0;stop=0;
        screenWidth = getWidth();
        screenHeight = getHeight();

        paint.setColor(Color.WHITE);
        paint.setTextSize(80);paint.setStrokeWidth(5);

        flag = true;
        th = new Thread(this);
        th.start();
        Log.d(TAG,"自定义动画界面创建");
    }

    // SurfaceView状态改变时的回调函数  比如横竖切换？
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //??
    }

    // SurfaceView被销毁时的回调函数
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
        stop=1;
        Log.d(TAG,"自定义动画界面销毁");
        //timer.cancel();
        //mediaplayer.pause();//用于切屏回来后继续播放音乐
    }

    public static void end(){//结束时外部调用用于关闭定时器线程
        timer.cancel();
    }
    //interface stage3_return{//接口
    //    int value=3;
    //}

    //绘制主函数
    private void myDraw() {
        try {
            canvas = sfh.lockCanvas();   // 获取和锁定当前画布
            if (canvas != null) {
                //滚动背景，仅关卡1实装，其他图不衔接
                canvas.drawBitmap(bg,null,new Rect(0, bggundong, screenWidth, bggundong+screenHeight),paint);
                canvas.drawBitmap(bg,null,new Rect(0, bggundong-screenHeight, screenWidth, bggundong),paint);

                //画物品
                for(int i=0;i<item_List.size();i++)
                {
                    D_bg_item bbullet=item_List.get(i);
                    if(bbullet.getWay()==3||bbullet.getWay()==4) drawRotateBitmap(canvas, paint, bbullet.getBitmap(), bbullet.getRotation_rand(), (int)bbullet.getX(), (int)bbullet.getY());//方式3 4  角度随机
                    else if(bbullet.getWay()==0) drawRotateBitmap(canvas, paint, bbullet.getBitmap(), bbullet.getAngle()-90, (int)bbullet.getX(), (int)bbullet.getY());//方式5  角度与方向同
                    else canvas.drawBitmap(bbullet.getBitmap(), (int)bbullet.getX(), (int)bbullet.getY(), paint);
                    //drawRotateBitmap(canvas, paint, bbullet.getBitmap(), bbullet.getRotation(), (int)bbullet.getX(), (int)bbullet.getY());//包括但不限于方式 0 8 42 50
                }

            }
        }catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (canvas != null) {
                sfh.unlockCanvasAndPost(canvas);    // 解锁当前画布
            }
        }
    }
    //自旋函数：参考https://blog.csdn.net/nupt123456789/article/details/44079741
    private void drawRotateBitmap(Canvas canvas, Paint paint, Bitmap bitmap,
                                  float rotation, float posX, float posY) {
        Matrix matrix = new Matrix();
        int offsetX = bitmap.getWidth() / 2;
        int offsetY = bitmap.getHeight() / 2;
        matrix.postTranslate(-offsetX, -offsetY);
        matrix.postRotate(rotation);
        matrix.postTranslate(posX + offsetX, posY + offsetY);
        canvas.drawBitmap(bitmap, matrix, paint);
    }



    //转阶段函数 各关卡不同
    public void item_change(){
        items=1-items;//切换雨/雪模式
        //重置阶段(疏密)
        section=2;
        //重置control(方向)
        snow_control=0;
        rain_control=0;
        //切换背景
        switch(items){
            case 0:
                bg = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
                break;
            case 1:
                bg = BitmapFactory.decodeResource(getResources(), R.drawable.bg2);
                break;
        }
        Log.i(TAG,"转阶段 items="+items);
    }

}

//方式构思
//已有：雪原-雪花，池塘-雨点
//构思：星空-流星，？？-蒲公英，花田-花
