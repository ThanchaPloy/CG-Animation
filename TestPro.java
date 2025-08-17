import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;



public class TestPro extends JPanel implements ActionListener {
    private Timer timer;
    private long startTime;
    private int sceneState = 0; // 0 = โลงศพ, 1 = กำลังเปลี่ยนฉาก, 2 = โทรศัพท์, 3 = โต๊ะ
    private long transitionStart = 0;

    private boolean showBone = false;
    private boolean showbrown = true;
    
    // ตัวแปร offset สำหรับเลื่อนโต๊ะและของบนโต๊ะ
    private int deskOffsetX = 0;
    private Timer deskTimer;
    private int chairOffsetX = 0;
    private Timer chairTimer;

    // สำหรับ animation ลุกขึ้นยืน
    private int pantsFrame = 0; // 0 = นั่ง, 1-3 = กำลังลุก, 4 = ยืนเต็ม
    private Timer standUpTimer;

    private boolean useSideShirt = false; 
    private int bodyoffsetY = 0;
    private int bodyoffsetX = 0;

    private int headOffsetX = 0;
    private int headOffsetY = 0;

    // ตำแหน่ง offset สำหรับการเลื่อน
    private int baseCoffinOffsetX = -400; // เริ่มอยู่นอกจอซ้าย
    private int lidCoffinOffsetX = 800;   // เริ่มอยู่นอกจอขวา

    // state ของแอนิเมชัน
    private boolean showBase = false;
    private boolean showLid = false;
    
    private int backgroundStep = -1; // -1 = สีปกติ, 0-3 = สี transition
    private final Color[] bgColors = {
    new Color(255, 240, 196),
    new Color(140, 16, 7),
    new Color(102, 11, 5),
    new Color(62, 7, 3),
    //new Color(0,0,0)
    };
    private BufferedImage canvas; // BufferedImage สำหรับวาดทุกอย่าง
    

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
                transitionStart = elapsed;
            }
        } else if (sceneState == 2) {
            // ฉากโทรศัพท์
            long transElapsed = elapsed - transitionStart;
            drawPhoneRing(g2).run();
            if (transElapsed > 800) {
                sceneState = 3; // เปลี่ยนไปฉากโต๊ะ
                transitionStart = elapsed;
                changeBG();
            }
        } else if (sceneState == 3) {

            long transElapsed = elapsed - transitionStart;
            // ทำ fade in เข้าฉากโต๊ะ (เช่น 400ms)
            if (transElapsed < 400) {
                drawSceneTransition(g2, getWidth(), getHeight(), transElapsed, 400, () -> {
                Graphics2D gCanvas = (Graphics2D) g2.create();
                // วาดฉากโต๊ะ
                gCanvas.setColor(new Color(220, 220, 220));
                gCanvas.fillRect(0, 0, getWidth(), getHeight());
                
                // 1. วาดฐานโลง (ด้านหลัง)
                if (showBase) {
                    gCanvas.translate(baseCoffinOffsetX, 0);
                    drawBaseCoffin(gCanvas);
                    gCanvas.translate(-baseCoffinOffsetX, 0);
                }

                // วาดเก้าอี้พร้อมกับ offset
                gCanvas.translate(chairOffsetX, 0);
                drawShair(gCanvas);
                gCanvas.translate(-chairOffsetX, 0);

                // วาดกางเกงตามเฟรม
                switch (pantsFrame) {
                    case 0 -> drawPants(gCanvas);         // นั่ง
                    case 1 -> drawPantsStanding1(gCanvas);
                    case 2 -> drawPantsStanding2(gCanvas);
                    case 3 -> drawPantsStanding3(gCanvas);
                    case 4 -> drawPantsStanding3(gCanvas);
                    case 5 -> drawPantsStanding4(gCanvas);
                    case 6 -> drawPantsStand5(gCanvas);
                    // ยืนเต็ม
                }
                
            gCanvas.translate(bodyoffsetX, bodyoffsetY);
                gCanvas.translate(headOffsetX, headOffsetY);
                if (showBone) {
                    drawBone(gCanvas);
                } else {
                    drawhair(gCanvas);
                    drawFace(gCanvas);
                    drawbang(gCanvas);
                }
                gCanvas.translate(-headOffsetX, -headOffsetY);
                if (useSideShirt) {
                    drawSidehand(gCanvas);
                    drawSideShirt(gCanvas);
                } else {
                    drawShirt(gCanvas);
                    drawHand(gCanvas);
                }
                gCanvas.translate(-bodyoffsetX, -bodyoffsetY);

                if (showbrown) {
                    drawPieceDesk(gCanvas);
                }

                // เลื่อนเฉพาะโต๊ะและของบนโต๊ะ
                gCanvas.translate(deskOffsetX, 0);
                drawDesk(gCanvas);
                drawLaptop(gCanvas);
                gCanvas.translate(-deskOffsetX, 0);
                // เพิ่มวัตถุอื่นๆ บนโต๊ะที่ต้องการเลื่อนในนี้
                
                
            // 3. วาดฝาโลง (อยู่หน้ากางเกง)
                if (showLid) {
                    gCanvas.translate(lidCoffinOffsetX, 0);
                    drawLidCoffin(gCanvas);
                    gCanvas.translate(-lidCoffinOffsetX, 0);
                }

                g2.drawImage(canvas, 0, 0, this);
                gCanvas.dispose();
            });
            } else {
                // วาดฉากโต๊ะปกติ
                Graphics2D gCanvas = (Graphics2D) g2.create();
                gCanvas.setColor(new Color(220, 220, 220));
                gCanvas.fillRect(0, 0, getWidth(), getHeight());

                // 1. วาดฐานโลง (ด้านหลัง)
                if (showBase) {
                    gCanvas.translate(baseCoffinOffsetX, 0);
                    drawBaseCoffin(gCanvas);
                    gCanvas.translate(-baseCoffinOffsetX, 0);
                }

                // วาดเก้าอี้พร้อมกับ offset
                gCanvas.translate(chairOffsetX, 0);
                drawShair(gCanvas);
                gCanvas.translate(-chairOffsetX, 0);

                // วาดกางเกงตามเฟรม
                switch (pantsFrame) {
                    case 0 -> drawPants(gCanvas);         // นั่ง
                    case 1 -> drawPantsStanding1(gCanvas);
                    case 2 -> drawPantsStanding2(gCanvas);
                    case 3 -> drawPantsStanding3(gCanvas);
                    case 4 -> drawPantsStanding3(gCanvas);
                    case 5 -> drawPantsStanding4(gCanvas);
                    case 6 -> drawPantsStand5(gCanvas);
                    // ยืนเต็ม
                }
                
            gCanvas.translate(bodyoffsetX, bodyoffsetY);
                gCanvas.translate(headOffsetX, headOffsetY);
                if (showBone) {
                    drawBone(gCanvas);
                } else {
                    drawhair(gCanvas);
                    drawFace(gCanvas);
                    drawbang(gCanvas);
                }
                gCanvas.translate(-headOffsetX, -headOffsetY);
                if (useSideShirt) {
                    drawSidehand(gCanvas);
                    drawSideShirt(gCanvas);
                } else {
                    drawShirt(gCanvas);
                    drawHand(gCanvas);
                }
                gCanvas.translate(-bodyoffsetX, -bodyoffsetY);

                if (showbrown) {
                    drawPieceDesk(gCanvas);
                }

                // เลื่อนเฉพาะโต๊ะและของบนโต๊ะ
                gCanvas.translate(deskOffsetX, 0);
                drawDesk(gCanvas);
                drawLaptop(gCanvas);
                gCanvas.translate(-deskOffsetX, 0);
                // เพิ่มวัตถุอื่นๆ บนโต๊ะที่ต้องการเลื่อนในนี้
                
                
            // 3. วาดฝาโลง (อยู่หน้ากางเกง)
                if (showLid) {
                    gCanvas.translate(lidCoffinOffsetX, 0);
                    drawLidCoffin(gCanvas);
                    gCanvas.translate(-lidCoffinOffsetX, 0);
                }

                g2.drawImage(canvas, 0, 0, this);
                gCanvas.dispose();
            }
        }

    }

    private void changeBG() {
            // Timer แสดงสีเปลี่ยน 0.25 วิ ต่อสี
        Timer bgChangeTimer = new Timer(250, e -> {
            backgroundStep++;
            repaint();
            if (backgroundStep >= bgColors.length) {
                backgroundStep = bgColors.length - 1; // หยุดที่สีสุดท้าย
                ((Timer)e.getSource()).stop();
            }
            
        });
        bgChangeTimer.start();
            // Timer เปลี่ยนหน้าเป็นหัวกะโหลกหลัง 0.5 วินาที
            Timer boneTimer = new Timer(500, e -> {
            showBone = true;
            repaint();

            // หลังจากเปลี่ยนหน้าเป็นหัวกะโหลกแล้ว 0.25 วินาที → เริ่มเลื่อนโต๊ะ
            Timer startDeskMove = new Timer(250, ev -> {
                deskTimer = new Timer(16, ev2 -> {
                    deskOffsetX -= 5; // เลื่อนไปทางขวา ถ้าอยากไปซ้ายเปลี่ยนเป็น -= 5
                    if (deskOffsetX < -600) { // เมื่อโต๊ะเลื่อนไปจนถึงตำแหน่งที่กำหนด
                        deskTimer.stop(); // หยุดการเลื่อนของโต๊ะ
                    }
                    repaint();
                });
                deskTimer.start();
                // ซ่อนชิ้นส่วนโต๊ะสีน้ำตาล
                Timer hidepeicedeskTimer = new Timer(20, ev3 -> {
                    showbrown = false;
                    repaint();
                });
                hidepeicedeskTimer.setRepeats(false);
                hidepeicedeskTimer.start();

                //เริ่มยืนขึ้นหลังโต๊ะเลื่อน 0.05 วินาที
                standUpTimer = new Timer(50, ev4 -> {
                    pantsFrame++;
                    bodyoffsetY = -pantsFrame * 6; // สูงขึ้น
                    bodyoffsetX = -pantsFrame * 2; // ขยับซ้าย
                    headOffsetY = -pantsFrame * 7; // ให้สูงขึ้นเท่ากับเสื้อ
                    headOffsetX = -pantsFrame * 5; // ให้ขยับซ้ายเท่ากับเสื้อ
                    if (pantsFrame > 6) {
                        standUpTimer.stop();
                        pantsFrame = 6;
                    }
                        repaint();
                });
                standUpTimer.start();

                // 150ms หลังจากเริ่มยืน → เปลี่ยนเสื้อเป็น SideShirt
                Timer changeShirtTimer = new Timer(150, ev5 -> {
                    useSideShirt = true;
                    repaint();
                    });
                    changeShirtTimer.setRepeats(false);
                    changeShirtTimer.start();
                });
                startDeskMove.setRepeats(false);
                startDeskMove.start();

            });
            boneTimer.setRepeats(false);
            boneTimer.start();


            // Timer ใหม่สำหรับเลื่อนเก้าอี้
            Timer startChairMove = new Timer(500, e -> {
                chairTimer = new Timer(8, ev -> {
                    chairOffsetX += 30; // เลื่อนเก้าอี้ไปทางขวา
                    repaint();
                });
                chairTimer.start();
            });
            startChairMove.setRepeats(false);
            startChairMove.start();

            // หลัง 2000ms → เริ่มเลื่อนฐานโลง
            Timer startBaseTimer = new Timer(2000, e -> {
                showBase = true;
                Timer moveBase = new Timer(8, ev -> {
                    baseCoffinOffsetX += 20;
                    if (baseCoffinOffsetX >= 0) { // มาถึงตำแหน่ง
                        baseCoffinOffsetX = 0;
                        ((Timer) ev.getSource()).stop();

                        // หลังฐานโลงหยุด → เริ่มเลื่อนฝาโลง
                        showLid = true;
                        Timer moveLid = new Timer(8, ev2 -> {
                            lidCoffinOffsetX -= 20;
                            if (lidCoffinOffsetX <= 0) {
                                lidCoffinOffsetX = 0;
                                ((Timer) ev2.getSource()).stop();
                            }
                            repaint();
                        });
                        moveLid.start();
                    }
                    repaint();
                });
                moveBase.start();
            });
            startBaseTimer.setRepeats(false);
            startBaseTimer.start();
        
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
    
    private void drawSideShirt(Graphics2D g2d){
        int dx=175;
        int dy=130;
        
        // สร้าง BufferedImage แยก ----------
    int width = 600, height = 600; // กำหนดพื้นที่พอสำหรับเสื้อ
    BufferedImage shirtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D gShirt = shirtImage.createGraphics();

    // เปิด Anti-alias
    gShirt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // ---------- 2 วาดเส้นขอบเสื้อ (สีดำ) ----------
    gShirt.setColor(Color.BLACK);
        // เส้นรอบนอก (ปิดรูปร่างกลับมาจุดเริ่มต้น)
        bezierCurve(gShirt, 83+dx,180+dy, 83+dx,180+dy, 95+dx,192+dy, 122+dx,181+dy);
        bezierCurve(gShirt, 122+dx,181+dy, 122+dx,181+dy, 148+dx,164+dy, 174+dx,236+dy);
        bezierCurve(gShirt, 174+dx,236+dy, 199+dx,305+dy, 209+dx,328+dy, 204+dx,338+dy);
        bezierCurve(gShirt, 204+dx,338+dy, 199+dx,347+dy, 168+dx,352+dy, 160+dx,344+dy);
        bezierCurve(gShirt, 160+dx,344+dy, 160+dx,344+dy, 151+dx,332+dy, 151+dx,323+dy);
        bezierCurve(gShirt, 151+dx,323+dy, 151+dx,323+dy, 90+dx,340+dy, 53+dx,328+dy);
        bezierCurve(gShirt, 53+dx,328+dy, 53+dx,328+dy, 50+dx,343+dy, 42+dx,345+dy);
        bezierCurve(gShirt, 42+dx,345+dy, 34+dx,345+dy, 25+dx,351+dy, 7+dx,343+dy);
        bezierCurve(gShirt, 7+dx,343+dy, -10+dx,338+dy, 25+dx,251+dy, 31+dx,233+dy);
        bezierCurve(gShirt, 31+dx,233+dy, 37+dx,215+dy, 57+dx,177+dy, 83+dx,180+dy);

         gShirt.dispose();

       // ---------- 3ใช้ FloodFill เติมสี ----------
    int seedX =90 + dx;  // เลือกจุดที่มั่นใจว่าอยู่ในเสื้อ
    int seedY = 190+ dy;  
    floodFill(shirtImage, seedX, seedY, new Color(0,0,0,0), new Color(131,192,231));

    // ---------- 4 วาดเสื้อที่เสร็จแล้วกลับมาที่ g2d ----------
    g2d.drawImage(shirtImage, 0, 0, null);
        
        bezierCurve(g2d, 136+dx,227+dy,142+dx, 305+dy,153+dx,325+dy, 153+dx, 325+dy);
        bezierCurve(g2d,63+dx,231+dy,59+dx,304+dy,53+dx,329+dy,53+dx,329+dy);

    }
    private void drawSidehand(Graphics2D g2d){
        int dx=177;
        int dy=122;
        int x =173;
        int y = 118;

        int width = 600, height = 600; // กำหนดพื้นที่พอสำหรับเสื้อ
        BufferedImage SideHandImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gSideHand = SideHandImage.createGraphics();

        // เปิด Anti-alias
        gSideHand.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gSideHand.setColor(Color.BLACK);
        bezierCurve(gSideHand, 196+dx,350+dy, 206+dx,363+dy, 205+dx,375+dy, 205+dx,375+dy);
        bezierCurve(gSideHand, 205+dx,375+dy, 203+dx,387+dy, 199+dx,392+dy, 193+dx,385+dy);
        bezierCurve(gSideHand, 193+dx,385+dy, 186+dx,377+dy, 191+dx,372+dy, 181+dx,364+dy);
        bezierCurve(gSideHand, 181+dx,364+dy, 181+dx,364+dy, 173+dx,380+dy, 171+dx,355+dy);
        bresenhamLine(gSideHand,171+dx,355+dy, 196+dx,350+dy,1);
            
        // --- วาดมือด้านซ้าย ---
        bezierCurve(gSideHand, 16+x,352+y, 16+x,352+y, 3+x,398+y, 15+x,395+y);
        bezierCurve(gSideHand, 15+x,395+y, 28+x,392+y, 21+x,377+y, 32+x,372+y);
        bezierCurve(gSideHand, 32+x,372+y, 32+x,372+y, 31+x,397+y, 44+x,358+y);
        bresenhamLine(gSideHand, 44+x,358+y, 16+x,352+y,1);
        gSideHand.dispose();
        
        floodFill(SideHandImage, 192+dx, 365+dy, new Color(0,0,0,0), Color.WHITE);
        floodFill(SideHandImage, 25+x, 370+y, new Color(0,0,0,0), Color.WHITE);
        
        g2d.drawImage(SideHandImage, 0, 0, null);

    
    }
    private void drawFace(Graphics2D g2d) {
        int dx = -40; // ลดแกน X ลง 50
        int dy = 100;  // เพิ่มแกน Y ขึ้น 70
         //วาดคอ
         Polygon neck = new Polygon();
        neck.addPoint(348 + dx, 235 + dy);      
        neck.addPoint(348 + 35 + dx, 235 + dy);     
        neck.addPoint(348 + 35 + dx, 235 + 25 + dy); 
        neck.addPoint(348 + dx, 235 + 25 + dy);   
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(neck); // วาดรูปสี่เหลี่ยมแบบทึบ
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(neck); // วาดเส้นขอบของรูปสี่เหลี่ยม
        
        //วาดหน้า
        int width = 600, height = 600; // กำหนดพื้นที่พอสำหรับเสื้อ
        BufferedImage faceImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gface = faceImage.createGraphics();
        gface.setColor(Color.BLACK);
        bezierCurve(gface, 314+dx,174+dy,261+dx,257+dy,367+dx,239+dy,367+dx,239+dy);
        bezierCurve(gface, 367+dx,239+dy,473+dx,220+dy,355+dx,86+dy,314+dx,174+dy);
        gface.dispose();
        floodFill(faceImage, 306, 326, new Color(0,0,0,0), Color.WHITE);
        g2d.drawImage(faceImage, 0, 0, null);
  
       
       //วาดตา
        g2d.setColor(Color.BLACK); 
        bresenhamLine(g2d,325 + dx, 190+dy , 325 + dx, 197+dy,5);
        bresenhamLine(g2d,350 + dx, 190+dy, 350 + dx, 197+dy,5);
       
    //วาดปาก
       bezierCurve(g2d, 292, 340-20, 303, 349-20, 315, 339-20,315, 339-20);
        
    //วาดหู
       
    }

     private void drawHand(Graphics2D g2d) {
        int dx = 18; // ลดแกน X ลง 50
        int dy = 169;  // เพิ่มแกน Y ขึ้น 70
        int x =14;
        // สร้าง BufferedImage สำหรับวาดมือ
        int width = 600, height = 600;
        BufferedImage handImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gHand = handImage.createGraphics();
        gHand.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // สร้าง Polygon สำหรับมือขวา
        Polygon rightHandPolygon = new Polygon();
        addBezierPointsToPolygon(rightHandPolygon, 289 + dx, 333 + dy, 289 + dx, 330 + dy, 294 + dx, 306 + dy, 269 + dx, 304 + dy);
        addBezierPointsToPolygon(rightHandPolygon, 269 + dx, 304 + dy, 242 + dx, 300 + dy, 224 + dx, 328 + dy, 231 + dx, 327 + dy);
        addBezierPointsToPolygon(rightHandPolygon, 231 + dx, 327 + dy, 231 + dx, 327 + dy, 225 + dx, 351 + dy, 247 + dx, 337 + dy);
        addBezierPointsToPolygon(rightHandPolygon, 247 + dx, 337 + dy, 247 + dx, 337 + dy, 277 + dx, 330 + dy, 289 + dx, 333 + dy);

        // สร้าง Polygon สำหรับมือซ้าย
        Polygon leftHandPolygon = new Polygon();
        addBezierPointsToPolygon(leftHandPolygon, 250 + x, 306 + dy, 250 + x, 306 + dy, 244 + x, 285 + dy, 219 + x, 301 + dy);
        addBezierPointsToPolygon(leftHandPolygon, 219 + x, 301 + dy, 195 + x, 316 + dy, 199 + x, 354 + dy, 231 + x, 339 + dy);
        addBezierPointsToPolygon(leftHandPolygon, 231 + x, 339 + dy, 231 + x, 339 + dy, 229 + x, 324 + dy, 250 + x, 306 + dy);
    // วาดและเติมสี Polygon มือขวา
        gHand.setColor(Color.WHITE);
        gHand.fillPolygon(rightHandPolygon);
        gHand.setColor(Color.BLACK);
        gHand.drawPolygon(rightHandPolygon);

        // วาดและเติมสี Polygon มือซ้าย
        gHand.setColor(Color.WHITE);
        gHand.fillPolygon(leftHandPolygon);
        gHand.setColor(Color.BLACK);
        gHand.drawPolygon(leftHandPolygon);

        // ปิด Graphics2D
        gHand.dispose();

        // วาด BufferedImage ลงบน Graphics2D หลัก
        g2d.drawImage(handImage, 0, 0, null);
    }

    private void drawBaseCoffin(Graphics2D g2d) {
        int cx = 300;
        int cy = 300;
        Polygon base = new Polygon();
        base.addPoint(cx - 100, cy - 200);
        base.addPoint(cx + 100, cy - 200);
        base.addPoint(cx + 200, cy);
        base.addPoint(cx + 120, cy * 2);
        base.addPoint(cx - 120, cy * 2);
        base.addPoint(cx - 200, cy);
        g2d.setColor(new Color(102, 51, 0));
        g2d.fillPolygon(base);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(base);
        //red carpet
            Polygon smallBase = new Polygon();
            smallBase.addPoint(cx - 90, cy - 180);   // บนซ้าย
            smallBase.addPoint(cx + 90, cy - 180);   // บนขวา
            smallBase.addPoint(cx + 180, cy);        // กลางขวา
            smallBase.addPoint(cx + 100, cy + 280);   // ล่างขวา
            smallBase.addPoint(cx - 100, cy + 280);   // ล่างซ้าย
            smallBase.addPoint(cx - 180, cy);        // กลางซ้าย
        // สร้าง Gradient จากบนลงล่าง (แดงเข้ม → แดงสด)
           GradientPaint redCarpetGradient = new GradientPaint(
        cx-50, cy -100, new Color(50,0,0),   // ด้านบนแดงสด
        cx+50, cy +280, new Color(170, 0, 0)     // ด้านล่างแดงเข้มเกือบดำ
    );
            g2d.setPaint(redCarpetGradient);
            g2d.fillPolygon(smallBase);

            // วาดเส้นขอบ
            g2d.setColor(new Color(0,0,0));
            g2d.drawPolygon(smallBase);

        

    }

    private void drawLidCoffin(Graphics2D g2d) {
       // ฝาโลงเหมือนฐาน
        int cx = 300;
        int cy = 300;
        Polygon lid = new Polygon();
        lid.addPoint(cx - 100, cy - 200);       // บนซ้าย(200,100)
        lid.addPoint(cx + 100, cy - 200);       // บนขวา(400,100)
        lid.addPoint(cx + 200, cy);             // กลางขวา(500,300)
        lid.addPoint(cx + 120 , cy * 2);    // ล่างขวา(420,600)
        lid.addPoint(cx - 120, cy * 2);    // ล่างซ้าย(180,600)
        lid.addPoint(cx - 200 , cy);        // กลางซ้าย(100,300)


       
        // Gradient น้ำตาลเข้ม → น้ำตาลอ่อน
        GradientPaint lidGradient = new GradientPaint(
            cx, cy - 200, new Color(60, 30, 10),   // ด้านบนเข้มมาก
            cx, cy * 2, new Color(153, 102, 0)     // ด้านล่างน้ำตาลอ่อน
        );
        g2d.setPaint(lidGradient);
        g2d.fillPolygon(lid);

        // ขอบฝาโลง
        g2d.setStroke(new BasicStroke(3)); // ความหนาเส้นขอบ
        g2d.setColor(new Color(50, 25, 5));
        g2d.drawPolygon(lid);
        g2d.setStroke(new BasicStroke(1)); // คืนความหนาเส้นขอบ

        // ไม้กางเขน
        int crossCenterX = cx;
        int crossCenterY = cy - 500/4; // ขยับขึ้นเล็กน้อย
        int crossWidth = (int)(600 * 0.05);
        int crossHeight = (int)(450 * 0.35);
        int crossBarWidth = (int)(600 * 0.16);
        int crossBarHeight = (int)(300 * 0.08);

        // สีทอง
        g2d.setColor(new Color(240, 220, 150));
        // แกนยาว (vertical)
        g2d.fillRect(crossCenterX - crossWidth / 2, crossCenterY - crossHeight / 8 + 25, crossWidth, crossHeight);
        // แกนขวาง (horizontal)
        g2d.fillRect(crossCenterX - crossBarWidth / 2, crossCenterY - crossBarHeight + 60, crossBarWidth, crossBarHeight);
        
    }

    private void drawShair(Graphics2D g2d){
        int dx = 30;
        int dy = 160;

        // สร้าง Polygon
        Polygon chairPolygon = new Polygon();

        // เพิ่มจุดจากเส้นตรงและเส้นโค้ง Bezier ลงใน Polygon
        addBresenhamLinePointsToPolygon(chairPolygon, 248 + dx, 200 + dy, 423 + dx, 215 + dy);
        addBezierPointsToPolygon(chairPolygon, 423 + dx, 215 + dy, 440 + dx, 214 + dy, 440 + dx, 245 + dy, 440 + dx, 245 + dy);
        addBresenhamLinePointsToPolygon(chairPolygon, 440 + dx, 245 + dy, 433 + dx, 457 + dy);
        addBresenhamLinePointsToPolygon(chairPolygon, 433 + dx, 457 + dy, 355 + dx, 485 + dy);
        addBresenhamLinePointsToPolygon(chairPolygon, 355 + dx, 485 + dy, 146 + dx, 451 + dy);
        addBresenhamLinePointsToPolygon(chairPolygon, 146 + dx, 451 + dy, 139 + dx, 354 + dy);
        addBezierPointsToPolygon(chairPolygon, 139 + dx, 354 + dy, 135 + dx, 330 + dy, 207 + dx, 306 + dy, 207 + dx, 306 + dy);
        addBezierPointsToPolygon(chairPolygon, 207 + dx, 306 + dy, 209 + dx, 200 + dy, 248 + dx, 200 + dy, 248 + dx, 200 + dy);
        
        // เติมสี Polygon
        g2d.setColor(new Color(210, 210, 210));
        g2d.fillPolygon(chairPolygon);

        // วาดเส้นขอบ
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(chairPolygon);

        // วาดเส้นส่วนประกอบอื่นๆ ที่ไม่ได้เป็นส่วนหนึ่งของรูปทรงหลัก
        g2d.drawLine(139 + dx, 354 + dy, 351 + dx, 388 + dy);
        g2d.drawLine(351 + dx, 388 + dy, 355 + dx, 485 + dy);
        bezierCurve(g2d, 351 + dx, 388 + dy, 340 + dx, 368 + dy, 394 + dx, 333 + dy, 394 + dx, 333 + dy);
        g2d.drawLine(207 + dx, 306 + dy, 393 + dx, 330 + dy);
        bezierCurve(g2d, 423 + dx, 215 + dy, 385 + dx, 197 + dy, 393 + dx, 325 + dy, 393 + dx, 325 + dy);
        g2d.drawLine(393 + dx, 325 + dy, 402 + dx, 468 + dy);
    }

    public void drawhair(Graphics2D g2d) {
    int dx = -40;
    int dy = 100;

    // สร้าง Polygon
    Polygon hairPolygon = new Polygon();

    // เพิ่มจุดจากเส้นโค้ง Bezier ลงใน Polygon
    addBezierPointsToPolygon(hairPolygon, 293 + dx, 224 + dy, 285 + dx, 171 + dy, 291 + dx, 156 + dy, 323 + dx, 130 + dy);
    addBezierPointsToPolygon(hairPolygon, 323 + dx, 130 + dy, 355 + dx, 105 + dy, 411 + dx, 118 + dy, 430 + dx, 163 + dy);
    addBezierPointsToPolygon(hairPolygon, 430 + dx, 163 + dy, 449 + dx, 209 + dy, 411 + dx, 253 + dy, 448 + dx, 287 + dy);
    addBezierPointsToPolygon(hairPolygon, 448 + dx, 287 + dy, 484 + dx, 322 + dy, 434 + dx, 323 + dy, 455 + dx, 334 + dy);
    addBezierPointsToPolygon(hairPolygon, 455 + dx, 334 + dy, 474 + dx, 346 + dy, 309 + dx, 343 + dy, 286 + dx, 334 + dy);
    addBezierPointsToPolygon(hairPolygon, 286 + dx, 334 + dy, 263 + dx, 323 + dy, 293 + dx, 325 + dy, 283 + dx, 313 + dy);
    addBezierPointsToPolygon(hairPolygon, 283 + dx, 313 + dy, 269 + dx, 295 + dy, 305 + dx, 287 + dy, 293 + dx, 224 + dy);

    // เติมสี Polygon
    g2d.setColor(new Color(238, 157, 236)); // สีชมพู
    g2d.fillPolygon(hairPolygon);

    // วาดเส้นขอบ (ถ้าต้องการ)
    g2d.setColor(Color.BLACK);
    g2d.drawPolygon(hairPolygon);
}
    public void drawbang(Graphics2D g2d){
    int dx = -40;
    int dy =100;
    int x=10;
   GeneralPath bang  = new GeneralPath();
   bang.moveTo(341+dx, 144+dy);
   bang.curveTo(341+dx,144+dy,298+dx,191+dy,385+dx,191+dy);
   bang.curveTo(471+dx,191+dy,374+dx,111+dy,341+dx,144+dy);
    
   g2d.setColor(new Color(238, 157, 236)); // สีชมพู
   g2d.fill(bang);
   g2d.setColor(Color.BLACK);
   bezierCurve(g2d, 288+x,250, 250+x,303, 360+x,288, 360+x,288);
}

    public void drawShirt(Graphics2D g2d){
       int dx = 10;
       int dy = 170;
        Polygon shirtPolygon = new Polygon();
        //    GeneralPath shirt = new GeneralPath();
        //    shirt.moveTo(336+dx,178+dy);
        //    shirt.curveTo(336+dx, 178+dy, 318+dx, 192+dy, 299+dx, 182+dy);
        //      shirt.curveTo(278+dx, 172+dy, 240+dx, 261+dy, 244+dx, 275+dy);
        //     shirt.curveTo(244+dx, 275+dy, 223+dx, 291+dy, 215+dx, 306+dy);
        //     shirt.curveTo(215+dx, 306+dy, 237+dx, 282+dy, 256+dx, 310+dy);
        //     shirt.curveTo(256+dx, 310+dy, 292+dx, 281+dy, 294+dx, 335+dy);
        //     shirt.curveTo(294+dx, 335+dy, 373+dx, 329+dy, 377+dx, 302+dy);
        //     shirt.curveTo(381+dx, 275+dy, 379+dx, 220+dy, 336+dx, 178+dy);
        //         g2d.setColor(new Color(131,192,231)); // สีน้ำเงิน
                // เพิ่มจุดจากเส้นโค้ง Bezier ลงใน Polygon
        addBezierPointsToPolygon(shirtPolygon, 336 + dx, 178 + dy, 318 + dx, 192 + dy, 299 + dx, 182 + dy, 299 + dx, 182 + dy);
        addBezierPointsToPolygon(shirtPolygon, 299 + dx, 182 + dy,278 + dx, 172 + dy, 240 + dx, 261 + dy, 244 + dx, 275 + dy);
        addBezierPointsToPolygon(shirtPolygon, 244 + dx, 275 + dy, 223+dx, 291+dy, 215+dx, 306+dy,215+dx, 306+dy);
        addBezierPointsToPolygon(shirtPolygon, 215+dx, 306+dy,237+dx, 282+dy, 256+dx, 310+dy,256+dx, 310+dy);
        addBezierPointsToPolygon(shirtPolygon, 256+dx, 310+dy,292+dx, 281+dy, 294+dx, 335+dy, 294+dx, 335+dy);
        addBezierPointsToPolygon(shirtPolygon,294+dx, 335+dy, 373+dx, 329+dy, 377+dx, 302+dy, 377+dx, 302+dy);
        addBezierPointsToPolygon(shirtPolygon, 377+dx, 302+dy,381+dx, 275+dy, 379+dx, 220+dy, 336+dx, 178+dy);


        // เติมสี Polygon
        g2d.setColor(new Color(131, 192, 231)); // สีน้ำเงิน
        g2d.fillPolygon(shirtPolygon);

        // วาดเส้นขอบ
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(shirtPolygon);

        // วาดรายละเอียดเพิ่มเติม (ถ้าต้องการ)
        g2d.setColor(Color.BLACK);
        bezierCurve(g2d, 329 + dx, 227 + dy, 330 + dx, 271 + dy, 323 + dx, 277 + dy, 323 + dx, 277 + dy);
        bezierCurve(g2d, 323 + dx, 277 + dy, 270 + dx, 293 + dy, 256 + dx, 310 + dy, 256 + dx, 310 + dy);
        bezierCurve(g2d, 281 + dx, 224 + dy, 262 + dx, 249 + dy, 256 + dx, 310 + dy, 256 + dx, 310 + dy);
        
        
    }

     private void drawBone(Graphics2D g2d) {
        int dx = 94;
        int dy = 170;
// Bone3
    Polygon bone3Polygon = new Polygon();
    addBezierPointsToPolygon(bone3Polygon, 250 + dx, 132 + dy, 262 + dx, 145 + dy, 248 + dx, 157 + dy, 189 + dx, 159 + dy);
    addBezierPointsToPolygon(bone3Polygon, 189 + dx, 159 + dy, 201 + dx, 148 + dy, 190 + dx, 137 + dy, 250 + dx, 132 + dy);
    g2d.setColor(new Color(74, 68, 70));
    g2d.fillPolygon(bone3Polygon);

    // Bone
    Polygon bonePolygon = new Polygon();
    addBezierPointsToPolygon(bonePolygon, 174 + dx, 130 + dy, 152 + dx, 102 + dy, 175 + dx, 72 + dy, 198 + dx, 40 + dy);
    addBezierPointsToPolygon(bonePolygon, 198 + dx, 40 + dy, 269 + dx, 46 + dy, 285 + dx, 77 + dy, 299 + dx, 109 + dy);
    addBezierPointsToPolygon(bonePolygon, 299 + dx, 109 + dy, 297 + dx, 153 + dy, 258 + dx, 156 + dy, 267 + dx, 179 + dy);
    addBezierPointsToPolygon(bonePolygon, 267 + dx, 179 + dy, 216 + dx, 175 + dy, 183 + dx, 177 + dy, 189 + dx, 162 + dy);
    addBezierPointsToPolygon(bonePolygon, 189 + dx, 162 + dy, 182 + dx, 153 + dy, 194 + dx, 153 + dy, 202 + dx, 152 + dy);
    addBezierPointsToPolygon(bonePolygon, 202 + dx, 152 + dy, 199 + dx, 162 + dy, 199 + dx, 153 + dy, 207 + dx, 153 + dy);
    addBezierPointsToPolygon(bonePolygon, 207 + dx, 153 + dy, 214 + dx, 152 + dy, 213 + dx, 163 + dy, 213 + dx, 151 + dy);
    addBezierPointsToPolygon(bonePolygon, 213 + dx, 151 + dy, 221 + dx, 153 + dy, 227 + dx, 154 + dy, 226 + dx, 162 + dy);
    addBezierPointsToPolygon(bonePolygon, 226 + dx, 162 + dy, 228 + dx, 142 + dy, 241 + dx, 158 + dy, 242 + dx, 159 + dy);
    addBezierPointsToPolygon(bonePolygon, 242 + dx, 159 + dy, 263 + dx, 146 + dy, 241 + dx, 132 + dy, 238 + dx, 138 + dy);
    addBezierPointsToPolygon(bonePolygon, 238 + dx, 138 + dy, 239 + dx, 144 + dy, 225 + dx, 156 + dy, 225 + dx, 137 + dy);
    addBezierPointsToPolygon(bonePolygon, 225 + dx, 137 + dy, 218 + dx, 158 + dy, 210 + dx, 138 + dy, 207 + dx, 159 + dy);
    addBezierPointsToPolygon(bonePolygon, 207 + dx, 159 + dy, 199 + dx, 138 + dy, 193 + dx, 159 + dy, 188 + dx, 139 + dy);
    addBezierPointsToPolygon(bonePolygon, 188 + dx, 139 + dy, 190 + dx, 150 + dy, 180 + dx, 148 + dy, 172 + dx, 145 + dy);
    addBezierPointsToPolygon(bonePolygon, 172 + dx, 145 + dy, 174 + dx, 130 + dy, 174 + dx, 130 + dy, 174 + dx, 130 + dy);
    g2d.setColor(Color.WHITE);
    g2d.fillPolygon(bonePolygon);
    g2d.setColor(Color.BLACK);
    g2d.drawPolygon(bonePolygon);

        g2d.setColor(new Color(74,68,70));
        g2d.fillOval(171+dx, 95+dy, 20, 26);
        g2d.fillOval(218+dx, 95+dy, 20, 26);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(171+dx, 95+dy, 20, 26);
        g2d.drawOval(218+dx, 95+dy, 20, 26);

        bezierCurve(g2d, 240+dx, 142+dy, 235+dx, 128+dy, 254+dx, 126+dy, 254+dx, 126+dy);
        bezierCurve(g2d, 261+dx, 124+dy, 273+dx, 122+dy, 271+dx, 108+dy, 271+dx, 108+dy);

        GeneralPath bone2 = new GeneralPath();  
        bone2.moveTo(200+dx, 126+dy);
        bone2.curveTo(200+dx, 126+dy, 205+dx, 132+dy, 211+dx, 126+dy);
        bone2.curveTo(211+dx, 126+dy, 209+dx, 116+dy, 201+dx, 114+dy);
        bone2.curveTo(201+dx, 114+dy, 188+dx, 122+dy, 192+dx, 128+dy);
        bone2.curveTo(192+dx, 128+dy, 193+dx, 132+dy, 200+dx, 126+dy);
        g2d.setColor(new Color(74,68,70));
        g2d.fill(bone2);
        g2d.setColor(Color.BLACK);
        g2d.draw(bone2);  
    }
    private void drawPieceDesk(Graphics2D g2d){
         int y = -33;
        int x = -15;
        GeneralPath d = new GeneralPath();
        d.moveTo(276+x, 473+y);
        d.lineTo(308+x, 491+y);
        d.curveTo(308+x, 491+y,285+x,499+y,270+x,511+y);
        d.curveTo(270+x,511+y, 271+x,475+y, 276+x, 473+y);
        g2d.setColor(new Color(153, 102, 51));
        g2d.fill(d);
    }
    
    private void drawPants(Graphics2D g2d){
        int dx = 20;
        int dy = 150;
        GeneralPath pant  = new GeneralPath();
        pant.moveTo(363+dx,320+dy);
        pant.curveTo(363+dx,320+dy, 397+dx,401+dy,288+dx,386+dy);
        pant.lineTo(287+dx,455+dy);
        pant.curveTo(287+dx,455+dy,252+dx,476+dy,232+dx,458+dy);
        pant.curveTo(232+dx,458+dy,212+dx,466+dy,199+dx,451+dy);
        pant.curveTo(199+dx,451+dy,174+dx,352+dy,206+dx,331+dy);
        pant.curveTo(237+dx,309+dy,251+dx,306+dy,268+dx,306+dy);
        pant.lineTo(363+dx, 320+dy);
        g2d.setColor(Color.BLACK);
        g2d.draw(pant);
        g2d.fill(pant);
        GeneralPath p2 = new GeneralPath();
        p2.moveTo(261+dx, 327+dy);
        p2.curveTo(261+dx, 327+dy, 223+dx, 324+dy, 222+dx, 359+dy);
        p2.curveTo(219+dx, 392+dy, 225+dx, 444+dy, 232+dx, 458+dy);
        g2d.setColor(new Color(68,71,73));
        g2d.draw(p2);
    }
    
    private void drawPantsStanding1(Graphics2D g2d){
        int dx = 20;
        int dy = 150; // ยกตัวขึ้นเล็กน้อย (จาก 150 -> 120)
        GeneralPath pant  = new GeneralPath();
        pant.moveTo(360+dx,300+dy);
        pant.curveTo(360+dx,300+dy, 410+dx,355+dy,288+dx,386+dy); // ลดความโค้งลง
        pant.lineTo(287+dx,455+dy);
        pant.curveTo(287+dx,455+dy,252+dx,470+dy,232+dx,458+dy);
        pant.curveTo(232+dx,458+dy,212+dx,460+dy,199+dx,451+dy);
        pant.curveTo(199+dx,451+dy,174+dx,350+dy,206+dx,331+dy);
        pant.curveTo(206+dx,331+dy,243+dx,297+dy,268+dx,296+dy);
        pant.lineTo(360+dx, 300+dy);
        g2d.setColor(Color.BLACK);
        g2d.draw(pant);
        g2d.fill(pant);

        GeneralPath p2 = new GeneralPath();
            p2.moveTo(261+dx, 327+dy);
            p2.curveTo(261+dx, 327+dy, 223+dx, 324+dy, 222+dx, 359+dy);
            p2.curveTo(219+dx, 392+dy, 225+dx, 444+dy, 232+dx, 458+dy);
            g2d.setColor(new Color(68,71,73));
            g2d.draw(p2);
    }

    private void drawPantsStanding2(Graphics2D g2d){
        int dx = 20;
        int dy = 150; // ยกขึ้นมากกว่าเดิม
        GeneralPath pant  = new GeneralPath();
        pant.moveTo(337+dx,276+dy);
        pant.curveTo(363+dx,310+dy, 380+dx,317+dy,288+dx,386+dy);
        pant.lineTo(287+dx,455+dy);
        pant.curveTo(287+dx,455+dy,252+dx,470+dy,232+dx,458+dy);
        pant.curveTo(232+dx,458+dy,212+dx,466+dy,199+dx,451+dy);
        pant.curveTo(199+dx,451+dy,174+dx,350+dy,206+dx,331+dy);
        pant.curveTo(206+dx,331+dy,238+dx,282+dy,250+dx,276+dy);
        pant.lineTo(337+dx,276+dy);
        g2d.setColor(Color.BLACK);
        g2d.draw(pant);
        g2d.fill(pant);

    GeneralPath p2 = new GeneralPath();
            p2.moveTo(261+dx, 327+dy);
            p2.curveTo(261+dx, 327+dy, 223+dx, 324+dy, 222+dx, 359+dy);
            p2.curveTo(219+dx, 392+dy, 225+dx, 444+dy, 232+dx, 458+dy);
            g2d.setColor(new Color(68,71,73));
            g2d.draw(p2);
    }
    private void drawPantsStanding3(Graphics2D g2d){
        int dx = 20;
        int dy = 150; // ยกเต็มที่
        GeneralPath pant  = new GeneralPath();
        pant.moveTo(315+dx,263+dy);
        pant.curveTo(315+dx,263+dy, 347+dx,291+dy,283+dx,372+dy); // ขาตรงขึ้น
        pant.lineTo(287+dx,455+dy);
        pant.curveTo(287+dx,455+dy,252+dx,470+dy,232+dx,458+dy);
    pant.curveTo(232+dx,458+dy,212+dx,466+dy,199+dx,451+dy);
        pant.curveTo(189+dx,441+dy,185+dx,374+dy,190+dx,360+dy);
        pant.curveTo(193+dx,346+dy,219+dx,276+dy,243+dx,263+dy);
        pant.lineTo(315+dx, 263+dy);
        g2d.setColor(Color.BLACK);
        g2d.draw(pant);
        g2d.fill(pant);

        GeneralPath p2 = new GeneralPath();
            p2.moveTo(261+dx, 327+dy);
            p2.curveTo(261+dx, 327+dy, 223+dx, 324+dy, 222+dx, 359+dy);
            p2.curveTo(219+dx, 392+dy, 225+dx, 444+dy, 232+dx, 458+dy);
            g2d.setColor(new Color(68,71,73));
            g2d.draw(p2);
    }
    private void drawPantsStanding4(Graphics2D g2d) {
        int dx = 20;
        int dy = 150; // ยกเต็มที่
        GeneralPath pant  = new GeneralPath();
        pant.moveTo(293+dx,260+dy);
        pant.curveTo(293+dx,260+dy, 311+dx,290+dy,283+dx,372+dy);
        pant.lineTo(287+dx,455+dy);
        pant.curveTo(287+dx,455+dy,252+dx,470+dy,232+dx,458+dy);
        pant.curveTo(232+dx,458+dy,212+dx,466+dy,199+dx,451+dy);
        pant.curveTo(185+dx,437+dy,188+dx,364+dy,190+dx,348+dy);
        pant.curveTo(195+dx,334+dy,202+dx,281+dy,221+dx,260+dy);
        pant.lineTo(293+dx,260+dy);
        g2d.setColor(Color.BLACK);
        g2d.draw(pant);
        g2d.fill(pant);

        GeneralPath p2 = new GeneralPath();
        p2.moveTo(261+dx, 327+dy);
        p2.curveTo(261+dx, 327+dy, 223+dx, 324+dy, 222+dx, 359+dy);
        p2.curveTo(219+dx, 392+dy, 225+dx, 444+dy, 232+dx, 458+dy);
        g2d.setColor(new Color(68,71,73));
        g2d.draw(p2);
    }
    private void drawPantsStand5(Graphics2D g2d) {
     int dx = 20;
    int dy = 150; // ยกเต็มที่
    GeneralPath pant  = new GeneralPath();
    pant.moveTo(297+dx, 260+dy);
    pant.curveTo(297+dx,260+dy,299+dx, 427+dy, 287+dx, 455+dy);
    pant.curveTo(287+dx,455+dy,467+dx,472+dy,240+dx,457+dy);
    pant.curveTo(240+dx,457+dy,215+dx,472+dy,199+dx,456+dy);
    pant.curveTo(199+dx,456+dy,179+dx,288+dy,188+dx,261+dy);
    pant.lineTo(297+dx, 260+dy);
    g2d.setColor(Color.BLACK);
    g2d.draw(pant);
    g2d.fill(pant);

    GeneralPath p2 = new GeneralPath();
    p2.moveTo(233+dx, 342+dy);
    p2.curveTo(233+dx, 342+dy, 232+dx, 419+dy, 240+dx,457+dy);
    
    g2d.setColor(new Color(68,71,73));
    g2d.draw(p2);

}
    private void drawDesk(Graphics2D g2d){
        g2d.setColor(new Color(153, 102, 51)); // สีน้ำตาลโต๊ะ
            int dx= 0;
        // กำหนดจุดสี่เหลี่ยมคางหมูเพื่อทำให้โต๊ะเอียง
        int[] xPoints = {0+dx, 550+dx, 600+dx, 0+dx};
        int[] yPoints = {300, 600, 600, 600};
        

        Polygon desk = new Polygon(xPoints, yPoints, 4);
        g2d.fillPolygon(desk);
        }
        private void drawLaptop(Graphics2D g2d) {
        
        // ======= เพิ่มเงาใต้ Laptop =======
        g2d.setColor(new Color(0, 0, 0, 50)); // โปร่งใส
        g2d.fillOval(140, 500, 190, 30); // ขยับขึ้นและขยายให้เงาอยู่ใกล้และใต้ Laptop มากขึ้น
        // ======= วาดฐานคีย์บอร์ด =======
        g2d.setColor(new Color(30, 30, 30)); // สีเทาเข้ม
        int[] baseX = {145 + 40, 81 + 40, 226 + 40, 303 + 40}; // ขยับขวาอีก 40
        int[] baseY = {414 + 70, 437 + 70, 452 + 70, 431 + 70};
        g2d.fillPolygon(baseX, baseY, 4);

        // ======= วาดฝาจอ =======
        g2d.setColor(new Color(20, 20, 20)); // ดำสนิท
        int[] screenX = {50+40, 81+40, 227+40, 201+40}; // ขยับขวาอีก 40
        int[] screenY = {324+70, 437+70, 451+70, 338+70};
        g2d.fillPolygon(screenX, screenY, 4);

        // วาดแอปเปิ้ล (ใช้วงรีสองอันซ้อนกัน)
        g2d.setColor(new Color(180, 180, 180)); // สีเทาเงิน
        g2d.fillOval(110+40, 370+70, 30, 35); // วงรีหลัก
        g2d.fillOval(125+40, 370+70, 30, 35); // วงรีซ้อนด้านขวา
        g2d.setColor(new Color(20, 20, 20)); // สีพื้นหลัง
        g2d.fillOval(140+40, 377+70, 25, 25);

        // เส้นขอบจอ
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(screenX, screenY, 4);
        
        // ;วาดเเก้วน้ำ
        g2d.setColor(new Color(180, 138, 96)); // ดำสนิท
        int[] glassX = {25, 72, 68, 34}; // ขยับขวาอีก 40
        int[] glassY = {403, 404, 454, 454};
        g2d.fillPolygon(glassX, glassY, 4);
        // วาดฐานแก้ว (วงรีล่าง)
        g2d.setColor(new Color(180, 138, 96));
        g2d.fillOval(34, 445, 35, 12); // ฐานแก้ว

        // วาดฝาแก้ว (วงรีบน) ให้กว้างเท่าปากแก้ว
        g2d.setColor(new Color(200, 170, 120));
        g2d.fillOval(25, 397, 47, 12); // ฝาแก้ว (กว้างเท่าปากแก้ว polygon)
        g2d.setColor(Color.BLACK);
        midpointElipse(g2d, 25+47/2, 397+12/2, 47/2, 12/2);
        //วาดหลอด
        g2d.setColor(new Color(0,0,0)); // สีเทาอ่อน
        g2d.setStroke(new BasicStroke(5)); // ความหนาหลอด
        g2d.drawLine(34, 373, 49, 402); // จุดเริ่มต้นที่ปากแก้วไปด้านบนซ้าย
        g2d.drawLine(24, 387, 33, 374);
        g2d.setStroke(new BasicStroke(1)); // คืนความหนาเดิม
        // ======= วาดหนังสือเปิดสีขาวตรงมุมซ้ายล่าง (ขนาดเล็กลงและขอบโค้ง) =======
        g2d.setColor(Color.WHITE);
        // วาดหน้าซ้าย (ขนาดเล็กลงและขยับขึ้น)
        int[] bookLeftX = {50, 105, 90, 30};
        int[] bookLeftY = {510, 525, 545, 530};
        g2d.fillPolygon(bookLeftX, bookLeftY, 4);
        // วาดหน้าขวา (ขนาดเล็กลงและขยับขึ้น)
        int[] bookRightX = {105, 160, 145, 90};
        int[] bookRightY = {525, 510, 540, 545};
        g2d.fillPolygon(bookRightX, bookRightY, 4);

        // เส้นขอบหนังสือ
        g2d.setColor(Color.BLACK);
        // ขอบหน้าซ้าย
        g2d.drawPolygon(bookLeftX, bookLeftY, 4);
        // ขอบหน้าขวา
        g2d.drawPolygon(bookRightX, bookRightY, 4);
        // วาดรอยพับกลางหนังสือ
        g2d.drawLine(90, 545, 105, 525);
    }

     public void midpointElipse(Graphics g,int xc,int yc,int a,int b){
        int x,y,d;
        //region 1
        x=0;
        y=b;
        d=Math.round(b*b-a*a*b+a*a/4);
        
        while(b*b*x <= a*a*y){
            plot(g,x+xc,y+yc,2);
            plot(g,-x+xc,y+yc,2);
            plot(g,x+xc,-y+yc,2);
            plot(g,-x+xc,-y+yc,2);

            x++;

            if(d>=0){
                y--;
                d= d-2*a*a*y;
            }
            d=d+2*b*b*x+ b*b;
        }
        //region 2
        x=a;
        y=0;
        d=Math.round(a*a-b*b*a+b*b/4);

        while(b*b*x >= a*a*y){
            plot(g,x+xc,y+yc,2);
            plot(g,-x+xc,y+yc,2);
            plot(g,x+xc,-y+yc,2);
            plot(g,-x+xc,-y+yc,2);

            y++;
            if(d>=0){
                x--;
                d=d-2*b*b*x;
            }
            d=d+2*a*a*y+a*a;
        }

    }
    
    public static void bezierCurve(Graphics g,int x1,int y1,int x2,int y2,int x3 ,int y3,int x4,int y4){ 
        for(int i =0;i<3000;i++){ double t = i/(double)(2999); //find the value of t each round 
            double xt = Math.pow(1-t, 3)*x1 + 3*t*Math.pow(1-t,2)*x2+ 3*Math.pow(t, 2)*(1-t)*x3+ Math.pow(t,3)*x4; 
            double yt = Math.pow(1-t, 3)*y1 + 3*t*Math.pow(1-t,2)*y2+ 3*Math.pow(t, 2)*(1-t)*y3+ Math.pow(t,3)*y4; 
            plot(g, (int)Math.round(xt), (int)Math.round(yt), 2);
        } 
    }
    private void addBezierPointsToPolygon(Polygon polygon, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        for (int i = 0; i < 3000; i++) {
            double t = i / (double) (2999);
            double xt = Math.pow(1 - t, 3) * x1 + 3 * t * Math.pow(1 - t, 2) * x2 + 3 * Math.pow(t, 2) * (1 - t) * x3 + Math.pow(t, 3) * x4;
            double yt = Math.pow(1 - t, 3) * y1 + 3 * t * Math.pow(1 - t, 2) * y2 + 3 * Math.pow(t, 2) * (1 - t) * y3 + Math.pow(t, 3) * y4;
            polygon.addPoint((int) Math.round(xt), (int) Math.round(yt));
        }
    }

     // ฟังก์ชันเพิ่มจุดจากเส้นตรง Bresenham ลงใน Polygon
private void addBresenhamLinePointsToPolygon(Polygon polygon, int x1, int y1, int x2, int y2) {
    int dx = Math.abs(x2 - x1);
    int dy = Math.abs(y2 - y1);
    int sx = (x1 < x2) ? 1 : -1;
    int sy = (y1 < y2) ? 1 : -1;
    boolean isSwap = false;

    if (dy > dx) {
        int temp = dx;
        dx = dy;
        dy = temp;
        isSwap = true;
    }

    int D = 2 * dy - dx;
    int x = x1;
    int y = y1;
    
    for (int i = 0; i <= dx; i++) {
        polygon.addPoint(x, y);

        if (D >= 0) {
            if (isSwap)
                x += sx;
            else
                y += sy;

            D -= 2 * dx;
        }

        if (isSwap)
            y += sy;
        else
            x += sx;

        D += 2 * dy;
    }

}
    
    private static void plot(Graphics g, int x, int y, int size){
        g.fillRect(x, y, size, size); // ทำให้ pixel ใหญ่ขึ้น
    }

     public  static BufferedImage floodFill(BufferedImage image, int x, int y, Color targetColor,Color replacementColor) {

        int targetRGB = targetColor.getRGB();
        int replacementRGB = replacementColor.getRGB();

        if (image.getRGB(x, y) != targetRGB || targetRGB == replacementRGB) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x, y));

        while (!queue.isEmpty()) {
            Point p = queue.remove();
            int px = p.x;
            int py = p.y;

            if (px < 0 || px >= width || py < 0 || py >= height)
                continue;
            if (image.getRGB(px, py) != targetRGB)
                continue;

            image.setRGB(px, py, replacementRGB);

            queue.add(new Point(px + 1, py));
            queue.add(new Point(px - 1, py));
            queue.add(new Point(px, py + 1));
            queue.add(new Point(px, py - 1));
        }

        return image;
    }
    private static void bresenhamLine(Graphics g, int x1, int y1, int x2, int y2,int si) {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);

            int sx = (x1 < x2) ? 1 : -1;
            int sy = (y1 < y2) ? 1 : -1;
             int size = si;
            boolean isSwap = false;

            if (dy > dx) {
                int temp = dx;
                dx = dy;
                dy = temp;
                isSwap = true;
            }

            int D = 2 * dy - dx;

            int x = x1;
            int y = y1;

            for (int i = 0; i <= dx; i++) {
                plot(g, x, y,size);

                if (D >= 0) {
                    if (isSwap)
                        x += sx;
                    else
                        y += sy;

                    D -= 2 * dx;
                }

                if (isSwap)
                    y += sy;
                else
                    x += sx;

                D += 2 * dy;
            }
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
