import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;


public class TestPro extends JPanel implements ActionListener {
    private Timer timer;
    private long startTime;
    private int sceneState = 0; // 0 = โลงศพ, 1 = กำลังเปลี่ยนฉาก, 2 = โทรศัพท์
    private long transitionStart = 0;

    public TestPro() {
        startTime = System.currentTimeMillis();
        timer = new Timer(20, this);
        timer.start();
    }

    public static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);
        Graphics2D g2 = (Graphics2D) g;

        long elapsed = System.currentTimeMillis() - startTime;
        double seconds = elapsed / 1000.0;

        // ใช้ sine wave เพื่อให้ scale แกว่งขึ้นลง
        // scale ดอกตอนขยาย & ตอนหด
        float scale = (float)(0.45 + 0.25 * Math.sin(seconds * 1.5 * Math.PI / 2)); // รอบละ 2 วิ

        int flowerSpacing = 100;

        for (int y = flowerSpacing / 2; y <= getHeight(); y += flowerSpacing) {
            for (int x = flowerSpacing / 2; x <= getWidth(); x += flowerSpacing) {
                AffineTransform old = g2.getTransform();
                g2.translate(x, y);
                g2.scale(scale, scale);
                drawFlower(g2);
                g2.setTransform(old);
            }
        }
        //drawFullCoffin(g2, elapsed, getWidth(), getHeight());
            // เริ่มเปลี่ยนฉากหลังจาก 5 วินาที
        if (sceneState == 0 && elapsed > 5000) {
            sceneState = 1;
            transitionStart = elapsed;
        }

        // วาดฉากตามสถานะ
        if (sceneState == 0) {
            // ฉากโลงศพ
            drawFullCoffin(g2, elapsed, getWidth(), getHeight());
        } else if (sceneState == 1) {
            // กำลังเปลี่ยนฉาก (fade)
            long transElapsed = elapsed - transitionStart;
            drawSceneTransition(g2, getWidth(), getHeight(), transElapsed, 2000, drawPhoneRing(g2));
            // เมื่อครบเวลา transition ให้เปลี่ยนไปฉากโทรศัพท์
            if (transElapsed > 2000) {
                sceneState = 2;
            }
        } else if (sceneState == 2) {
            // ฉากโทรศัพท์
            drawPhoneRing(g2).run();
        }

    }

    private void drawFlower(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        for (int i = 0; i < 8; i++) {
            g2.fillOval(-30, -80, 65, 65);
            g2.rotate(Math.PI / 4);
        }
        g2.setColor(Color.BLACK);
        g2.fillOval(-20, -20, 40, 40);
    }
    private void drawFullCoffin(Graphics2D g2, long elapsed, int panelWidth, int panelHeight) {
        int cx = 300;
        int cy = 300;

        // ====== วาดโลงฐาน ======
        Polygon base = new Polygon();
        base.addPoint(cx - 100, cy - 200);   // บนซ้าย(200,100)
        base.addPoint(cx + 100, cy - 200);   // บนขวา(400,100)
        base.addPoint(cx + 200, cy);         // กลางขวา(500,300)
        base.addPoint(cx + 120 , cy * 2);    // ล่างขวา(420,600)
        base.addPoint(cx - 120, cy * 2);    // ล่างซ้าย(180,600)
        base.addPoint(cx - 200 , cy);       // กลางซ้าย(100,300)

        g2.setColor(new Color(102, 51, 0));  // น้ำตาลเข้ม
        g2.fillPolygon(base);

        // ====== คนที่นอนในโลง (เห็นแค่ครึ่งตัวบน) ======
        drawLyingPerson(g2, cx, cy, 500);

        // ====== ฝาโลงเคลื่อนออกด้านขวา ======
        float progress = Math.min(1.0f, elapsed / 3000f); // 3 วินาทีเปิดฝา
        int xOffset = (int)(progress * (panelWidth * 0.5)); // เคลื่อนฝาไปทางขวา

        AffineTransform old = g2.getTransform();
        g2.translate(xOffset, 0); // เคลื่อนฝาออก

        // ฝาโลงเหมือนฐาน
        Polygon lid = new Polygon();
        lid.addPoint(cx - 100, cy - 200);       // บนซ้าย(200,100)
        lid.addPoint(cx + 100, cy - 200);       // บนขวา(400,100)
        lid.addPoint(cx + 200, cy);             // กลางขวา(500,300)
        lid.addPoint(cx + 120 , cy * 2);    // ล่างขวา(420,600)
        lid.addPoint(cx - 120, cy * 2);    // ล่างซ้าย(180,600)
        lid.addPoint(cx - 200 , cy);        // กลางซ้าย(100,300)


        g2.setColor(new Color(153, 102, 0)); // ฝาสีน้ำตาลอ่อน
        g2.fillPolygon(lid);

        // ไม้กางเขน
        int crossCenterX = cx;
        int crossCenterY = cy - 500/4; // ขยับขึ้นเล็กน้อย
        int crossWidth = (int)(panelWidth * 0.05);
        int crossHeight = (int)(450 * 0.35);
        int crossBarWidth = (int)(panelWidth * 0.16);
        int crossBarHeight = (int)(300 * 0.08);

        g2.setColor(new Color(240, 220, 150)); // สีเหลืองทอง
        // แกนยาว (vertical)
        g2.fillRect(crossCenterX - crossWidth/2, crossCenterY - crossHeight/8+25, crossWidth, crossHeight);
        // แกนขวาง (horizontal)
        g2.fillRect(crossCenterX - crossBarWidth/2, crossCenterY - crossBarHeight+60, crossBarWidth, crossBarHeight);

        g2.setTransform(old); // รีเซ็ต transform
    }

    private void drawLyingPerson(Graphics2D g2, int cx, int cy, int height) {
        
        int headRadius = (int)(height * 0.175);
        int headY = cy - height / 2 + (int)(height * 0.2);

        //hair
        drawHair(g2, cx, headY, headRadius);
        // หัว
        drawFace(g2, cx, headY, headRadius);
        //Bangs
        drawbang(g2, cx, headY, headRadius);
        
        
        g2.setColor(new Color(0xDEAA7F)); // สีผิวคน

        // ลำตัว (ครึ่งบน)
        drawBody(g2, cx, cy, height, headRadius, 0xDEAA7F);

        // // แขน
        // g2.fillRect(cx - (int)(height * 0.11), bodyY + (int)(height * 0.03), (int)(height * 0.08), (int)(height * 0.04)); // ซ้าย
        // g2.fillRect(cx + (int)(height * 0.03), bodyY + (int)(height * 0.03), (int)(height * 0.08), (int)(height * 0.04)); // ขวา
        
    }

    void drawHair(Graphics2D g, int cx, int headY, int headRadius){
        double scale = headRadius / 70.0; // ปรับขนาดตาม headRadius
        int dx = cx - (int)(334 * scale); // ทำให้จุดเริ่มต้นตรงกลางหัว
        int dy = headY - (int)(116* scale);

        GeneralPath hairPath = new GeneralPath();
        hairPath.moveTo((int)((293 + dx) * scale), (int)((224 + dy) * scale));
        hairPath.curveTo((int)((285 + dx) * scale), (int)((171 + dy) * scale), (int)((291 + dx) * scale), (int)((156 + dy) * scale), (int)((323 + dx) * scale), (int)((130 + dy) * scale));
        hairPath.curveTo((int)((355 + dx) * scale), (int)((105 + dy) * scale), (int)((411 + dx) * scale), (int)((118 + dy) * scale), (int)((430 + dx) * scale), (int)((163 + dy) * scale));
        hairPath.curveTo((int)((449 + dx) * scale), (int)((209 + dy) * scale), (int)((411 + dx) * scale), (int)((253 + dy) * scale), (int)((448 + dx) * scale), (int)((287 + dy) * scale));
        hairPath.curveTo((int)((484 + dx) * scale), (int)((322 + dy) * scale), (int)((434 + dx) * scale), (int)((323 + dy) * scale), (int)((455 + dx) * scale), (int)((334 + dy) * scale));
        hairPath.curveTo((int)((474 + dx) * scale), (int)((346 + dy) * scale), (int)((309 + dx) * scale), (int)((343 + dy) * scale), (int)((286 + dx) * scale), (int)((334 + dy) * scale));
        hairPath.curveTo((int)((263 + dx) * scale), (int)((323 + dy) * scale), (int)((293 + dx) * scale), (int)((325 + dy) * scale), (int)((283 + dx) * scale), (int)((313 + dy) * scale));
        hairPath.curveTo((int)((269 + dx) * scale), (int)((295 + dy) * scale), (int)((305 + dx) * scale), (int)((287 + dy) * scale), (int)((293 + dx) * scale), (int)((224 + dy) * scale));
        hairPath.closePath();
        g.setColor(new Color(238, 157, 236)); // สีชมพู
        g.fill(hairPath);
        g.setColor(Color.BLACK);
        g.draw(hairPath);
    }

    private void drawFace(Graphics2D g2d, int cx, int headY, int headRadius) {
        double scale = headRadius / 90.0 * 1.3; // ปรับขนาดตาม headRadius
        int dx = cx - (int)(327 * scale); // จุดเริ่มต้นตรงกลางหัว
        int dy = headY - (int)(121 * scale);

        GeneralPath face = new GeneralPath();
        face.moveTo((int)((314+dx)*scale),(int)((174+dy)*scale));
        face.curveTo((int)((314+dx)*scale),(int)((174+dy)*scale),(int)((261+dx)*scale),(int)((257+dy)*scale),(int)((367+dx)*scale),(int)((239+dy)*scale));
        face.curveTo((int)((473+dx)*scale),(int)((220+dy)*scale),(int)((355+dx)*scale),(int)((86+dy)*scale),(int)((314+dx)*scale),(int)((174+dy)*scale));
        g2d.setColor(new Color(0xDEAA7F)); // สีผิว
        g2d.fill(face);
        g2d.setColor(new Color(0xDEAA7F));
        g2d.draw(face);

        // วาดตา
        // --- กระพริบตา ---
        long elapsed = System.currentTimeMillis() - startTime;
        double t = (elapsed % 2200) / 2200.0; // รอบละ 2.2 วินาที
        // เริ่มต้นหลับตา (h เล็กมาก) แล้วค่อยๆ ลืมตา (h เพิ่มขึ้น)
        // หลับสนิทค้างไว้ 1 วินาทีแรก
        double blink;
        if (elapsed < 1000) {
            blink = 0; // หลับสนิท
        } else {
            blink = Math.min(1, Math.max(0, (t - 0.15) * 1.2)); // 0=หลับ, 1=ลืมตา
        }
        double eyeW = headRadius * 0.34; // ปรับให้เล็กลงจากเดิม
        double eyeH = headRadius * (0.08 + 0.24 * blink); // h เล็กตอนหลับ, ใหญ่ตอนลืม
        double eyeY = headY + headRadius * 1.21;    
        drawAnimeEye(g2d, (cx - headRadius * 0.36)+11, eyeY, eyeW, eyeH, 0, 0, 0, new Color(120, 180, 255)); // ตาซ้าย
        drawAnimeEye(g2d, (cx + headRadius * 0.36)+11, eyeY, eyeW, eyeH, 0, 0, 0, new Color(120, 180, 255)); // ตาขวา

        // วาดปาก
        g2d.setColor(Color.BLACK);
        int mouthW = (int)(headRadius * 0.38);
        int mouthH = (int)(headRadius * 0.18);
        int mouthX = cx - mouthW / 2;
        int mouthY = (int)(headY + headRadius * 1.45);
        g2d.drawArc(mouthX, mouthY, mouthW, mouthH, 0, -160);
    }

    public void drawbang(Graphics2D g2d, int cx, int headY, int headRadius){
        double scale = headRadius / 90.0 * 1.3;
        int dx = cx - (int)(337 * scale); // จุดเริ่มต้นตรงกลางหัว
        int dy = headY - (int)(121 * scale);

        GeneralPath bang  = new GeneralPath();
        bang.moveTo((int)((341+dx)*scale), (int)((144+dy)*scale));
        bang.curveTo((int)((341+dx)*scale), (int)((144+dy)*scale), (int)((298+dx)*scale), (int)((191+dy)*scale), (int)((385+dx)*scale), (int)((191+dy)*scale));
        bang.curveTo((int)((471+dx)*scale), (int)((191+dy)*scale), (int)((374+dx)*scale), (int)((111+dy)*scale), (int)((341+dx)*scale), (int)((144+dy)*scale));
        g2d.setColor(new Color(238, 157, 236)); // สีชมพู
        g2d.fill(bang);

    }

    public static void drawAnimeEye(Graphics2D g2, double cx, double cy, double w, double h, double lookX, double lookY, double tiltDeg, Color irisColor) {
        smooth(g2);
        AffineTransform old = g2.getTransform();
        g2.rotate(Math.toRadians(tiltDeg), cx, cy);

        // Taller shape
        Shape sclera = new RoundRectangle2D.Double(cx - w/2, cy - h/2, w*0.78 , h*0.84, w*0.2, h*0.9);
        g2.setColor(Color.WHITE);
        g2.fill(sclera);

        // Bold upper lid
        g2.setStroke(new BasicStroke((float)Math.max(2, h*0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.BLACK);
        g2.draw(new QuadCurve2D.Double(cx - w*0.51, cy - h*0.45, cx,cy - h*0.55, cx + w*0.31, cy - h*0.45));

        // Thin outline around
        g2.setStroke(new BasicStroke((float)Math.max(1, h*0.06)));
        g2.draw(sclera);

        // Iris + pupil (slightly larger)
        double irisR = Math.min(w, h) * 0.33;
        double pupilR = irisR * 0.72;
        double maxOffset = (Math.min(w, h)*0.45) - irisR*1.05;
        double px = cx + clamp(lookX, -1, 1) * (maxOffset);
        double py = cy + clamp(lookY, -1, 1) * (maxOffset*0.75);

        Shape iris = new Ellipse2D.Double((px - irisR)-3, py - irisR, irisR*2, irisR*2);
        g2.setColor(irisColor);
        g2.fill(iris);
        g2.setColor(Color.black);
        g2.draw(iris);

        Shape pupil = new Ellipse2D.Double((px - pupilR)-3, py - pupilR, pupilR*2, pupilR*2);
        g2.setColor(Color.BLACK);
        g2.fill(pupil);

        // Multiple highlights
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(px - pupilR*0.2, py - pupilR*0.9, pupilR*0.8, pupilR*0.9));
        g2.fill(new Ellipse2D.Double(px + pupilR*0.2, py - pupilR*0.1, pupilR*0.35, pupilR*0.35));

        // Small lashes at the outer corner
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke((float)Math.max(1, h*0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i=0; i<3; i++) {
            double t = i / 2.0;
            double x1 = cx + w*0.3;
            double y1 = cy - h*0.5 + i*h*0.08;
            double x2 = x1 + w*0.18;
            double y2 = y1 - h*0.10 + t*h*0.06;
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        g2.setTransform(old);
    }

    private void drawBody(Graphics2D g2, int cx, int cy, int bodyW, int bodyH, int skinColor) {
        // คอ
        int neckW = (int)(bodyW * 0.12);
        int neckH = (int)(bodyH * 0.48);
        int neckX = (cx - neckW / 2)+5;
        int neckY = cy-10;

        g2.setColor(new Color(skinColor));
        g2.fillRoundRect(neckX, neckY, neckW, neckH, neckW/2, neckH/2);
         // สัดส่วนไหล่
        int shoulderW = (int)(bodyW * 0.37);
        int shoulderH = (int)(bodyH * 0.42);
        int shoulderX = (cx - shoulderW / 2)+5;
        int shoulderY = neckY + neckH-5;
        //สัดส่วนตัว
        int torsoTopW = (int)(bodyW * 0.37); // กว้างช่วงไหล่
        int torsoBotW = (int)(bodyW * 0.22); // กว้างช่วงเอว
        int torsoH = (int)(bodyH * 1.1);
        int torsoY = shoulderY + shoulderH - 10;

        // กำหนดขนาดช่วงเอวและสะโพก
        int waistW = (int)(bodyW * 0.22);   // กว้างช่วงเอว
        int hipW   = (int)(bodyW * 0.37);   // กว้างช่วงสะโพก
        int lowerH = (int)(bodyH * 0.7);    // ความสูงครึ่งล่าง

        // สี่เหลี่ยมคางหมู (สะโพก)
        Polygon lowerTorso = new Polygon();
        lowerTorso.addPoint((cx - waistW/2)+5, (int) (torsoY*1.26));            // บนซ้าย (เอว)
        lowerTorso.addPoint((cx + waistW/2)+5,(int) (torsoY*1.26));            // บนขวา (เอว)
        lowerTorso.addPoint((cx + hipW/2)+5, (int) (torsoY*1.5) + lowerH);     // ล่างขวา (สะโพก)
        lowerTorso.addPoint((cx - hipW/2)+5, (int) (torsoY*1.5) + lowerH);     // ล่างซ้าย (สะโพก)

        g2.setColor(Color.darkGray); // สีชุด
        g2.fillPolygon(lowerTorso);

        // แขนซ้าย
        int armW = (int)(bodyW * 0.074);
        int armH = (int)(bodyH * 1.52);
        int armY = cy + (int)(bodyH * 0.4);
        int armXLeft = cx - (int)(bodyW * 0.24)/2 - armW + 5;

        g2.setColor(new Color(skinColor));
        g2.fillRoundRect(armXLeft, armY, armW, armH, armW/2, armW);

        // แขนขวา
        int armXRight = cx + (int)(bodyW * 0.24)/2 + 5;
        g2.fillRoundRect(armXRight, armY, armW, armH, armW/2, armW);

        // มือ (วงรีเล็กๆ)
        int handW = (int)(armW * 0.9);
        int handH = (int)(armW * 0.9);
        g2.fillOval(armXLeft + armW/2 - handW/2, armY + armH - handH/2, handW, handH); // มือซ้าย
        g2.fillOval(armXRight + armW/2 - handW/2, armY + armH - handH/2, handW, handH); // มือขวา

        //วาดไหล่
        g2.setColor(new Color(240, 240, 240)); // สีเสื้อ
        g2.fillRoundRect(shoulderX, shoulderY, shoulderW, shoulderH, 30, 30);

        // ตัว (ครึ่งบน)
        
        Polygon torso = new Polygon();
        torso.addPoint((cx - torsoTopW/2)+5, torsoY);           // บนซ้าย
        torso.addPoint((cx + torsoTopW/2)+5, torsoY);           // บนขวา
        torso.addPoint((cx + torsoBotW/2)+5, torsoY + torsoH);  // ล่างขวา
        torso.addPoint((cx - torsoBotW/2)+5, torsoY + torsoH);  // ล่างซ้าย
        g2.setColor(new Color(240, 240, 240)); // สีเสื้อ
        g2.fillPolygon(torso);

        // ปกเสื้อเชิ้ตสามเหลี่ยม
        Polygon leftCollar = new Polygon();
        leftCollar.addPoint((cx - neckW/2)+5, neckY + neckH-10);
        leftCollar.addPoint(cx+5, neckY + neckH + 18);
        leftCollar.addPoint((cx - neckW)+5, neckY + neckH + 18);
        g2.setColor(Color.WHITE);
        g2.fillPolygon(leftCollar);

        Polygon rightCollar = new Polygon();
        rightCollar.addPoint((cx + neckW/2)+5, neckY + neckH-10);
        rightCollar.addPoint(cx+5, neckY + neckH + 18);
        rightCollar.addPoint((cx + neckW)+5, neckY + neckH + 18);
        g2.setColor(Color.WHITE);
        g2.fillPolygon(rightCollar);

        Polygon neck = new Polygon();
        neck.addPoint((cx - neckW/2)+5, neckY + neckH-10);
        neck.addPoint((cx + neckW/2)+5, neckY + neckH-10);
        neck.addPoint(cx+5, neckY + neckH + 18);
        g2.setColor(new Color(skinColor));
        g2.fillPolygon(neck);

        // รอยกระดุม
        g2.setColor(Color.GRAY);
        for (int i = 0; i < 5; i++) {
            g2.fillOval(cx, neckY + neckH + 30 + i*18, 8, 8);
        }
    }

    private void drawSceneTransition(Graphics2D g2, int panelWidth, int panelHeight, long elapsed, long duration, Runnable drawNewScene) {
        // elapsed: เวลาที่ผ่านไป (ms)
        // duration: เวลารวมของ transition (ms)
        // drawNewScene: โค้ดวาดฉากใหม่ (Runnable)

        float progress = Math.min(1f, elapsed / (float)duration);

        // Fade out (ครึ่งแรก), Fade in (ครึ่งหลัง)
        if (progress < 0.5f) {
            // วาดฉากเดิม (ถ้ามี)
            // drawOldScene(g2); // ถ้ามี method วาดฉากเดิม
            // วาด overlay สีดำค่อยๆ ทึบขึ้น
            float alpha = progress * 2; // 0 ถึง 1
            g2.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        } else {
            // วาดฉากใหม่
            drawNewScene.run();
            // วาด overlay สีดำค่อยๆ จางลง
            float alpha = 1f - ((progress - 0.5f) * 2); // 1 ถึง 0
            g2.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }
    }
    private Runnable drawPhoneRing(Graphics2D g2) {
    return () -> {
        
        smooth(g2);

        int panelW = getWidth();
        int panelH = getHeight();

        // พื้นหลัง
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(0, 0, panelW, panelH);

        // เวลาสำหรับสั่น
        long elapsed = System.currentTimeMillis() - startTime;
        double shake = Math.sin(elapsed * 0.015) * 3; // สั่นซ้ายขวา

        // วาดโทรศัพท์
        int phoneW = 120, phoneH = 220;
        int phoneX = panelW/2 - phoneW/2 + (int)shake;
        int phoneY = panelH/2 - phoneH/2;

        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(phoneX, phoneY, phoneW, phoneH, 30, 30);

        // หน้าจอ
        g2.setColor(new Color(220, 255, 220));
        g2.fillRoundRect(phoneX+12, phoneY+28, phoneW-24, phoneH-80, 18, 18);

        // ปุ่ม Home
        g2.setColor(new Color(180,180,180));
        g2.fillOval(phoneX+phoneW/2-14, phoneY+phoneH-32, 28, 28);

        // ข้อความ "Midterm calling"
        g2.setColor(new Color(40, 120, 40));
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("Midterm calling", phoneX+18, phoneY+phoneH/2);

        // สัญลักษณ์สายโทรเข้า
        g2.setColor(new Color(80, 180, 80));
        g2.setStroke(new BasicStroke(6));
        g2.drawArc(phoneX+40, phoneY+phoneH/2+30, 40, 40, 30, 120);

        // วงกลมสายเข้า
        g2.setColor(new Color(80, 180, 80));
        g2.fillOval(phoneX+phoneW/2-16, phoneY+phoneH/2+60, 32, 32);
    };
}
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Breathing Flowers");
        TestPro panel = new TestPro();
        frame.add(panel);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
