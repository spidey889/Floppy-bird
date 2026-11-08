package com.flappybro.skyline;

import java.util.ArrayList;
import java.util.Random;

/** Pure game rules; coordinates are density-independent world units. */
public final class FlightWorld {
    public static final float BIRD_X = 108, RADIUS = 14, GATE_WIDTH = 68;
    public static class Gate { public float x, center, gap; public boolean counted; }
    public final ArrayList<Gate> gates = new ArrayList<>();
    private final Random random;
    public float height, y, velocity, elapsed, distance;
    public int score;
    public boolean dead;
    public FlightWorld(long seed) { random = new Random(seed); }
    public void reset(float height) {
        this.height = height; y = height * .43f; velocity = 0; elapsed = distance = 0; score = 0; dead = false; gates.clear();
        addGate(480); addGate(710); addGate(940);
    }
    private void addGate(float x) {
        Gate g = new Gate(); g.x = x; g.gap = Math.max(158, 188 - score * 1.1f);
        float min = 125 + g.gap / 2, max = height - 148 - g.gap / 2;
        g.center = min + random.nextFloat() * Math.max(1, max - min);
        if (!gates.isEmpty()) g.center = Math.max(gates.get(gates.size()-1).center - 100, Math.min(gates.get(gates.size()-1).center + 100, g.center));
        gates.add(g);
    }
    public void flap() { if (!dead) velocity = -325; }
    public void step(float dt) {
        if (dead) return;
        elapsed += dt; float speed = Math.min(205, 145 + score * 2f);
        distance += speed * dt; velocity = Math.min(500, velocity + 970 * dt); y += velocity * dt;
        if (y - RADIUS <= 0 || y + RADIUS >= height - 94) { dead = true; return; }
        for (Gate g : gates) {
            g.x -= speed * dt;
            if (circleRect(BIRD_X, y, RADIUS, g.x - 5, 0, GATE_WIDTH + 10, g.center - g.gap/2) ||
                circleRect(BIRD_X, y, RADIUS, g.x - 5, g.center + g.gap/2, GATE_WIDTH + 10, height)) { dead = true; return; }
            if (!g.counted && g.x + GATE_WIDTH < BIRD_X - RADIUS) { g.counted = true; score++; }
        }
        if (!gates.isEmpty() && gates.get(0).x < -100) { gates.remove(0); addGate(gates.get(gates.size()-1).x + 230); }
    }
    public static boolean circleRect(float cx,float cy,float r,float x,float y,float w,float h) {
        float dx = cx - Math.max(x, Math.min(cx,x+w)), dy = cy - Math.max(y,Math.min(cy,y+h));
        return dx*dx + dy*dy < r*r;
    }
}
