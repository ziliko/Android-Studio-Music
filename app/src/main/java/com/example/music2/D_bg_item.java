package com.example.music2;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/*
 **    该界面负责设计各种下坠物细节
 **    雪花、雨点、落叶
 */
public class D_bg_item {
    Bitmap bbt;
    Bitmap bbt2;//用于单个子弹形态变化
    Bitmap bbt3;
    int bullet_speed;
    double x;//执行算术运算时，低字节可转换为高字节，即double可以直接+int  double主要是为了三角函数
    double y;
    double center_x;//用于部分有轨迹的子弹运动方式，作为运动圆心
    double center_y;
    int w;//子弹weith
    int h;//子弹height
    int screenW;                        //BOSS子弹额外需要屏幕尺寸，因为它一般是在屏幕下方结算的
    int screenH;
    boolean isBullet;//子弹存活标志
    double speed;
    int angle;//角度 0-360
    int jump_time;//弹射次数上限 默认0
    double speed_x;//圆形扩散运动 =speed*Math.cos(Math.PI/2)); PI=180°对应的弧度3.14?  输入度数angle 则PI=PI*angle/180  由于屏幕以下为正 所以正常向下=90°
    double speed_y;//圆形扩散运动 =speed*Math.sin(Math.PI/2));
    int rotation;//对部分子弹有旋转效果，有个自带初始随机角度
    double rand;
    int rand2;
    int move_way;//移动方式：0 正常下落 1 X轴追踪
    //暂定5关卡技能为方式11-15
    int move_waytime;//变量，用于定次改变移动 (20ms移动一次)

    String form="";//形状：圆形round，矩形rectangle，椭圆形oval
    //int ro_randomSpeed;//随机转速
    //int ro_randomWay;//随机方向
    //构造函数0
    public D_bg_item() {
    }
    //通用初始化部分
    public void basic_Init(int x, int y, int screenW, int screenH, double speed, Bitmap bitmap){
        this.w=bitmap.getWidth();
        this.h=bitmap.getHeight();
        this.x=x-w/2;
        this.y=y-h/2;
        this.screenW=screenW;
        this.screenH=screenH;
        this.bbt=bitmap;
        this.speed=speed;
        this.angle=90;//默认前进矢量方向
        rand=Math.random();//0-1
        rand2=(int)(rand*2);//0|1
        isBullet=true;
    }
    //初始化构造函数1  得到BOSS中下点坐标和子弹图形，数据需要处理  默认方式0，90角度
    public D_bg_item(int x, int y, int screenW, int screenH, double speed, Bitmap bitmap) {
        basic_Init(x,y,screenW,screenH,speed,bitmap);
        speed_x=this.speed*Math.cos(Math.PI/2);
        speed_y=this.speed*Math.sin(Math.PI/2);
        move_way=0;
    }
    //初始化构造函数2  加入方式
    public D_bg_item(int x, int y, int screenW, int screenH, double speed, Bitmap bitmap, int way) {
        basic_Init(x,y,screenW,screenH,speed,bitmap);
        move_way=way;
        if(way==1) this.angle+=180;
        speed_x=this.speed*Math.cos(Math.PI*angle/180);
        speed_y=this.speed*Math.sin(Math.PI*angle/180);
        if(way==3||way==4||way==53||way==54) rotation=(int)(rand*300);//生成随机初始角度(旋转)
    }
    //初始化构造函数3  加入方式0和角度(其他方式未测试)
    public D_bg_item(int x, int y, int screenW, int screenH, double speed, Bitmap bitmap, int way, int angle) {
        basic_Init(x,y,screenW,screenH,speed,bitmap);
        move_way=way;
        this.angle=angle;//角度（前进方向）
        if(way==1) this.angle+=180;
        if(way==6||way==7) {center_x=x;center_y=y-w/2;this.y=y-w/2;}//标准轨迹圆心坐标
        speed_x=this.speed*Math.cos(Math.PI*angle/180);
        speed_y=this.speed*Math.sin(Math.PI*angle/180);
        if(way==3||way==4||way==53||way==54) rotation=(int)(rand*300);//生成随机初始角度(旋转)
    }
    //初始化构造函数4  加入方式和角度和弹射次数 （值小于等于10时）
    /*初始化构造函数  针对四方形扩散弹幕，加入方式和 方向角度和 计算速度大小的角度  参数相同合并，此处角度加360区分(值大于10时)*/
    public D_bg_item(int x, int y, int screenW, int screenH, double speed, Bitmap bitmap, int way, int angle, int jump_time) {
        basic_Init(x,y,screenW,screenH,speed,bitmap);
        move_way=way;
        this.angle=angle;//角度（前进方向）
        if(way==1) this.angle+=180;
        if(jump_time<=10) this.jump_time=jump_time;
        else{
            jump_time=jump_time%90;
            if(jump_time>45) jump_time=90-jump_time;
            this.speed=speed/Math.cos(Math.PI*jump_time/180);;
        }
        speed_x=this.speed*Math.cos(Math.PI*angle/180);
        speed_y=this.speed*Math.sin(Math.PI*angle/180);
        if(way==3||way==4||way==53||way==54) rotation=(int)(rand*300);//生成随机初始角度(旋转)
    }
    //初始化构造函数5  加入方式和角度和形状
    public D_bg_item(int x, int y, int screenW, int screenH, double speed, Bitmap bitmap, int way, int angle, String form) {
        basic_Init(x,y,screenW,screenH,speed,bitmap);
        move_way=way;
        this.angle=angle;//角度（前进方向）
        if(way==1) this.angle+=180;
        speed_x=this.speed*Math.cos(Math.PI*angle/180);
        speed_y=this.speed*Math.sin(Math.PI*angle/180);
        rotation=angle-90;//※※因为除了圆以外的形状都在这里，所以判定的旋转角度初始化也在这里
        if(way==3||way==4||way==53||way==54) rotation=(int)(rand*300);//生成随机初始角度(旋转)
        this.form=form;
    }

    //返回Bitmap对象，用于绘制不同种类子弹
    public Bitmap getBitmap() {
        return bbt;
    }
    //子弹生命结束判断函数
    public boolean end()
    {
        if(isBullet) return false;
        else return true;//该子弹对象生命周期结束，清除
    }
    //
    public void Draw_Bullet(Canvas canvas) {
        //
    }
    //子弹移动总函数--总之需要带参数了（大概）
    public void Move() {
        if(move_way==50) Move_50();
        else if(move_way==30) Move_30();
        else if(move_way==12) Move_12();
        else if(move_way==7) Move_7();
        else if(move_way==6) Move_6();
        else if(move_way==5) Move_5();
        else if(move_way==1||move_way==0) Move_0();
        //else Move_0();
    }
    public void Move(int X) {
        if(move_way==41) Move_41(X);
        else if(move_way==31) Move_31(X);
        else if(move_way==4||move_way==54) Move_4(X);
        else if(move_way==3||move_way==53) Move_3(X);
        else if(move_way==2) Move_2(X);
        //else Move_0();
    }
    public void Move(int X,int Y){
        if(move_way==43) Move_43(X,Y);
        else if(move_way==42) Move_42(X,Y);
        else if(move_way==8) Move_8(X,Y);
        //else Move_0();
    }
    //子弹移动0：正常移动
    public void Move_0() {
        if(isBullet) {
            move_waytime++;
            x+=speed_x;
            y+=speed_y;
            if(jump_time>0){
                if(y>=screenH-h||y<=0) {speed_y=-speed_y;jump_time--;}//四种弹射
                else if(x<=0||x>screenW-w) {speed_x=-speed_x;jump_time--;}
            }
            //else if(move_way>10){//11-15 技能的判定范围不同 移动方式也不同 如魔炮不动，枪高速移动，玉极慢移动，连追正常
            else{
                if(y>screenH*2||y<-screenH-h) isBullet=false;//大范围不注重即时注销 通用 -h是为了暂时处理红枪
                else if(x<-screenH||x>screenW+screenH) isBullet=false;
            }
        }
    }
    //子弹移动1： （略）反向移动(爆菊弹)   可省略，只需要读取方式1时角度改成相反数然后使用0即可

    //子弹移动2：追踪弹(x轴)
    public void Move_2(int myX) {//myX为中心坐标
        if(isBullet) {
            y+=speed;
            if(x+w/2<myX) x+=2;//追踪速度
            else x-=2;
            if(y>screenH+h*2||y<-h*2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
            else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }
    //子弹移动3：旋转(x轴)--雪花
    public void Move_3(int time) {//虽然用不到time但还是要带参
        if(isBullet) {
            y+=speed;
            move_waytime++;
            rotation++;
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y>screenH+h*2||y<-screenH/2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
            else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }
    //子弹移动4：飘摇(变量控制方向)+旋转(x轴)--雪花
    public void Move_4(int time) {
        if(isBullet) {
            y+=speed;
            x+=time;//对应飘摇速度 time负则向左，time正则向右，大小影响速度
            move_waytime++;
            rotation++;
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y>screenH+h*2||y<-screenH/2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
            else if(x<-screenW||x>screenW*2) isBullet=false;
        }
    }
    //子弹移动5：匀加速(自由落体)运动
    public void Move_5() {
        if(isBullet) {
            x+=speed_x;
            y+=speed_y;
            move_waytime++;
            if(move_waytime>4) {speed_y++;move_waytime=0;}//0.1秒加速1
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y>screenH*2||y<-screenH) isBullet=false;//大范围不注重即时注销 通用
            else if(x<-screenH||x>screenW+screenH) isBullet=false;
        }
    }
    //子弹移动6：先从圆心出来四散1秒，然后绕圆心旋转运动，不断改变方向
    public void Move_6() {//用于追逐？用圆心
        if(isBullet) {
            x+=speed_x;
            y+=speed_y;
            move_waytime++;
            if(move_waytime>100){//1秒后开始每次重置方向   根据中心--角度--角度+90--得到新的xy速度
                if(x>=center_x) angle=(int)Math.toDegrees(Math.atan((y-center_y)/(x-center_x)))+90;//注！这两句只能处理顺时针的情况
                else angle=(int)Math.toDegrees(Math.atan((y-center_y)/(x-center_x)))+270;
                speed_x=speed*Math.cos(Math.PI*angle/180);
                speed_y=speed*Math.sin(Math.PI*angle/180);
                move_waytime=0;//重置用于前进
            }
            //else if(move_way>10){//11-15 技能的判定范围不同 移动方式也不同 如魔炮不动，枪高速移动，玉极慢移动，连追正常
            if(y>screenH*2||y<-screenH) isBullet=false;//大范围不注重即时注销 通用
            else if(x<-screenH||x>screenW+screenH) isBullet=false;
            //if(y>screenH+h*2||y<-h*2) isBullet=false;//大图用这个
            //else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }
    //子弹移动7(参考6)：绕统一圆心转动的散弹
    public void Move_7() {//用于追逐？用圆心
        if(isBullet) {
            x+=speed_x;
            y+=speed_y;
            move_waytime++;
            if(move_waytime>50){//1秒后开始每次重置方向   根据中心--角度--角度+90--得到新的xy速度
                if(x>=center_x) angle=(int)Math.toDegrees(Math.atan((y-center_y)/(x-center_x)))+90;//注！这两句只能处理顺时针的情况
                else angle=(int)Math.toDegrees(Math.atan((y-center_y)/(x-center_x)))+270;
                speed_x=speed*Math.cos(Math.PI*angle/180);
                speed_y=speed*Math.sin(Math.PI*angle/180);
                y+=speed;center_y+=speed;//整体向下
            }
            //else if(move_way>10){//11-15 技能的判定范围不同 移动方式也不同 如魔炮不动，枪高速移动，玉极慢移动，连追正常
            if(y>screenH*2||y<-screenH) isBullet=false;//大范围不注重即时注销 通用
            else if(x<-screenH||x>screenW+screenH) isBullet=false;
            //if(y>screenH+h*2||y<-h*2) isBullet=false;//大图小范围用这个
            //else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }
    //子弹移动8： 瞄准弹（全方位通用） 仅出场瞄准一次
    public void Move_8(int myX_centre,int myY_centre) {
        if(isBullet) {
            move_waytime++;
            //仅在此时调用两个参数改变子弹方向，指向玩家当前位置
            if(move_waytime==50) {
                double dx=myX_centre-(x+w/2);
                double dy=myY_centre-(y+h/2);
                double dz=Math.sqrt(Math.pow((int)dx, 2)+ Math.pow((int)dy,2));
                speed=dx/dy;//此方式speed用不到，作为寄存器使用
                if(dy<0) speed_y=-1;//关键 赋初值
                else speed_y=1;
                speed_x=speed_y*speed;
                rotation=(int)(Math.acos(dx/dz)/Math.PI*180)-90;
                if(dy<0) rotation=180-rotation;
                //注意！！！反三角函数有范围局限asin只能-90~90 acos只能0~180 atan只能-90~90?
                //k=Math.tan((rotation+90)*Math.PI/180);
            }
            if(move_waytime>50) {//50约1秒？
                x+=speed_x;
                y+=speed_y;
                if(move_waytime>60) {
                    if(speed_y<0) speed_y-=2;//跟据原方向分类加速
                    else speed_y+=2;
                    speed_x=speed_y*speed;
                    move_waytime=50;
                }//0.1秒加速1

                if(y>screenH*2||y<-screenH) isBullet=false;//大范围不注重即时注销 通用
                else if(x<-screenH||x>screenW+screenH) isBullet=false;
            }
        }
    }


    //子弹移动12：（仿魔炮）加速自由落体运动 出场停驻1秒，然后180°密集下落
    public void Move_12() {
        if(isBullet) {
            move_waytime++;
            if(move_waytime>50){
                x+=speed_x;
                y+=speed_y;
                speed_y++;//疯狂加速1
            }
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y>screenH+h*2||y<-h*2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
            else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }

    //子弹移动30:（基于方式0） 加入重力加速，多方向，弹射，仅触碰底部会减次数  阴阳散珠   重力加速下落，并加入反弹，加入能量守恒
    public void Move_30(){
        if(isBullet) {
            //if(move_waytime==0) y=0;//初始化y速度为0
            //move_waytime++;
            x+=speed_x;
            y+=speed_y;
            speed_y++;
            if(jump_time>0){
                if(y>=screenH-h) {speed_y=-speed_y;jump_time--;}//四种弹射
                else if(x<=0||x>screenW-w) {speed_x=-speed_x;}
            }
            //else if(move_way>10){//11-15 技能的判定范围不同 移动方式也不同 如魔炮不动，枪高速移动，玉极慢移动，连追正常
            else{
                if(y>screenH+h*2||y<-h*2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
                else if(x<-w*2||x>screenW+w*2) isBullet=false;
            }
        }
    }
    //子弹移动31：（类雪花）   阴阳玉 旋转匀速下落 转速加快(函数外实现：吸附效果)
    public void Move_31(int time) {//虽然用不到time但还是要带参
        if(isBullet) {
            y+=speed;
            move_waytime++;
            rotation+=move_waytime/10;
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y<-h*2||y>screenH) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h  1倍边缘，出界立即消除，吸附效果结束
            else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }


    //子弹移动41(基于方式5)：停驻几秒+加速下落+加速追逐（梦想封印）
    public void Move_41(int myX) {
        if(isBullet) {
            move_waytime++;
            if(move_waytime>50) {
                y+=speed;
                if(x+w/2<myX) x+=speed/5;//追踪速度
                else x-=speed/5;
                if(move_waytime>60) {speed++;move_waytime=50;}//0.2秒加速1
                //if(rand2==1) rotation++;//旋转不知道为什么会抖动
                //else rotation--;
                if(y>screenH+h*2||y<-h*2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
                else if(x<-w*2||x>screenW+w*2) isBullet=false;
            }
        }
    }
    //子弹移动42(基于方式5)：停驻几秒+加速下落+方向指向玩家（梦想妙珠）  PS缺陷：只能向下，不能向上追击
    public void Move_42(int myX_centre,int myY_centre) {
        if(isBullet) {
            move_waytime++;
            //仅在此时调用两个参数改变子弹方向，指向玩家当前位置
            if(move_waytime==50) {
                double dx=myX_centre-(x+w/2);
                double dy=myY_centre-(y+h/2);
                double dz=Math.sqrt(Math.pow((int)dx, 2)+ Math.pow((int)dy,2));
                speed=dx/dy;//此方式speed用不到，作为寄存器使用
                speed_x=speed_y*speed;
                rotation=(int)(Math.acos(dx/dz)/Math.PI*180)-90;
                //if(dy<0) rotation=270-rotation;
                //注意！！！反三角函数有范围局限asin只能-90~90 acos只能0~180 atan只能-90~90?
                //k=Math.tan((rotation+90)*Math.PI/180);
            }
            if(move_waytime>50) {//50约1秒？
                x+=speed_x;
                y+=speed_y;
                if(move_waytime>60) {
                    if(myY_centre-(y+h/2)>0)
                    speed_y+=2;
                    speed_x+=2*speed;
                    move_waytime=50;
                }//0.1秒加速1
                //if(rand2==1) rotation++;//旋转不知道为什么会抖动
                //else rotation--;
                if(y>screenH*2||y<-screenH) isBullet=false;//大范围不注重即时注销 通用
                else if(x<-screenH||x>screenW+screenH) isBullet=false;
            }
        }
    }
    //DNA测试
    public void Move_43(int myX_centre,int myY_centre) {
        if(isBullet) {
            if(move_waytime==0) {speed=(myX_centre-(x+w/2))/(myY_centre-(y+h/2));speed_x=speed_y*speed;}//此方式speed用不到，作为寄存器使用
            move_waytime++;
            //仅在此时调用两个参数改变子弹方向，指向玩家当前位置
            x+=speed_x;
            y+=speed_y;
            if(move_waytime==50) move_waytime=0;
            //不加速if(move_waytime>60) {speed_y+=2;speed_x+=2*speed;move_waytime=50;}//0.1秒加速1
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y>screenH+h*2||y<-h*2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
            else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }
    //子弹移动50  停留一段时间后消失的子弹 原地旋转雪花
    public void Move_50() {
        if(isBullet) {
            move_waytime++;
            rotation++;
            if(move_waytime>300) isBullet=false;//6秒
            //if(rand2==1) rotation++;//旋转不知道为什么会抖动
            //else rotation--;
            if(y>screenH+h*2||y<-h*2) isBullet=false;//若是敌机子弹则需上下左右4个方向/下1个方向判定，需要screen w、h
            else if(x<-w*2||x>screenW+w*2) isBullet=false;
        }
    }
    //子弹移动53 雪花型子弹独有  沿用方式3 加入冰冻Debuff判断依据53  （没啥用，改成宽大于高即为雪花判定）
    //子弹移动54 雪花型子弹独有  沿用方式4 加入冰冻Debuff判断依据54
    // (千里冰封) 全屏随机位置生成椭圆冰，不动，1秒后开始检测碰撞，持续一段时间消失
    //万里雪飘：即原一难
    //冰雪封天：千里冰封+万里雪飘
    //永恒冰冻：冰冻效果弹，弹幕密度大降，但冰冻弹范围大增，中弹累积减速，5层永久冰冻
    //EX冰之梦 ？？？


    //子弹碰撞处理+ 提前结束生命(圆形)
    public boolean crash(int myX,int myY,int myW,int myR) {//玩家判定范围会变动，需要外部处理好当前范围
        //玩家范围为圆形，需要不同半径
        if(isBullet){
            int dx=Math.abs((myX+myW/2)-((int)x+w/2));//abs求绝对值
            int dy=Math.abs((myY+myW/2)-((int)y+h/2));
            switch(form){
                //case "round":
                case "rectangle":
                    if(rotation!=0){//旋转了，对dx,dy进行处理
                        int myXmid=myX+myW/2;
                        int myYmid=myY+myW/2;
                        center_x=x+w/2;
                        center_y=y+h/2;
                        int d2=(int)(Math.pow(dx, 2)+ Math.pow(dy,2));
                        double k=Math.tan((rotation+90)*Math.PI/180);
                        int dx2=(int)(Math.pow(k*myXmid-myYmid+center_y-center_x*k,2)/(k*k+1));//点线距离公式： d= |AX.+BY.+C| / 根号(A^2+B^2)
                        dx=(int)Math.sqrt(dx2);
                        dy=(int)Math.sqrt(d2-dx2);
                    }
                    if(dx<=myR+w/2&&dy<=myR+h/2) {//未旋转，计算简单
                        isBullet=false;//提前结束生命
                        return true;//调用hp--的函数?
                    }

                    break;
                case "oval":
                    int a=(int)Math.sqrt(Math.pow(h, 2)- Math.pow(w,2));
                    int dx1=(myX+myW/2)-((int)x+w/2);
                    int dy1=(myY+myW/2)-((int)y+h/2+a);
                    int dx2=(myX+myW/2)-((int)x+w/2);
                    int dy2=(myY+myW/2)-((int)y+h/2-a);
                    if (Math.sqrt(Math.pow(dx1, 2)+ Math.pow(dy1,2))<= myR+w/2&&Math.sqrt(Math.pow(dx2, 2)+ Math.pow(dy2,2))<= myR+h) {//备注，必须h>w?
                        isBullet=false;//提前结束生命
                        return true;//调用hp--的函数?
                    }break;
                default://case "round":圆形（默认）
                    if (Math.sqrt(Math.pow(dx, 2)+ Math.pow(dy,2))<= myR+w/2) {
                    //if(Math.abs(centre_bossX-centre_x)<r && Math.abs(centre_bossY-centre_y)<r)
                    isBullet=false;//提前结束生命
                    return true;//调用hp--的函数?
                }break;
            }
        }
        return false;
    }
    //子弹擦弹处理+ 不提前结束生命（圆形）
    public boolean garze(int myX,int myY,int myW,int myR) {
        //玩家范围为圆形，需要不同半径
        if(isBullet){
            int dx=Math.abs((myX+myW/2)-((int)x+w/2));//abs求绝对值
            int dy=Math.abs((myY+myW/2)-((int)y+h/2));
            switch(form){
                //case "round":
                case "rectangle":
                    if (dx<=myR+w/2+30&&dy<=myR+h/2+30) {
                        return true;//调用hp--的函数?
                    }break;
                case "oval":
                    int a=(int)Math.sqrt(Math.pow(h, 2)- Math.pow(w,2));
                    int dx1=(myX+myW/2)-((int)x+w/2);
                    int dy1=(myY+myW/2)-((int)y+h/2+a);
                    int dx2=(myX+myW/2)-((int)x+w/2);
                    int dy2=(myY+myW/2)-((int)y+h/2-a);
                    if (Math.sqrt(Math.pow(dx1, 2)+ Math.pow(dy1,2))<= myR+w/2&&Math.sqrt(Math.pow(dx2, 2)+ Math.pow(dy2,2))<= myR+h+60) {//备注，必须h>w?
                        return true;//调用hp--的函数?
                    }break;
                default://case "round":圆形（默认）
                    if (Math.sqrt(Math.pow(dx, 2)+ Math.pow(dy,2))<= myR+w/2+30) {
                        //if(Math.abs(centre_bossX-centre_x)<r && Math.abs(centre_bossY-centre_y)<r)
                        return true;//调用hp--的函数?
                    }break;
            }
        }
        return false;
    }

    public int getWay(){
        return move_way;
    }

    //返回左上角坐标
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    //返回圆心坐标
    public double getX_mid() {
        return x+w/2;
    }
    public double getY_mid() {
        return y+h/2;
    }

    public int getAngle() {
        return angle;
    }
    public int getMove_waytime() {
        return move_waytime;
    }
    public int getRotation() {
        return rotation;
    }
    public int getRotation_rand() {
        return (int)(rotation*rand);
    }//关于这个随机转速，感觉有点局限 待改

    //返回冰效果判定 （暂定为 宽-5>高 ）
    public boolean is_ice(){if(w-5>h) return true;return false;}
    public void jixu(){//部分子弹碰撞后不立即消失
        isBullet=true;
    }

}
