import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;



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
        
        drawBackground(g2, getWidth(), getHeight());
        smooth(g2);
        
        //เริ่มเปลี่ยนฉากหลังจาก 1 วินาที
        if (sceneState == 0 && elapsed > 1000) {
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
            drawSceneTransition(g2, getWidth(), getHeight(), transElapsed, 500, drawPhoneRing(g2));
            // เมื่อครบเวลา transition ให้เปลี่ยนไปฉากโทรศัพท์
            if (transElapsed > 400) {
                sceneState = 2 ;
            }
        } else if (sceneState == 2) {
            // ฉากโทรศัพท์
            drawPhoneRing(g2).run();
        }

    }

    private void drawBackground(Graphics2D g, int width, int height) {
        // ใช้ sine wave เพื่อให้ scale แกว่งขึ้นลง
        // scale ดอกตอนขยาย & ตอนหด
        long elapsed = System.currentTimeMillis() - startTime;
        double seconds = elapsed / 1000.0;
        float scale = (float)(0.25 + 0.05 * Math.sin(seconds * 1.5 * Math.PI / 2)); // รอบละ 2 วิ

        int flowerSpacing = 100;

        for (int y = flowerSpacing / 2; y <= getHeight(); y += flowerSpacing) {
            for (int x = flowerSpacing / 2; x <= getWidth(); x += flowerSpacing) {
                AffineTransform old = g.getTransform();
                g.translate(x, y);
                g.scale(scale, scale);
                drawFlower(g);
                g.setTransform(old);
            }
        }
    }

    private void drawFlower(Graphics2D g2) {
        // ===== วาดกลีบดอกด้วย Midpoint Ellipse =====
        for (int i = 0; i < 8; i++) {
            Polygon petal = new Polygon();
            int xc = -30, yc = -80;
            int rx = 65, ry = 65;
            int x = 0;
            int y = ry;

            double d1 = (ry * ry) - (rx * rx * ry) + (0.25 * rx * rx);
            int dx = 2 * ry * ry * x;
            int dy = 2 * rx * rx * y;

            // region 1
            while (dx < dy) {
                petal.addPoint(xc + x, yc + y);
                petal.addPoint(xc - x, yc + y);
                petal.addPoint(xc + x, yc - y);
                petal.addPoint(xc - x, yc - y);
                if (d1 < 0) {
                    x++;
                    dx += 2 * ry * ry;
                    d1 += dx + (ry * ry);
                } else {
                    x++;
                    y--;
                    dx += 2 * ry * ry;
                    dy -= 2 * rx * rx;
                    d1 += dx - dy + (ry * ry);
                }
            }

            // region 2
            double d2 = ((ry * ry) * ((x + 0.5) * (x + 0.5)))
                    + ((rx * rx) * ((y - 1) * (y - 1)))
                    - (rx * rx * ry * ry);

            while (y >= 0) {
                petal.addPoint(xc + x, yc + y);
                petal.addPoint(xc - x, yc + y);
                petal.addPoint(xc + x, yc - y);
                petal.addPoint(xc - x, yc - y);
                if (d2 > 0) {
                    y--;
                    dy -= 2 * rx * rx;
                    d2 += (rx * rx) - dy;
                } else {
                    y--;
                    x++;
                    dx += 2 * ry * ry;
                    dy -= 2 * rx * rx;
                    d2 += dx - dy + (rx * rx);
                }
            }

            g2.setColor(Color.WHITE);
            g2.fillPolygon(petal);
            g2.rotate(Math.PI / 4); // หมุนกลีบทีละ 45 องศา
        }
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

        // Gradient น้ำตาลเข้ม → น้ำตาลอ่อน
        GradientPaint coffinGradient = new GradientPaint(
            cx, cy - 200, new Color(60, 30, 10),   // ด้านบนเข้มมาก
            cx, cy * 2, new Color(153, 102, 0)     // ด้านล่างน้ำตาลอ่อน
        );
        g2.setPaint(coffinGradient);
        g2.fillPolygon(base);
        // ขอบโลง
        g2.setStroke(new BasicStroke(3)); // ความหนาเส้นขอบ
        g2.setColor(new Color(50, 25, 5));
        g2.drawPolygon(base);
        g2.setStroke(new BasicStroke(1)); // คืนความหนาเส้นขอบ

        // ====== คนที่นอนในโลง (เห็นแค่ครึ่งตัวบน) ======
        drawLyingPerson(g2, cx, cy, 500);

        // ====== ฝาโลงเคลื่อนออกด้านขวา ======
        float progress = Math.min(1.0f, elapsed / 1000f); // 3 วินาทีเปิดฝา
        int xOffset = (int)(progress * (panelWidth * 0.5)); // เคลื่อนฝาไปทางขวา

        g2.translate(xOffset, 0); // เคลื่อนฝาออก

        // ฝาโลงเหมือนฐาน
        Polygon lid = new Polygon();
        lid.addPoint(cx - 100, cy - 200);       // บนซ้าย(200,100)
        lid.addPoint(cx + 100, cy - 200);       // บนขวา(400,100)
        lid.addPoint(cx + 200, cy);             // กลางขวา(500,300)
        lid.addPoint(cx + 120 , cy * 2);    // ล่างขวา(420,600)
        lid.addPoint(cx - 120, cy * 2);    // ล่างซ้าย(180,600)
        lid.addPoint(cx - 200 , cy);        // กลางซ้าย(100,300)

        g2.setPaint(coffinGradient);
        g2.fillPolygon(lid);
        // ขอบโลง
        g2.setStroke(new BasicStroke(3)); // ความหนาเส้นขอบ
        g2.setColor(new Color(50, 25, 5));
        g2.drawPolygon(base);
        g2.setStroke(new BasicStroke(1)); // คืนความหนาเส้นขอบ

       // ไม้กางเขน
        int crossCenterX = cx;
        int crossCenterY = cy - 500/4; // ขยับขึ้นเล็กน้อย
        int crossWidth = (int)(600 * 0.05);
        int crossHeight = (int)(450 * 0.35);
        int crossBarWidth = (int)(600 * 0.16);
        int crossBarHeight = (int)(300 * 0.08);

        // สีทอง
        g2.setColor(new Color(240, 220, 150));

        // --- แกนยาว (vertical) ---
        Polygon vertical = new Polygon();
        vertical.addPoint(crossCenterX - crossWidth / 2, crossCenterY - crossHeight / 8 + 25); // บนซ้าย
        vertical.addPoint(crossCenterX + crossWidth / 2, crossCenterY - crossHeight / 8 + 25); // บนขวา
        vertical.addPoint(crossCenterX + crossWidth / 2, crossCenterY - crossHeight / 8 + 25 + crossHeight); // ล่างขวา
        vertical.addPoint(crossCenterX - crossWidth / 2, crossCenterY - crossHeight / 8 + 25 + crossHeight); // ล่างซ้าย
        g2.fillPolygon(vertical);
        

        // --- แกนขวาง (horizontal) ---
        g2.setColor(new Color(240, 220, 150));
        Polygon horizontal = new Polygon();
        horizontal.addPoint(crossCenterX - crossBarWidth / 2, crossCenterY - crossBarHeight + 60); // ซ้ายบน
        horizontal.addPoint(crossCenterX + crossBarWidth / 2, crossCenterY - crossBarHeight + 60); // ขวาบน
        horizontal.addPoint(crossCenterX + crossBarWidth / 2, crossCenterY - crossBarHeight + 60 + crossBarHeight); // ขวาล่าง
        horizontal.addPoint(crossCenterX - crossBarWidth / 2, crossCenterY - crossBarHeight + 60 + crossBarHeight); // ซ้ายล่าง
        g2.fillPolygon(horizontal);
        
        
        g2.setStroke(new BasicStroke(2)); // คืนความหนาเส้นขอบ
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
        // ลำตัว (ครึ่งบน)
        drawBody(g2, cx, cy, height, headRadius, 0xDEAA7F);

    }

    void drawHair(Graphics2D g, int cx, int headY, int headRadius) {
        double scale = headRadius / 70.0;
        int dx = cx - (int)(334 * scale);
        int dy = headY - (int)(116 * scale);

        Polygon hair = new Polygon();

        // Bezier helper (cubic)
        java.util.function.BiConsumer<int[], Polygon> bezier = (pts, poly) -> {
            int steps = 100; // ความละเอียดของเส้นโค้ง
            for (int tStep = 0; tStep <= steps; tStep++) {
                double t = tStep / (double) steps;
                double x = Math.pow(1 - t, 3) * pts[0] +
                        3 * Math.pow(1 - t, 2) * t * pts[2] +
                        3 * (1 - t) * t * t * pts[4] +
                        Math.pow(t, 3) * pts[6];
                double y = Math.pow(1 - t, 3) * pts[1] +
                        3 * Math.pow(1 - t, 2) * t * pts[3] +
                        3 * (1 - t) * t * t * pts[5] +
                        Math.pow(t, 3) * pts[7];
                poly.addPoint((int) x, (int) y);
            }
        };

        // สร้างเส้นโค้งต่อเนื่อง
        bezier.accept(new int[]{(int)((293+dx)*scale), (int)((224+dy)*scale),
                                (int)((285+dx)*scale), (int)((171+dy)*scale),
                                (int)((291+dx)*scale), (int)((156+dy)*scale),
                                (int)((323+dx)*scale), (int)((130+dy)*scale)}, hair);

        bezier.accept(new int[]{(int)((323+dx)*scale), (int)((130+dy)*scale),
                                (int)((355+dx)*scale), (int)((105+dy)*scale),
                                (int)((411+dx)*scale), (int)((118+dy)*scale),
                                (int)((430+dx)*scale), (int)((163+dy)*scale)}, hair);

        bezier.accept(new int[]{(int)((430+dx)*scale), (int)((163+dy)*scale),
                                (int)((449+dx)*scale), (int)((209+dy)*scale),
                                (int)((411+dx)*scale), (int)((253+dy)*scale),
                                (int)((448+dx)*scale), (int)((287+dy)*scale)}, hair);

        bezier.accept(new int[]{(int)((448+dx)*scale), (int)((287+dy)*scale),
                                (int)((484+dx)*scale), (int)((322+dy)*scale),
                                (int)((434+dx)*scale), (int)((323+dy)*scale),
                                (int)((455+dx)*scale), (int)((334+dy)*scale)}, hair);

        bezier.accept(new int[]{(int)((455+dx)*scale), (int)((334+dy)*scale),
                                (int)((474+dx)*scale), (int)((346+dy)*scale),
                                (int)((309+dx)*scale), (int)((343+dy)*scale),
                                (int)((286+dx)*scale), (int)((334+dy)*scale)}, hair);

        bezier.accept(new int[]{(int)((286+dx)*scale), (int)((334+dy)*scale),
                                (int)((263+dx)*scale), (int)((323+dy)*scale),
                                (int)((293+dx)*scale), (int)((325+dy)*scale),
                                (int)((283+dx)*scale), (int)((313+dy)*scale)}, hair);

        bezier.accept(new int[]{(int)((283+dx)*scale), (int)((313+dy)*scale),
                                (int)((269+dx)*scale), (int)((295+dy)*scale),
                                (int)((305+dx)*scale), (int)((287+dy)*scale),
                                (int)((293+dx)*scale), (int)((224+dy)*scale)}, hair);

        // ปิดรูป
        hair.addPoint((int)((293+dx)*scale), (int)((224+dy)*scale));

        // วาดผม
        g.setColor(new Color(238, 157, 236)); // สีชมพู
        g.fillPolygon(hair);
        g.setColor(Color.BLACK);
        g.drawPolygon(hair);
    }


    private void drawFace(Graphics2D g2d, int cx, int headY, int headRadius) {
        double scale = headRadius / 90.0 * 1.3;
        int dx = cx - (int)(327 * scale);
        int dy = headY - (int)(121 * scale);

        Polygon face = new Polygon();

        // ===== Bezier Helper =====
        java.util.function.BiConsumer<int[], Polygon> bezier = (pts, poly) -> {
            int steps = 100;
            for (int tStep = 0; tStep <= steps; tStep++) {
                double t = tStep / (double) steps;
                double x = Math.pow(1 - t, 3) * pts[0] +
                        3 * Math.pow(1 - t, 2) * t * pts[2] +
                        3 * (1 - t) * t * t * pts[4] +
                        Math.pow(t, 3) * pts[6];
                double y = Math.pow(1 - t, 3) * pts[1] +
                        3 * Math.pow(1 - t, 2) * t * pts[3] +
                        3 * (1 - t) * t * t * pts[5] +
                        Math.pow(t, 3) * pts[7];
                poly.addPoint((int) x, (int) y);
            }
        };

        // ===== Face Shape =====
        bezier.accept(new int[]{(int)((314+dx)*scale), (int)((174+dy)*scale),
                                (int)((314+dx)*scale), (int)((174+dy)*scale),
                                (int)((261+dx)*scale), (int)((257+dy)*scale),
                                (int)((367+dx)*scale), (int)((239+dy)*scale)}, face);

        bezier.accept(new int[]{(int)((367+dx)*scale), (int)((239+dy)*scale),
                                (int)((473+dx)*scale), (int)((220+dy)*scale),
                                (int)((355+dx)*scale), (int)((86+dy)*scale),
                                (int)((314+dx)*scale), (int)((174+dy)*scale)}, face);

        // วาดใบหน้า
        g2d.setColor(new Color(0xDEAA7F)); // skin
        g2d.fillPolygon(face);
        g2d.setColor(new Color(0xDEAA7F));
        g2d.drawPolygon(face);

        // วาดตา
        // --- กระพริบตา ---
        long elapsed = System.currentTimeMillis() - startTime;
        double t = (elapsed % 100) / 100.0; // รอบละ 0.1 วินาที
        // เริ่มต้นหลับตา (h เล็กมาก) แล้วค่อยๆ ลืมตา (h เพิ่มขึ้น)
        // หลับสนิทค้างไว้ 0.2 วินาทีแรก
        double blink;
        if (elapsed < 200) {
            blink = 0; // หลับสนิท
        } else {
            blink = Math.min(1, Math.max(0, (t - 0.15) * 1.2)); // 0=หลับ, 1=ลืมตา
        }
        double eyeW = headRadius * 0.34; // ปรับให้เล็กลงจากเดิม
        double eyeH = headRadius * (0.08 + 0.24 * blink); // h เล็กตอนหลับ, ใหญ่ตอนลืม
        double eyeY = headY + headRadius * 1.21;    
        drawAnimeEye(g2d, (cx - headRadius * 0.36)+11, eyeY, eyeW, eyeH, 0, 0, 0, new Color(120, 180, 255)); // ตาซ้าย
        drawAnimeEye(g2d, (cx + headRadius * 0.36)+11, eyeY, eyeW, eyeH, 0, 0, 0, new Color(120, 180, 255)); // ตาขวา

        // วาดปากด้วย Midpoint Circle Arc) =====
        g2d.setColor(Color.BLACK);
        int mouthW = (int)(headRadius * 0.38);
        int mouthH = (int)(headRadius * 0.18);
        int mouthX = cx - mouthW / 2;
        int mouthY = (int)(headY + headRadius * 1.45);

        Polygon mouth = new Polygon();
        int rx = mouthW / 2;
        int ry = mouthH / 2;
        int xc = mouthX + rx;
        int yc = mouthY + ry;

        int stepsArc = 100;
        double startAngle = Math.toRadians(0);
        double endAngle = Math.toRadians(160);

        for (int i = 0; i <= stepsArc; i++) {
            double z = i / (double) stepsArc;
            double theta = startAngle + z * (endAngle - startAngle);
            int x = (int) (xc + rx * Math.cos(theta));
            int y = (int) (yc + ry * Math.sin(theta));
            mouth.addPoint(x, y);
        }

        g2d.drawPolygon(mouth);
    }

    public void drawbang(Graphics2D g2d, int cx, int headY, int headRadius) {
        double scale = headRadius / 90.0 * 1.3;
        int dx = cx - (int)(337 * scale);
        int dy = headY - (int)(121 * scale);

        Polygon bang = new Polygon();

        // ===== Bezier Helper =====
        java.util.function.BiConsumer<int[], Polygon> bezier = (pts, poly) -> {
            int steps = 100; // ความละเอียดของเส้นโค้ง
            for (int tStep = 0; tStep <= steps; tStep++) {
                double t = tStep / (double) steps;
                double x = Math.pow(1 - t, 3) * pts[0] +
                        3 * Math.pow(1 - t, 2) * t * pts[2] +
                        3 * (1 - t) * t * t * pts[4] +
                        Math.pow(t, 3) * pts[6];
                double y = Math.pow(1 - t, 3) * pts[1] +
                        3 * Math.pow(1 - t, 2) * t * pts[3] +
                        3 * (1 - t) * t * t * pts[5] +
                        Math.pow(t, 3) * pts[7];
                poly.addPoint((int) x, (int) y);
            }
        };

        // ===== โค้ง 1 =====
        bezier.accept(new int[]{(int)((341+dx)*scale), (int)((144+dy)*scale),
                                (int)((341+dx)*scale), (int)((144+dy)*scale),
                                (int)((298+dx)*scale), (int)((191+dy)*scale),
                                (int)((385+dx)*scale), (int)((191+dy)*scale)}, bang);

        // ===== โค้ง 2 =====
        bezier.accept(new int[]{(int)((385+dx)*scale), (int)((191+dy)*scale),
                                (int)((471+dx)*scale), (int)((191+dy)*scale),
                                (int)((374+dx)*scale), (int)((111+dy)*scale),
                                (int)((341+dx)*scale), (int)((144+dy)*scale)}, bang);

        // ปิดรูป
        bang.addPoint((int)((341+dx)*scale), (int)((144+dy)*scale));

        // วาดหน้าม้า
        g2d.setColor(new Color(238, 157, 236)); // สีชมพู
        g2d.fillPolygon(bang);
    }

    public static void drawAnimeEye(Graphics2D g2, double cx, double cy, double w, double h, double lookX, double lookY, double tiltDeg, Color irisColor) {
        AffineTransform oldTx = g2.getTransform();
        g2.rotate(Math.toRadians(tiltDeg), cx, cy);

        // ===== Helper แบบ in-method (ไม่สร้างเมธอดใหม่) =====
        // เพิ่มจุดโค้ง Quadratic Bezier (สำหรับขอบตาบน)
        java.util.function.BiConsumer<double[], java.awt.Polygon> addQuad = (pts, poly) -> {
            int steps = 120; // ความเนียน
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double omt = 1 - t;
                double x = omt*omt*pts[0] + 2*omt*t*pts[2] + t*t*pts[4];
                double y = omt*omt*pts[1] + 2*omt*t*pts[3] + t*t*pts[5];
                poly.addPoint((int)Math.round(x), (int)Math.round(y));
            }
        };

        // เพิ่มจุดของอาร์ควงรี (ใช้พาราเมตริกเป็นตัวแทน Midpoint, ได้ลำดับจุดรอบรูปพอดี)
        java.util.function.Consumer<double[]> addEllipse = spec -> {
            // spec = {cx, cy, rx, ry, startRad, endRad, steps, fill(1)/draw(0), r,g,b,a, strokeWidthIfDraw}
            double ecx = spec[0], ecy = spec[1], rx = Math.max(0.0, spec[2]), ry = Math.max(0.0, spec[3]);
            double a0 = spec[4], a1 = spec[5]; int steps = (int) spec[6];
            boolean doFill = spec[7] > 0.5;
            int R = (int) spec[8], G = (int) spec[9], B = (int) spec[10], A = (int) spec[11];
            float sw = (float) spec[12];
            Polygon poly = new Polygon();
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double ang = a0 + t * (a1 - a0);
                double x = ecx + rx * Math.cos(ang);
                double y = ecy + ry * Math.sin(ang);
                poly.addPoint((int)Math.round(x), (int)Math.round(y));
            }
            g2.setColor(new Color(R, G, B, A));
            if (doFill) g2.fillPolygon(poly); else {
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolygon(poly);
                g2.setStroke(old);
            }
        };

        // สร้างแถบหนา (stroke) ตามเส้นโค้ง โดยทำเป็น "ริบบิ้น" แล้ว fillPolygon
        java.util.function.BiConsumer<double[], Color> fillThickQuad = (pts, color) -> {
            int steps = 120;
            double thick = Math.max(2.0, h*0.12); // เทียบของเดิม
            java.util.ArrayList<double[]> center = new java.util.ArrayList<>();
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps, omt = 1 - t;
                double x = omt*omt*pts[0] + 2*omt*t*pts[2] + t*t*pts[4];
                double y = omt*omt*pts[1] + 2*omt*t*pts[3] + t*t*pts[5];
                center.add(new double[]{x, y});
            }
            java.util.ArrayList<double[]> left = new java.util.ArrayList<>(), right = new java.util.ArrayList<>();
            for (int i = 0; i < center.size()-1; i++) {
                double[] p = center.get(i), q = center.get(i+1);
                double dx = q[0] - p[0], dy = q[1] - p[1];
                double len = Math.hypot(dx, dy);
                if (len < 1e-6) continue;
                double nx = -dy/len, ny = dx/len; // นอร์มัลตั้งฉาก
                double ox = nx * (thick*0.5), oy = ny * (thick*0.5);
                left.add(new double[]{p[0] + ox, p[1] + oy});
                right.add(new double[]{p[0] - ox, p[1] - oy});
                if (i == center.size()-2) { // จุดท้าย
                    left.add(new double[]{q[0] + ox, q[1] + oy});
                    right.add(new double[]{q[0] - ox, q[1] - oy});
                }
            }
            Polygon ribbon = new Polygon();
            for (double[] L : left)  ribbon.addPoint((int)Math.round(L[0]), (int)Math.round(L[1]));
            for (int i = right.size()-1; i >= 0; i--) {
                double[] R = right.get(i);
                ribbon.addPoint((int)Math.round(R[0]), (int)Math.round(R[1]));
            }
            g2.setColor(color);
            g2.fillPolygon(ribbon);
        };

        // สร้างสี่เหลี่ยมผอมๆ ตามแนวเส้น (ใช้ทำขนตา) แล้ว fillPolygon
        java.util.function.BiConsumer<double[], Color> fillThickLine = (seg, color) -> {
            double x1=seg[0], y1=seg[1], x2=seg[2], y2=seg[3], th=seg[4];
            double dx = x2 - x1, dy = y2 - y1, len = Math.hypot(dx, dy);
            if (len < 1e-6) return;
            double nx = -dy/len, ny = dx/len;
            double ox = nx * (th*0.5), oy = ny * (th*0.5);
            Polygon poly = new Polygon();
            poly.addPoint((int)Math.round(x1 + ox), (int)Math.round(y1 + oy));
            poly.addPoint((int)Math.round(x2 + ox), (int)Math.round(y2 + oy));
            poly.addPoint((int)Math.round(x2 - ox), (int)Math.round(y2 - oy));
            poly.addPoint((int)Math.round(x1 - ox), (int)Math.round(y1 - oy));
            g2.setColor(color);
            g2.fillPolygon(poly);
        };

        // ===== 1) Sclera (ทรงสี่เหลี่ยมมุมโค้ง) ด้วย Polygon =====
        // ตรงตาม RoundRectangle2D.Double(cx - w/2, cy - h/2, w*0.78, h*0.84, w*0.2, h*0.9)
        double W = w*0.78, H = h*0.84;
        double x0 = cx - W/2, y0 = cy - H/2;
        double arcW = Math.min(w*0.2, W);
        double arcH = Math.min(h*0.9, H);
        double rx = arcW/2.0, ry = arcH/2.0;

        // เดินรอบรูปตามเข็ม: บน → ขวา → ล่าง → ซ้าย (มีอาร์ค 4 มุม)
        Polygon sclera = new Polygon();
        // จุดเริ่ม top-left (สัมผัสอาร์ค)
        sclera.addPoint((int)Math.round(x0 + rx), (int)Math.round(y0));
        // top straight ไป top-right
        sclera.addPoint((int)Math.round(x0 + W - rx), (int)Math.round(y0));
        // TR arc 270°→360°
        {
            int steps = 36;
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = Math.toRadians(270) + t * Math.toRadians(90);
                double x = (x0 + W - rx) + rx * Math.cos(ang);
                double y = (y0 + ry)      + ry * Math.sin(ang);
                sclera.addPoint((int)Math.round(x), (int)Math.round(y));
            }
        }
        // right straight
        sclera.addPoint((int)Math.round(x0 + W), (int)Math.round(y0 + H - ry));
        // BR arc 0°→90°
        {
            int steps = 36;
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = Math.toRadians(0) + t * Math.toRadians(90);
                double x = (x0 + W - rx) + rx * Math.cos(ang);
                double y = (y0 + H - ry) + ry * Math.sin(ang);
                sclera.addPoint((int)Math.round(x), (int)Math.round(y));
            }
        }
        // bottom straight
        sclera.addPoint((int)Math.round(x0 + rx), (int)Math.round(y0 + H));
        // BL arc 90°→180°
        {
            int steps = 36;
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = Math.toRadians(90) + t * Math.toRadians(90);
                double x = (x0 + rx) + rx * Math.cos(ang);
                double y = (y0 + H - ry) + ry * Math.sin(ang);
                sclera.addPoint((int)Math.round(x), (int)Math.round(y));
            }
        }
        // left straight
        sclera.addPoint((int)Math.round(x0), (int)Math.round(y0 + ry));
        // TL arc 180°→270°
        {
            int steps = 36;
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = Math.toRadians(180) + t * Math.toRadians(90);
                double x = (x0 + rx) + rx * Math.cos(ang);
                double y = (y0 + ry) + ry * Math.sin(ang);
                sclera.addPoint((int)Math.round(x), (int)Math.round(y));
            }
        }

        g2.setColor(Color.WHITE);
        g2.fillPolygon(sclera);

        // ===== 2) ขอบตาบนหนา (แทน QuadCurve2D ด้วย Bezier + ริบบิ้น) =====
        // QuadCurve2D.Double(cx - w*0.51, cy - h*0.45, cx, cy - h*0.55, cx + w*0.31, cy - h*0.45)
        double[] quad = {
            cx - w*0.51, cy - h*0.45,
            cx,          cy - h*0.55,
            cx + w*0.31, cy - h*0.45
        };
        fillThickQuad.accept(quad, Color.BLACK);

        // ===== 3) เส้นรอบ (เส้นบาง) ด้วย drawPolygon (อนุญาตเฉพาะ Polygon) =====
        Stroke oldStroke = g2.getStroke();
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke((float)Math.max(1, h*0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(sclera);
        g2.setStroke(oldStroke);

        // ===== 4) Iris & Pupil (คงพิกัดเดิมทุกอย่าง รวมออฟเซ็ต -3) =====
        double irisR  = Math.min(w, h) * 0.33;
        double pupilR = irisR * 0.72;
        double maxOffset = (Math.min(w, h) * 0.45) - irisR * 1.05;
        double px = cx + Math.max(-1, Math.min(1, lookX)) * maxOffset;
        double py = cy + Math.max(-1, Math.min(1, lookY)) * (maxOffset * 0.75);

        // iris center = (px - 3, py)
        {
            int steps = 96;
            Polygon irisPoly = new Polygon();
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = t * Math.PI * 2;
                double x = (px - 3) + irisR * Math.cos(ang);
                double y = (py     ) + irisR * Math.sin(ang);
                irisPoly.addPoint((int)Math.round(x), (int)Math.round(y));
            }
            g2.setColor(irisColor);
            g2.fillPolygon(irisPoly);
            g2.setColor(Color.BLACK);
            g2.drawPolygon(irisPoly);
        }

        // pupil center = (px - 3, py)
        {
            int steps = 96;
            Polygon pupilPoly = new Polygon();
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = t * Math.PI * 2;
                double x = (px - 3) + pupilR * Math.cos(ang);
                double y = (py     ) + pupilR * Math.sin(ang);
                pupilPoly.addPoint((int)Math.round(x), (int)Math.round(y));
            }
            g2.setColor(Color.BLACK);
            g2.fillPolygon(pupilPoly);
        }

        // ===== 5) Highlights (ตำแหน่งเทียบโค้ดเดิม เป๊ะ) =====
        // 5.1 Ellipse: (px - pupilR*0.2, py - pupilR*0.9, pupilR*0.8, pupilR*0.9)
        {
            double rxH = pupilR * 0.8 / 2.0;
            double ryH = pupilR * 0.9 / 2.0;
            double cxH = (px - pupilR*0.2) + rxH;
            double cyH = (py - pupilR*0.9) + ryH;
            int steps = 60;
            Polygon hi = new Polygon();
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = t * Math.PI * 2;
                double x = cxH + rxH * Math.cos(ang);
                double y = cyH + ryH * Math.sin(ang);
                hi.addPoint((int)Math.round(x), (int)Math.round(y));
            }
            g2.setColor(Color.WHITE);
            g2.fillPolygon(hi);
        }
        // 5.2 Ellipse: (px + pupilR*0.2, py - pupilR*0.1, pupilR*0.35, pupilR*0.35)
        {
            double rxH = pupilR * 0.35 / 2.0;
            double ryH = rxH;
            double cxH = (px + pupilR*0.2) + rxH;
            double cyH = (py - pupilR*0.1) + ryH;
            int steps = 60;
            Polygon hi = new Polygon();
            for (int i = 0; i <= steps; i++) {
                double t = i/(double)steps;
                double ang = t * Math.PI * 2;
                double x = cxH + rxH * Math.cos(ang);
                double y = cyH + ryH * Math.sin(ang);
                hi.addPoint((int)Math.round(x), (int)Math.round(y));
            }
            g2.setColor(Color.WHITE);
            g2.fillPolygon(hi);
        }

        // ===== 6) ขนตาหางตา (3 เส้น) แทน Line2D ด้วยสี่เหลี่ยมผอมๆ แล้ว fillPolygon =====
        {
            double lashT = Math.max(1.0, h*0.06);
            for (int i = 0; i < 3; i++) {
                double t = i / 2.0;
                double x1 = cx + w*0.3;
                double y1 = cy - h*0.5 + i*h*0.08;
                double x2 = x1 + w*0.18;
                double y2 = y1 - h*0.10 + t*h*0.06;
                fillThickLine.accept(new double[]{x1, y1, x2, y2, lashT}, Color.BLACK);
            }
        }

        // ---- Restore Transform ----
        g2.setTransform(oldTx);
    }

    private void drawBody(Graphics2D g2, int cx, int cy, int bodyW, int bodyH, int skinColor) {
        // คอ
        int neckW = (int)(bodyW * 0.12);
        int neckH = (int)(bodyH * 0.46);
        int neckX = (cx - neckW / 2) + 5;
        int neckY = cy - 10;

        Polygon neckPoly = new Polygon();
        neckPoly.addPoint(neckX, neckY);
        neckPoly.addPoint(neckX + neckW, neckY);
        neckPoly.addPoint(neckX + neckW, neckY + neckH);
        neckPoly.addPoint(neckX, neckY + neckH);
        g2.setColor(new Color(skinColor));
        g2.fillPolygon(neckPoly);

        // สัดส่วนไหล่
        int shoulderW = (int)(bodyW * 0.37);
        int shoulderH = (int)(bodyH * 0.42);
        int shoulderX = (cx - shoulderW / 2) + 5;
        int shoulderY = neckY + neckH - 5;

        Polygon shoulder = new Polygon();
        shoulder.addPoint(shoulderX, shoulderY);
        shoulder.addPoint(shoulderX + shoulderW, shoulderY);
        shoulder.addPoint(shoulderX + shoulderW, shoulderY + shoulderH);
        shoulder.addPoint(shoulderX, shoulderY + shoulderH);
        g2.setColor(new Color(240, 240, 240));
        g2.fillPolygon(shoulder);

        // สัดส่วนตัว
        int torsoTopW = (int)(bodyW * 0.37);
        int torsoBotW = (int)(bodyW * 0.22);
        int torsoH = (int)(bodyH * 1.1);
        int torsoY = shoulderY + shoulderH - 10;

        Polygon torso = new Polygon();
        torso.addPoint((cx - torsoTopW/2) + 5, torsoY);
        torso.addPoint((cx + torsoTopW/2) + 5, torsoY);
        torso.addPoint((cx + torsoBotW/2) + 5, torsoY + torsoH);
        torso.addPoint((cx - torsoBotW/2) + 5, torsoY + torsoH);
        g2.setColor(new Color(240, 240, 240));
        g2.fillPolygon(torso);

        // สะโพก
        int waistW = (int)(bodyW * 0.22);
        int hipW   = (int)(bodyW * 0.37);
        int lowerH = (int)(bodyH * 0.7);

        Polygon lowerTorso = new Polygon();
        lowerTorso.addPoint((cx - waistW/2) + 5, (int)(torsoY * 1.26));
        lowerTorso.addPoint((cx + waistW/2) + 5, (int)(torsoY * 1.26));
        lowerTorso.addPoint((cx + hipW/2) + 5, (int)(torsoY * 1.5) + lowerH);
        lowerTorso.addPoint((cx - hipW/2) + 5, (int)(torsoY * 1.5) + lowerH);
        g2.setColor(Color.DARK_GRAY);
        g2.fillPolygon(lowerTorso);
        

        // แขน
        int armW = (int)(bodyW * 0.074);
        int armH = (int)(bodyH * 1.56);
        int armY = cy + (int)(bodyH * 0.3);
        int armXLeft = cx - (int)(bodyW * 0.24)/2 - armW + 5;
        int armXRight = cx + (int)(bodyW * 0.24)/2 + 5;

        // มือ
        int handR = (int)(armW * 0.45);
        // มือซ้าย
        drawHand(g2, armXLeft + armW/2, armY + armH, handR*3, true, new Color(skinColor));
        // มือขวา
        drawHand(g2, armXRight + armW/2, armY + armH, handR*3, false, new Color(skinColor));

        //แขนซ้าย
        Polygon leftArm = new Polygon();
        leftArm.addPoint(armXLeft, armY);
        leftArm.addPoint(armXLeft + armW, armY);
        leftArm.addPoint(armXLeft + armW, armY + armH);
        leftArm.addPoint(armXLeft, armY + armH);
        g2.setColor(new Color(240,240,240));
        g2.fillPolygon(leftArm);
        //แขนขวา
        Polygon rightArm = new Polygon();
        rightArm.addPoint(armXRight, armY);
        rightArm.addPoint(armXRight + armW, armY);
        rightArm.addPoint(armXRight + armW, armY + armH);
        rightArm.addPoint(armXRight, armY + armH);
        g2.fillPolygon(rightArm);

        
        // ปกเสื้อซ้าย
        Polygon leftCollar = new Polygon();
        leftCollar.addPoint((cx - neckW/2) + 5, neckY + neckH - 10);
        leftCollar.addPoint(cx + 5, neckY + neckH + 18);
        leftCollar.addPoint((cx - neckW) + 5, neckY + neckH + 18);
        g2.setColor(Color.WHITE);
        g2.fillPolygon(leftCollar);

        // ปกเสื้อขวา
        Polygon rightCollar = new Polygon();
        rightCollar.addPoint((cx + neckW/2) + 5, neckY + neckH - 10);
        rightCollar.addPoint(cx + 5, neckY + neckH + 18);
        rightCollar.addPoint((cx + neckW) + 5, neckY + neckH + 18);
        g2.setColor(Color.WHITE);
        g2.fillPolygon(rightCollar);

        //คอตรงปกเสื้อ
        Polygon neck = new Polygon();
        neck.addPoint((cx - neckW/2) + 5, neckY + neckH - 10);
        neck.addPoint((cx + neckW/2) + 5, neckY + neckH - 10);
        neck.addPoint(cx + 5, neckY + neckH + 18);
        g2.setColor(new Color(skinColor));
        g2.fillPolygon(neck);

        // รอยกระดุม → วาดเป็นวงกลมเล็กๆ (Polygon)
        g2.setColor(Color.GRAY);
        for (int i = 0; i < 5; i++) {
            drawCirclePolygon(g2, cx, neckY + neckH + 30 + i*18, 4, Color.GRAY);
        }
    }
    private void drawHand(Graphics2D g2, int cx, int cy, int size, boolean isLeft, Color skinColor) {
        // cx, cy = จุดกลางข้อมือ, size = ขนาดมือ, isLeft = true=ซ้าย, false=ขวา
        double dir = isLeft ? -1 : 1;
        int palmW = (int)(size * 0.9);
        int palmH = (int)(size * 0.7);

        // สร้างจุด Bezier สำหรับฝ่ามือและนิ้วโป้ง
        int[][] bezierPoints = {
            {cx, cy}, // จุดเริ่มต้นข้อมือ
            {cx + (int)(dir * palmW * 0.3), cy - (int)(palmH * 0.2)},
            {cx + (int)(dir * palmW * 0.7), cy + (int)(palmH * 0.4)},
            {cx + (int)(dir * palmW * 0.5), cy + (int)(palmH * 0.8)},
            {cx + (int)(dir * palmW * 0.2), cy + (int)(palmH * 1.0)},
            {cx + (int)(dir * palmW * 0.0), cy + (int)(palmH * 0.7)},
            {cx, cy + (int)(palmH * 0.7)},
            {cx - (int)(dir * palmW * 0.2), cy + (int)(palmH * 0.7)},
            {cx - (int)(dir * palmW * 0.3), cy + (int)(palmH * 0.2)},
            {cx, cy}
        };

        Polygon handPoly = new Polygon();

        // ใช้ Bezier สร้างจุดรอบมือ
        for (int i = 0; i < bezierPoints.length - 3; i += 3) {
            int[] p0 = bezierPoints[i];
            int[] p1 = bezierPoints[i+1];
            int[] p2 = bezierPoints[i+2];
            int[] p3 = bezierPoints[i+3];
            int steps = 30;
            for (int tStep = 0; tStep <= steps; tStep++) {
                double t = tStep / (double) steps;
                double omt = 1 - t;
                double x = omt*omt*omt*p0[0] + 3*omt*omt*t*p1[0] + 3*omt*t*t*p2[0] + t*t*t*p3[0];
                double y = omt*omt*omt*p0[1] + 3*omt*omt*t*p1[1] + 3*omt*t*t*p2[1] + t*t*t*p3[1];
                handPoly.addPoint((int)x, (int)y);
            }
        }

        g2.setColor(skinColor);
        g2.fillPolygon(handPoly);
        g2.setColor(Color.BLACK);
        g2.drawPolygon(handPoly);
    }

    private void drawCirclePolygon(Graphics2D g2, int cx, int cy, int r, Color c) {
        Polygon p = new Polygon();
        int steps = 20; // ยิ่งมากยิ่งกลม
        for (int i=0; i<steps; i++) {
            double theta = 2 * Math.PI * i / steps;
            int x = (int)(cx + r * Math.cos(theta));
            int y = (int)(cy + r * Math.sin(theta));
            p.addPoint(x, y);
        }
        g2.setColor(c);
        g2.fillPolygon(p);
    }


    private void drawSceneTransition(Graphics2D g2, int panelWidth, int panelHeight, long elapsed, long duration, Runnable drawNewScene) {
        // elapsed: เวลาที่ผ่านไป (ms)
        // duration: เวลารวมของ transition (ms)
        // drawNewScene: โค้ดวาดฉากใหม่ (Runnable)

        float progress = Math.min(1f, elapsed / (float)duration);

        // เตรียม polygon แทนสี่เหลี่ยมเต็มจอ
        int[] px = {0, panelWidth, panelWidth, 0};
        int[] py = {0, 0, panelHeight, panelHeight};
        Polygon fullScreenRect = new Polygon(px, py, 4);

        if (progress < 0.5f) {

            // overlay สีดำทึบขึ้น
            float alpha = progress * 1; // 0 → 1
            g2.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
            g2.fillPolygon(fullScreenRect);

        } else {
            // วาดฉากใหม่
            drawNewScene.run();

            // overlay สีดำจางลง
            float alpha = 1f - ((progress - 0.5f) * 1); // 1 → 0
            g2.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
            g2.fillPolygon(fullScreenRect);
        }
    }
    
    private Runnable drawPhoneRing(Graphics2D g2) {
        return () -> {

            smooth(g2);

            int panelW = getWidth();
            int panelH = getHeight();

            long elapsed = System.currentTimeMillis() - startTime;
            double shake = Math.sin(elapsed * 0.015) * 3; // สั่นซ้ายขวา

            int phoneW = 220, phoneH = 360;
            int phoneX = panelW / 2 - phoneW / 2 + (int)shake;
            int phoneY = panelH / 2 - phoneH / 2;

            //โทรศัพท์
            drawRoundedRectPolygon(g2, phoneX, phoneY, phoneW, phoneH, 30, 30, new Color(60, 60, 60));
            drawRoundedRectPolygon(g2, phoneX + 12, phoneY + 28, phoneW - 24, phoneH - 80, 18, 18, new Color(220, 255, 220));

            //ปุ่ม Home
            drawBezierPolygonCircle(g2, phoneX + phoneW / 2, phoneY + phoneH - 28, 14, new Color(180, 180, 180));

            // ปุ่มรับสาย / วางสาย
            int btnRadius = 25;
            int btnCenterX = phoneX + phoneW / 4 - 20;
            int btnCenterY = phoneY + phoneH / 2 + 90;

            // ปุ่มวางสาย (สีแดง)
            //drawBezierPolygonCircle(g2, btnCenterX + btnRadius + 5, btnCenterY, btnRadius, new Color(220, 40, 40));
            drawCirclePolygon(g2, btnCenterX + 5*btnRadius, btnCenterY , btnRadius, Color.red);
            // ปุ่มรับสาย (สีเขียว)
            drawCirclePolygon(g2, btnCenterX + btnRadius, btnCenterY, btnRadius,Color.green);
            //drawBezierPolygonCircle(g2, btnCenterX - btnRadius - 5, btnCenterY, btnRadius, new Color(80, 180, 80));

            //ไอคอนโทรศัพท์
            drawPhoneIcon(g2, btnCenterX + btnRadius, btnCenterY+6, 15, 15, Color.WHITE);
            drawPhoneIcon(g2, btnCenterX + 5*btnRadius, btnCenterY+6, 15, 15, Color.WHITE);

            // สายเรียกเข้า
            g2.setColor(new Color(120, 0, 0));
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.drawString("Midterm is", phoneX + 55, phoneY + phoneH / 2 - 60);
            g2.drawString("calling you...", phoneX + 50, phoneY + phoneH / 2 - 30);
        };
    }

    // --- Helper methods ---
    private void drawPhoneIcon(Graphics2D g2, int cx, int cy, int w, int h, Color c) {
        // วาดส่วนโค้งโทรศัพท์ (arc ยาวขึ้น)
        int arcStart = 210, arcAngle = 120;
        int arcSteps = 24;
        Polygon arcPoly = new Polygon();
        for (int i = 0; i <= arcSteps; i++) {
            double angle = Math.toRadians(arcStart + i * (arcAngle / (double)arcSteps));
            int x = cx + (int)(w * Math.cos(angle));
            int y = cy + (int)(h * Math.sin(angle));
            arcPoly.addPoint(x, y);
        }
        g2.setColor(c);
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(arcPoly);

        // วาดปลายโทรศัพท์ (วงกลมมนๆ)
        int tipR = 8;
        int tipLx = cx + (int)(w * Math.cos(Math.toRadians(arcStart)))+1;
        int tipLy = cy + (int)(h * Math.sin(Math.toRadians(arcStart))) + 2 ;
        int tipRx = cx + (int)(w * Math.cos(Math.toRadians(arcStart + arcAngle)))-1;
        int tipRy = cy + (int)(h * Math.sin(Math.toRadians(arcStart + arcAngle)))+2;
        drawCirclePolygon(g2, tipLx, tipLy, tipR, c);
        drawCirclePolygon(g2, tipRx, tipRy, tipR, c);
    }

    private void drawBezierPolygonCircle(Graphics2D g2, int cx, int cy, int r, Color c) {
        double cR = r * 0.552284749831; // magic number
        Polygon p = new Polygon();

        // ขวาล่าง
        p.addPoint(cx + r, cy);
        p.addPoint(cx + r, cy + (int)cR);
        p.addPoint(cx + (int)cR, cy + r);
        p.addPoint(cx, cy + r);

        // ซ้ายล่าง
        p.addPoint(cx - (int)cR, cy + r);
        p.addPoint(cx - r, cy + (int)cR);
        p.addPoint(cx - r, cy);

        // ซ้ายบน
        p.addPoint(cx - r, cy - (int)cR);
        p.addPoint(cx - (int)cR, cy - r);
        p.addPoint(cx, cy - r);

        // ขวาบน
        p.addPoint(cx + (int)cR, cy - r);
        p.addPoint(cx + r, cy - (int)cR);

        g2.setColor(c);
        g2.fillPolygon(p);
    }


    private void drawRoundedRectPolygon(Graphics2D g2, int x, int y, int w, int h, int arcW, int arcH, Color c) {
        Polygon p = new Polygon();
        int steps = 10; // จำนวน points ต่อมุมโค้ง
        // มุมซ้ายบน
        for (int i = 0; i <= steps; i++) {
            double theta = Math.PI + (Math.PI/2)*i/steps;
            int px = x + arcW + (int)(arcW * Math.cos(theta));
            int py = y + arcH + (int)(arcH * Math.sin(theta));
            p.addPoint(px, py);
        }
        // มุมขวาบน
        for (int i = 0; i <= steps; i++) {
            double theta = -Math.PI/2 + (Math.PI/2)*i/steps;
            int px = x + w - arcW + (int)(arcW * Math.cos(theta));
            int py = y + arcH + (int)(arcH * Math.sin(theta));
            p.addPoint(px, py);
        }
        // มุมขวาล่าง
        for (int i = 0; i <= steps; i++) {
            double theta = 0 + (Math.PI/2)*i/steps;
            int px = x + w - arcW + (int)(arcW * Math.cos(theta));
            int py = y + h - arcH + (int)(arcH * Math.sin(theta));
            p.addPoint(px, py);
        }
        // มุมซ้ายล่าง
        for (int i = 0; i <= steps; i++) {
            double theta = Math.PI/2 + (Math.PI/2)*i/steps;
            int px = x + arcW + (int)(arcW * Math.cos(theta));
            int py = y + h - arcH + (int)(arcH * Math.sin(theta));
            p.addPoint(px, py);
        }
        g2.setColor(c);
        g2.fillPolygon(p);
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
