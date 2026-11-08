package com.flappybro.skyline;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.*;
import java.util.ArrayList;
import java.util.Random;

/** Native Android Canvas renderer, driven by the display's vsync. */
public final class GameView extends View implements Choreographer.FrameCallback {
    private static final int HOME=0, PLAY=1, OVER=2, PAUSE=3;
    private final Paint p = new Paint(3);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final FlightWorld world = new FlightWorld(System.nanoTime());
    private final SharedPreferences prefs;
    private final SoundPool sounds;
    private final int flapSound, pointSound, hitSound;
    private final Random rng = new Random(81);
    private final ArrayList<Spark> sparks = new ArrayList<>();
    private int state=HOME, best;
    private boolean sound, active, newBest;
    private float H=800, scale=1, offsetX, time, accumulator, overTime, flash, shake;
    private long lastFrame;
    private int safeTop;
    private static final class Spark { float x,y,vx,vy,life,max; int color; }

    public GameView(Context ctx) {
        super(ctx); setFocusable(true); setContentDescription("FlappyBro. Tap to flap through the gates. Sound and pause controls are at the top.");
        prefs=ctx.getSharedPreferences("flappybro",Context.MODE_PRIVATE);
        best=prefs.getInt("best",0); sound=prefs.getBoolean("sound",true);
        sounds=new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        flapSound=sounds.load(ctx,R.raw.flap,1); pointSound=sounds.load(ctx,R.raw.point,1); hitSound=sounds.load(ctx,R.raw.hit,1);
        setOnApplyWindowInsetsListener((v,insets)-> { safeTop=insets.getSystemWindowInsetTop(); return insets; });
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        scale=Math.min(w/400f,h/650f); offsetX=(w-400*scale)/2; H=h/scale;
        world.reset(H);
    }
    public void resumeFrames() { if (!active) { active=true; lastFrame=0; Choreographer.getInstance().postFrameCallback(this); } }
    public void suspend() { if(state==PLAY) state=PAUSE; active=false; lastFrame=0; Choreographer.getInstance().removeFrameCallback(this); }
    public void close() { suspend(); sounds.release(); }
    public boolean back() { if(state==PLAY) { state=PAUSE; return true; } if(state==PAUSE || state==OVER) { state=HOME; world.reset(H); return true; } return false; }
    @Override public void doFrame(long nanos) {
        if(!active) return;
        float dt=lastFrame==0 ? 0 : Math.min(.05f,(nanos-lastFrame)/1e9f); lastFrame=nanos;
        time+=dt; flash=Math.max(0,flash-dt*2.5f); shake=Math.max(0,shake-dt*30);
        if(state==PLAY) {
            accumulator+=dt;
            while(accumulator>=1f/120 && state==PLAY) {
                int prev=world.score; world.step(1f/120); accumulator-=1f/120;
                if(world.score>prev) { play(pointSound); burst(108,world.y,8,0xff60ead1); }
                if(world.dead) die();
            }
        }
        if(state==OVER) overTime+=dt;
        for(int i=sparks.size()-1;i>=0;i--) { Spark s=sparks.get(i); s.life-=dt; if(s.life<=0) { sparks.remove(i); continue; } s.x+=s.vx*dt; s.y+=s.vy*dt; s.vy+=220*dt; }
        invalidate(); Choreographer.getInstance().postFrameCallback(this);
    }
    private void play(int id) { if(sound) sounds.play(id,.7f,.7f,1,0,1); }
    private void start() { world.reset(H); sparks.clear(); accumulator=0; state=PLAY; newBest=false; flap(); }
    private void flap() { world.flap(); play(flapSound); burst(88,world.y+5,4,0xffffd39b); }
    private void die() {
        state=OVER; overTime=0; flash=.6f; shake=6; play(hitSound); burst(108,world.y,24,0xffffb87d);
        newBest=world.score>best;
        if(newBest) { best=world.score; prefs.edit().putInt("best",best).apply(); }
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }
    private void burst(float x,float y,int n,int color) {
        for(int i=0;i<n;i++) { Spark s=new Spark(); s.x=x; s.y=y; s.vx=(rng.nextFloat()-.6f)*200; s.vy=(rng.nextFloat()-.7f)*180; s.life=s.max=.3f+rng.nextFloat()*.5f; s.color=color; sparks.add(s); }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override public boolean onTouchEvent(android.view.MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_DOWN) return true;
        performClick(); float x=(e.getX()-offsetX)/scale, y=e.getY()/scale;
        float top=top();
        if(x>330 && y>top-14 && y<top+44) { sound=!sound; prefs.edit().putBoolean("sound",sound).apply(); if(sound)play(pointSound); return true; }
        if(state==PLAY && x<65 && y>top-14 && y<top+44) { state=PAUSE; return true; }
        if(state==HOME) { start(); return true; }
        if(state==PLAY) { flap(); return true; }
        if(state==PAUSE) { if(y>H*.5f+70 && y<H*.5f+124) { state=HOME; world.reset(H); } else { state=PLAY; accumulator=0; } return true; }
        if(state==OVER && overTime>.5f) {
            if(y>H*.5f+175 && y<H*.5f+225) { state=HOME; world.reset(H); } else start();
        }
        return true;
    }
    private float top() { return Math.max(36,safeTop/scale+14); }
    private void color(int c) { p.setColor(c); p.setShader(null); p.setStyle(Paint.Style.FILL); p.setAlpha(255); }
    private void box(Canvas c,float x,float y,float w,float h,float r,int col) { color(col); rect.set(x,y,x+w,y+h); c.drawRoundRect(rect,r,r,p); }
    private void circle(Canvas c,float x,float y,float r,int col) { color(col); c.drawCircle(x,y,r,p); }
    private void text(Canvas c,String s,float x,float y,float size,int col,boolean bold) {
        color(col); p.setTextSize(size); p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); p.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y,p);
    }
    private void line(Canvas c,float x1,float y1,float x2,float y2,float width,int col) { color(col); p.setStrokeWidth(width); p.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x1,y1,x2,y2,p); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(0xff10263e); c.save(); c.translate(offsetX,0); c.scale(scale,scale);
        if(shake>0)c.translate((rng.nextFloat()-.5f)*shake,(rng.nextFloat()-.5f)*shake);
        background(c);
        if(state!=HOME) for(FlightWorld.Gate g:world.gates) gate(c,g);
        ground(c);
        if(state!=HOME) bird(c,108,world.y,Math.max(-24,Math.min(75,world.velocity*.13f)),1);
        for(Spark s:sparks) { color(s.color); p.setAlpha((int)(255*s.life/s.max)); c.drawCircle(s.x,s.y,3*s.life/s.max+1,p); }
        p.setAlpha(255);
        if(state==HOME) home(c);
        if(state==PLAY) {
            text(c,""+world.score,200,top()+48,62,0xff071e34,true); text(c,""+world.score,200,top()+45,62,0xfffff1dd,true);
            box(c,20,top()-10,42,42,14,0x6636576b); line(c,36,top()+3,36,top()+19,3,0xffe6faf1); line(c,46,top()+3,46,top()+19,3,0xffe6faf1);
            if(world.elapsed<2) text(c,"TAP TO FLAP",200,H-128,12,0xffa8c9cd,true);
        }
        if(state==OVER) gameOver(c);
        if(state==PAUSE) paused(c);
        soundButton(c);
        if(flash>0) { color(0xffffe5c2); p.setAlpha((int)(flash*180)); c.drawRect(0,0,400,H,p); p.setAlpha(255); }
        c.restore();
    }
    private void background(Canvas c) {
        color(0xff10263e); p.setShader(new LinearGradient(0,0,0,H,new int[]{0xff10233b,0xff23485c,0xff70918a},null,Shader.TileMode.CLAMP)); c.drawRect(0,0,400,H,p); p.setShader(null);
        for(int i=0;i<35;i++) { float x=(i*127.1f)%400, y=(i*67.4f)%(H*.57f); circle(c,x,y, i%4==0?1.6f:.8f,0x667bd4d5); }
        circle(c,300,H*.27f,63,0x0fffdcaa); circle(c,300,H*.27f,47,0x15ffdcaa); circle(c,300,H*.27f,34,0xffffca91);
        for(int layer=0;layer<3;layer++) {
            float base=H-154+layer*30, drift=(state==PLAY?world.distance:time*14)*(.10f+layer*.08f);
            color(new int[]{0xff2c5666,0xff346577,0xff3e7780}[layer]);
            path.reset(); path.moveTo(-200, H);
            for(int i=-2;i<8;i++) { float x=i*110-(drift%110); path.lineTo(x,base); path.lineTo(x+48,base-65-(i%3)*20); path.lineTo(x+110,base); }
            path.lineTo(600,H); path.close(); c.drawPath(path,p);
        }
        cloud(c,(80-time*5)%600,H*.23f,.8f); cloud(c,360-(time*8)%600,H*.47f,1.1f);
    }
    private void cloud(Canvas c,float x,float y,float s) {
        box(c,x-40*s,y,100*s,12*s,6*s,0x185dd8d0); box(c,x-15*s,y-12*s,50*s,18*s,9*s,0x185dd8d0);
    }
    private void ground(Canvas c) {
        box(c,0,H-94,400,94,0,0xff102e43); box(c,0,H-94,400,5,0,0xff69d7bd);
        float d=state==PLAY?world.distance:time*20;
        for(int i=-1;i<18;i++)box(c,i*30-d%30,H-81,15,3,2,0xff28586a);
        text(c,"F L A P P Y B R O",200,H-39,10,0xff72a7ac,true);
        text(c,"ONE MORE FLIGHT.",200,H-22,8,0xff507e8d,false);
    }
    private void gate(Canvas c,FlightWorld.Gate g) {
        float upper=g.center-g.gap/2, lower=g.center+g.gap/2;
        tower(c,g.x,-20,68,upper+20,true); tower(c,g.x,lower,68,H-94-lower,false);
        for(int i=0;i<3;i++)circle(c,g.x+34,g.center+(i-1)*15,1.6f,0x447fffdc);
    }
    private void tower(Canvas c,float x,float y,float w,float h,boolean top) {
        box(c,x+4,y,w,h,9,0x22051d30); box(c,x,y,w,h,9,0xff24576a);
        box(c,x+7,y+4,5,h-8,2,0xff438c91); box(c,x+w-10,y+4,5,h-8,2,0xff163f54);
        float cap=top?y+h-21:y;
        box(c,x-5,cap,w+10,21,6,0xff78e0c4); box(c,x-5,cap+15,w+10,6,3,0xff3da599);
        box(c,x+6,cap+5,w-12,3,2,0xffc2ffe0);
        for(float yy=y+44; yy<y+h-32; yy+=65) { box(c,x+23,yy,22,6,3,0xff397b85); }
    }
    private void bird(Canvas c,float x,float y,float angle,float size) {
        c.save(); c.translate(x,y); c.rotate(angle); c.scale(size,size);
        color(0xffef7678); path.reset(); path.moveTo(-17,1); path.lineTo(-32,-6); path.lineTo(-26,9); path.close(); c.drawPath(path,p);
        circle(c,0,3,21,0xffdf816d); circle(c,0,0,21,0xffffbe7d); circle(c,5,6,13,0xffffdba3);
        float wing=(float)Math.sin(time*22)*4;
        box(c,-21,wing,23,12,7,0xfff3817a); line(c,-17,wing+4,-7,wing+7,2,0xffffa18c);
        box(c,2,-12,21,18,7,0xff203c51); box(c,5,-10,15,12,5,0xffe9fff3); circle(c,14,-4,3.5f,0xff132f46); circle(c,15,-5,1,0xffffffff);
        color(0xfff28d6d); path.reset(); path.moveTo(19,3); path.lineTo(33,8); path.lineTo(19,13); path.close(); c.drawPath(path,p);
        line(c,-7,-16,0,-21,3,0xffef7b76); c.restore();
    }
    private void soundButton(Canvas c) {
        float y=top(); box(c,340,y-10,42,42,14,0x5536576b);
        color(sound?0xffc9ede2:0xff83a7ae); path.reset(); path.moveTo(349,y+7); path.lineTo(355,y+7); path.lineTo(362,y+1); path.lineTo(362,y+23); path.lineTo(355,y+17); path.lineTo(349,y+17); path.close(); c.drawPath(path,p);
        if(sound) { line(c,368,y+5,371,y+11,2,0xffc9ede2); line(c,371,y+11,368,y+18,2,0xffc9ede2); }
        else { line(c,367,y+7,375,y+17,2,0xff83a7ae); line(c,375,y+7,367,y+17,2,0xff83a7ae); }
    }
    private void button(Canvas c,String label,float y) {
        box(c,64,y+5,272,54,18,0xff1d665f); box(c,64,y,272,54,18,0xff78e0c4); text(c,label,200,y+34,17,0xff123b46,true);
    }
    private void home(Canvas c) {
        float mid=H*.43f;
        text(c,"A LITTLE BIRD. A BIG SKY.",200,mid-129,10,0xffa7d7cf,true);
        text(c,"FLAPPY",200,mid-75,51,0xffffedcf,true); text(c,"BRO",200,mid-26,55,0xffffb580,true);
        box(c,169,mid-9,62,3,2,0xff78e0c4);
        bird(c,200,mid+55+(float)Math.sin(time*3)*7,-7,1.6f);
        text(c,"Find your rhythm. Chase the skyline.",200,mid+121,13,0xffc7dfd9,false);
        button(c,"LET'S FLY  →",mid+150);
        text(c,"TAP ANYWHERE TO FLAP",200,mid+229,10,0xffa8c9cd,true);
        text(c,"BEST FLIGHT   "+best,200,mid+257,12,0xffffd3a0,true);
    }
    private void shade(Canvas c) { color(0xbb071d31); c.drawRect(0,0,400,H-94,p); }
    private void gameOver(Canvas c) {
        shade(c); float mid=H*.5f;
        text(c,newBest?"A NEW PERSONAL BEST!":"EVERY FLIGHT COUNTS.",200,mid-150,10,0xff78e0c4,true);
        text(c,"Nice flight, bro.",200,mid-109,30,0xffffedcf,true);
        box(c,49,mid-78,302,157,23,0xff1d3d51); box(c,50,mid-79,300,153,23,0xff24485c);
        text(c,"SCORE",126,mid-43,10,0xff9bc3c7,true); text(c,"BEST",274,mid-43,10,0xff9bc3c7,true);
        line(c,200,mid-39,200,mid+31,1,0xff3b6271);
        text(c,""+world.score,126,mid+14,46,0xffffedcf,true); text(c,""+best,274,mid+14,46,0xff78e0c4,true);
        text(c,world.score>=20?"SKY LEGEND":world.score>=10?"CLOUD CRUISER":world.score>=5?"FINDING YOUR WINGS":"THE SKY'S STILL YOURS",200,mid+53,10,0xffffc891,true);
        button(c,"FLY AGAIN  ↗",mid+103); text(c,"BACK TO SKYLINE",200,mid+203,11,0xffb0cdd0,true);
    }
    private void paused(Canvas c) {
        shade(c); float mid=H*.5f;
        text(c,"Take a breather.",200,mid-48,30,0xffffedcf,true); text(c,"Your flight is right here.",200,mid-18,13,0xffa8c9cd,false);
        button(c,"RESUME FLIGHT",mid+15); text(c,"BACK TO SKYLINE",200,mid+105,11,0xffb0cdd0,true);
    }
}
