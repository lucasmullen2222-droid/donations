package donations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AlertManager {
    private static final ArrayDeque<Alert> QUEUE = new ArrayDeque<>();
    private static Alert current;
    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static final Random RNG = new Random();
    private static long lastFrame = System.currentTimeMillis();
    private static long lastRain = 0;
    private static long lastBurst = 0;

    private static class Particle {
        float x, y, vx, vy, life, size; int color; float decay = 0.7f; float gravity = 220f;
    }

    public static void push(Alert a) { QUEUE.add(a); }

    // ---------- helpers ----------
    private static void play(SoundEvent s, float pitch) {
        if (!DonoAlertsClient.CONFIG.soundsEnabled) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 1.0f));
    }

    private static void startSounds(Alert a) {
        switch (a.tier()) {
            case 1 -> play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f);
            case 2 -> { play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f); play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f); }
            case 3 -> { play(SoundEvents.PLAYER_LEVELUP, 1.0f); play(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f); }
            case 4 -> { play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f); play(SoundEvents.PLAYER_LEVELUP, 0.8f); play(SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f); }
            case 5 -> { play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f); play(SoundEvents.TOTEM_USE, 1.0f); play(SoundEvents.ENDER_DRAGON_GROWL, 1.0f); }
            default -> { play(SoundEvents.WITHER_SPAWN, 1.0f); play(SoundEvents.ENDER_DRAGON_DEATH, 1.0f); play(SoundEvents.TOTEM_USE, 0.8f); play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f); }
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    private static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float r = 0, g = 0, b = 0;
        float f = h * 6; int i = (int) f; float ff = f - i;
        float p = v * (1 - s), q = v * (1 - s * ff), t = v * (1 - s * (1 - ff));
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    private static int rainbow(long now, float offset) { return hsv((now % 2000) / 2000f + offset, 0.85f, 1f); }

    private static int tierColor(int tier, long now) {
        return switch (tier) {
            case 1 -> 0xFF55FF55;
            case 2 -> 0xFFFFD700;
            case 3 -> 0xFFFF9A2E;
            case 4 -> 0xFF3FE0FF;
            case 5 -> (now / 120 % 2 == 0) ? 0xFFFF3B3B : 0xFFFFFFFF;
            default -> rainbow(now, 0f);
        };
    }

    private static void burst(float cx, float cy, int count, int tier, long now) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            double ang = RNG.nextDouble() * Math.PI * 2;
            float sp = 40 + RNG.nextFloat() * (60 + tier * 45);
            p.x = cx; p.y = cy;
            p.vx = (float) Math.cos(ang) * sp; p.vy = (float) Math.sin(ang) * sp - 40;
            p.color = tier >= 6 ? rainbow(now, RNG.nextFloat()) : tierColor(tier, now);
            p.life = 1f; p.size = tier >= 5 ? 4 : 3;
            PARTICLES.add(p);
        }
    }

    private static void rain(int sw, int tier, long now) {
        for (int i = 0; i < tier * 2; i++) {
            Particle p = new Particle();
            p.x = RNG.nextInt(sw); p.y = -4; p.vx = (RNG.nextFloat() - 0.5f) * 30; p.vy = 40 + RNG.nextFloat() * 60;
            p.gravity = 20; p.decay = 0.25f; p.size = 3;
            p.color = rainbow(now, RNG.nextFloat()); p.life = 1f;
            PARTICLES.add(p);
        }
    }

    private static void centerText(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int color) {
        g.text(f, s, cx - f.width(s) / 2, y, color, true);
    }

    private static void bigCenter(GuiGraphicsExtractor g, Font f, String s, int cx, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(scale, scale);
        g.text(f, s, -f.width(s) / 2, 0, color, true);
        g.pose().popMatrix();
    }

    // ---------- main render ----------
    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        if (current == null && !QUEUE.isEmpty()) {
            current = QUEUE.poll();
            current.startMs = now;
            startSounds(current);
        }

        int sw = g.guiWidth(), sh = g.guiHeight();

        // particles first (behind box)
        Iterator<Particle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= dt * p.decay;
            if (p.life <= 0 || p.y > sh + 10) { it.remove(); continue; }
            p.vy += p.gravity * dt;
            p.x += p.vx * dt; p.y += p.vy * dt;
            int a = Math.max(0, Math.min(255, (int) (p.life * 255)));
            int s = (int) p.size;
            g.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, (a << 24) | (p.color & 0xFFFFFF));
        }

        if (current == null) return;
        Alert a = current;
        long t = now - a.startMs;
        if (t > a.durationMs()) { current = null; return; }

        int tier = a.tier();
        int color = tierColor(tier, now);
        int rgb = color & 0xFFFFFF;

        // ---- full screen effects ----
        if (tier >= 3 && t < 800) {
            int[] flash = {0, 0, 45, 80, 120, 170};
            int fa = (int) ((1f - t / 800f) * flash[tier - 1]);
            g.fill(0, 0, sw, sh, (fa << 24) | (tier == 5 ? 0xFFFFFF : rgb));
        }
        if (tier == 6) {
            // slow rainbow tint over the whole screen
            int ta = 22 + (int) (14 * Math.sin(now / 250.0));
            g.fill(0, 0, sw, sh, (ta << 24) | (rainbow(now, 0.3f) & 0xFFFFFF));
        }
        if (tier >= 5) {
            int ga = 70 + (int) (50 * Math.sin(now / 130.0));
            int thick = tier == 6 ? 8 : 5;
            int ge = (ga << 24) | rgb;
            g.fill(0, 0, sw, thick, ge); g.fill(0, sh - thick, sw, sh, ge);
            g.fill(0, 0, thick, sh, ge); g.fill(sw - thick, 0, sw, sh, ge);
        }
        if (tier >= 4) {
            // light beams down from the card
            int bx = sw / 2;
            int ba = 35 + (int) (20 * Math.sin(now / 200.0));
            int bw = 10 + tier * 6;
            g.fill(bx - bw, 0, bx + bw, sh, (ba << 24) | rgb);
        }
        // shockwave rings
        if (tier >= 5) {
            int rings = tier == 6 ? 3 : 1;
            for (int r = 0; r < rings; r++) {
                float rt = ((now + r * 600) % 1800) / 1800f;
                int rad = (int) (rt * Math.max(sw, sh) * 0.6f);
                int ra = (int) ((1f - rt) * 160);
                int cx = sw / 2, cy = 30;
                g.outline(cx - rad, cy - rad / 3, rad * 2, rad * 2 / 3 + 1, (ra << 24) | (tier == 6 ? (rainbow(now, r * 0.2f) & 0xFFFFFF) : rgb));
            }
        }
        if (tier == 6 && now - lastRain > 40) { lastRain = now; rain(sw, tier, now); }

        // ---- the card ----
        float in = Math.min(1f, t / 500f);
        float out = Math.min(1f, (a.durationMs() - t) / 500f);
        float slide = Math.min(easeOutBack(in), out);

        int boxW = 200 + tier * 22;
        int boxH = 46 + tier * 4;
        int boxX = (sw - boxW) / 2;
        int boxY = (int) (-boxH + (boxH + 14) * slide);

        if (t < 150 && PARTICLES.size() < 600) burst(sw / 2f, boxY + boxH / 2f, 6 + tier * 14, tier, now);
        if (tier >= 3 && now - lastBurst > 600 / (tier - 1)) {
            lastBurst = now;
            if (PARTICLES.size() < 700) burst(boxX + RNG.nextInt(boxW), boxY + boxH, tier * 5, tier, now);
        }

        float shakeX = 0, shakeY = 0;
        if (DonoAlertsClient.CONFIG.screenShake && tier >= 4 && t < (tier == 6 ? 3000 : 1200)) {
            float dur = tier == 6 ? 3000f : 1200f;
            float k = (1f - t / dur) * (tier - 2);
            shakeX = (RNG.nextFloat() - 0.5f) * 2 * k; shakeY = (RNG.nextFloat() - 0.5f) * 2 * k;
        }

        g.pose().pushMatrix();
        g.pose().translate(shakeX, shakeY);

        int border = tier == 6 ? rainbow(now, 0f) : color;
        g.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, border);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xEA101018);
        int pulse = 110 + (int) (100 * Math.sin(now / 200.0 + tier));
        g.fill(boxX, boxY + boxH - 3, boxX + boxW, boxY + boxH, (pulse << 24) | (border & 0xFFFFFF));

        // coin icon
        int ix = boxX + 8, iy = boxY + 8, is = 26;
        g.fill(ix, iy, ix + is, iy + is, 0xFF000000 | (border & 0xFFFFFF));
        g.fill(ix + 3, iy + 3, ix + is - 3, iy + is - 3, 0xFFFFD700);
        g.fill(ix + 8, iy + 8, ix + is - 8, iy + is - 8, 0xFFC99A00);

        // amount
        float scale = 1.6f + tier * 0.35f;
        g.pose().pushMatrix();
        g.pose().translate(boxX + 42, boxY + 8);
        g.pose().scale(scale, scale);
        g.text(font, a.amountText(), 0, 0, tier == 6 ? rainbow(now, 0.1f) : color, true);
        g.pose().popMatrix();

        int ty = boxY + 8 + (int) (9 * scale) + 4;
        g.text(font, a.name + " paid you!", boxX + 42, ty, 0xFFFFFFFF, true);
        g.text(font, a.tag(), boxX + boxW - 8 - font.width(a.tag()), boxY + 8, tier == 6 ? rainbow(now, 0.5f) : color, true);

        g.pose().popMatrix();

        // ---- giant banner text for the big ones ----
        if (tier >= 5 && t < 3500) {
            float bt = t / 3500f;
            float pop = t < 300 ? easeOutBack(t / 300f) : 1f;
            int ba = (int) (255 * Math.min(1f, (1f - bt) * 3f));
            int bcol = (Math.max(30, ba) << 24) | ((tier == 6 ? rainbow(now, 0f) : 0xFFFF3B3B) & 0xFFFFFF);
            String word = tier == 6 ? "OMEGA" : "INSANE";
            bigCenter(g, font, word, sw / 2, sh / 3, (tier == 6 ? 7f : 5f) * pop, bcol);
            bigCenter(g, font, a.amountText(), sw / 2, sh / 3 + (tier == 6 ? 62 : 46), 3.5f * pop, bcol);
        }
    }
}
