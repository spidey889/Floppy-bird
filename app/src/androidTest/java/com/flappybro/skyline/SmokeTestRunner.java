package com.flappybro.skyline;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.MotionEvent;
import java.lang.reflect.Field;

/** Exercises the actual Android view and game loop without a third-party test framework. */
public final class SmokeTestRunner extends Instrumentation {
    private GameView view;
    private FlightWorld world;
    private Field state;
    private StringBuilder checks = new StringBuilder();
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private Object value(Object object,String name) throws Exception { Field f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    private void check(boolean condition,String label) { if(!condition) throw new AssertionError(label); checks.append("PASS ").append(label).append('\n'); }
    private void tap(float x,float y) {
        float s=view.getWidth()/400f;
        long n=SystemClock.uptimeMillis(); MotionEvent e=MotionEvent.obtain(n,n,MotionEvent.ACTION_DOWN,x*s,y*s,0); view.onTouchEvent(e); e.recycle();
    }
    @Override public void onStart() {
        Bundle result=new Bundle(); Activity activity=null;
        try {
            Intent intent=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=startActivitySync(intent); waitForIdleSync();
            view=(GameView)value(activity,"game"); world=(FlightWorld)value(view,"world"); state=GameView.class.getDeclaredField("state"); state.setAccessible(true);
            runOnMainSync(()-> { try { check(state.getInt(view)==0,"native home screen launched"); tap(200,450); check(state.getInt(view)==1,"tap starts flight"); } catch(Exception e) { throw new RuntimeException(e); } });
            SystemClock.sleep(500);
            runOnMainSync(()-> { tap(40,40); try { check(state.getInt(view)==3,"pause control"); } catch(Exception e) { throw new RuntimeException(e); } });
            float pausedY=world.y; SystemClock.sleep(400); check(world.y==pausedY,"physics frozen during pause");
            runOnMainSync(()-> tap(200,350));
            long deadline=SystemClock.uptimeMillis()+120000;
            while(SystemClock.uptimeMillis()<deadline && !world.dead && world.score<3) {
                runOnMainSync(()-> {
                    FlightWorld.Gate next=null;
                    for(FlightWorld.Gate g:world.gates) if(g.x+FlightWorld.GATE_WIDTH>FlightWorld.BIRD_X-FlightWorld.RADIUS) { next=g; break; }
                    float target=next==null?world.height*.43f:next.center;
                    if(world.y>target+15 && world.velocity>0)tap(108,world.y);
                });
                SystemClock.sleep(30);
            }
            check(world.score>=3,"real time touch flight passes three obstacles");
            deadline=SystemClock.uptimeMillis()+5000;
            while(!world.dead && SystemClock.uptimeMillis()<deadline)SystemClock.sleep(50);
            check(world.dead,"collision ends flight"); check(state.getInt(view)==2,"game over screen");
            SystemClock.sleep(750);
            check(getTargetContext().getSharedPreferences("flappybro",0).getInt("best",0)>=3,"high score saved to Android preferences");
            runOnMainSync(()->tap(200,world.height*.5f+125));
            check(world.score==0 && state.getInt(view)==1,"restart resets score and begins flight");
            activity.finish();
            Activity again=startActivitySync(intent); waitForIdleSync();
            GameView restored=(GameView)value(again,"game");
            check((Integer)value(restored,"best")>=3,"high score survives activity recreation");
            result.putString("stream",checks.toString()); finish(Activity.RESULT_OK,result);
        } catch(Throwable e) { result.putString("stream",checks+"FAIL "+e); finish(Activity.RESULT_CANCELED,result); }
    }
}
