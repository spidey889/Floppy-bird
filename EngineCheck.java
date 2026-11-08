import com.flappybro.skyline.FlightWorld;
public class EngineCheck {
 public static void main(String[] args) {
  int min=10000;
  for(int seed=0;seed<100;seed++) {
   FlightWorld w=new FlightWorld(seed); w.reset(711); w.flap();
   for(int frame=0;frame<36000 && !w.dead && w.score<10;frame++) {
    FlightWorld.Gate next=null;
    for(FlightWorld.Gate g:w.gates) if(g.x+68>108-14) {next=g;break;}
    float target=next==null?310:next.center;
    if(w.y>target+15 && w.velocity>0)w.flap();
    w.step(1f/120);
   }
   min=Math.min(min,w.score);
   if(w.score<10)throw new AssertionError("Seed "+seed+" died at score "+w.score);
  }
  if(!FlightWorld.circleRect(10,10,4,12,5,8,10))throw new AssertionError("Collision");
  if(FlightWorld.circleRect(0,0,4,12,5,8,10))throw new AssertionError("Separation");
  System.out.println("PASS: 100 seeded flights each passed "+min+" gates; collision geometry checked.");
 }
}
