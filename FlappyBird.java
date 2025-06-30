
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    int frameWidth = 800;
    int frameHeight = 600;

    Timer timer;
    int delay = 20;

    Rectangle bird;
    int birdWidth = 40;
    int birdHeight = 30;
    int birdY = 300;
    int birdX = 200;
    int velocity = 0;
    int gravity = 1;

    ArrayList<Rectangle> pipes;
    int pipeWidth = 80;
    int pipeGap = 200;
    int pipeSpeed = 5;
    int score = 0;
    int highScore = 0;
    boolean gameOver = false;

    Image birdImage;
    Image []backgroundImages;
    int currentBackgroundIndex=0;
    Image pipeImage;
    Image starImage;
    Image coinImage;
    

    double starAngle = 0;
    int starOffsetX = 0;
    int starDirection = 1;
    int bgX = 0;

    Rectangle coin;
    boolean coinVisible = false;
    boolean coinCollected = false;
    int nextCoinScore=5;
    int totalCoinsCollected=0;

    
   

    long lastCycleChange = System.currentTimeMillis();
    long lastBackgroundChange = System.currentTimeMillis();

    long startTime;
    long elapsedTime;
    long totalDistance = 0;
    long highestDistance = 0;
    long highestTime = 0;

    Clip coinSound;
    Clip collisionSound;
    Clip backgroundMusic;

    Image cloudImage, mountainImage;
    int cloudX = 0;
    int mountainX = 0;
    int lastScoreCheck = 0;



    public FlappyBird() {
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        setBackground(Color.cyan);
        addKeyListener(this);
        setFocusable(true);

        try {
            birdImage = ImageIO.read(getClass().getResource("bird.png"));
            backgroundImages = new Image[]{

                 ImageIO.read(getClass().getResource("background.png")),
                 ImageIO.read(getClass().getResource("background2.png")),
                 ImageIO.read(getClass().getResource("background3.png")),
                 ImageIO.read(getClass().getResource("background4.png"))
            };
            starImage = ImageIO.read(getClass().getResource("star.png"));
            coinImage = ImageIO.read(getClass().getResource("coin.png"));
            cloudImage = ImageIO.read(getClass().getResource("clouds.png"));
            // mountainImage = ImageIO.read(getClass().getResource("mountain.png"));


            AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource("coinSound.wav"));
            coinSound = AudioSystem.getClip();
            coinSound.open(audioIn);

            AudioInputStream collisionIn = AudioSystem.getAudioInputStream(getClass().getResource("collision.wav"));
            collisionSound = AudioSystem.getClip();
            collisionSound.open(collisionIn);

            AudioInputStream bgMusicStream = AudioSystem.getAudioInputStream(getClass().getResource("back.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(bgMusicStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (Exception e) {
            e.printStackTrace();
        }


        bird = new Rectangle(birdX, birdY, birdWidth, birdHeight);
        pipes = new ArrayList<>();
        addPipe(true);
        addPipe(true);
        addPipe(true);
        addPipe(true);

        timer = new Timer(delay, this);
        timer.start();
        startTime = System.currentTimeMillis();
    }

    public void addPipe(boolean start) {
        int space = pipeGap;
        int height = 50 + new Random().nextInt(300);
        if (start) {
            pipes.add(new Rectangle(frameWidth + pipes.size() * 300, frameHeight - height, pipeWidth, height));
            pipes.add(new Rectangle(frameWidth + pipes.size() * 300, 0, pipeWidth, frameHeight - height - space));
        } else {
            pipes.add(new Rectangle(pipes.get(pipes.size() - 1).x + 600, frameHeight - height, pipeWidth, height));
            pipes.add(new Rectangle(pipes.get(pipes.size() - 1).x, 0, pipeWidth, frameHeight - height - space));
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        drawBird(g);
        drawSpinningStar(g);
        drawPipes(g);
        drawCoin(g);
        drawScore(g);
        drawStats(g);
        if (gameOver) {
            drawGameOver(g);
        }
        
        
    }

    private void playSound(Clip sound) {
        if (sound != null) {
            if (sound.isRunning()) sound.stop();
            sound.setFramePosition(0);
            sound.start();
        }
    }
    private void drawStats(Graphics g) {
         g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.BLACK);

        String timeDisplay = (elapsedTime >= 60000)
            ? (elapsedTime / 60000) + "m " + ((elapsedTime % 60000) / 1000) + "s"
            : (elapsedTime / 1000) + "s";

        String distanceDisplay = ((totalDistance/40) >= 1000)
            ? String.format("%.2f km", (totalDistance/40) / 1000.0)
            : totalDistance/40 + " m";

        String maxTimeDisplay = (highestTime >= 60000)
            ? (highestTime / 60000) + "m " + ((highestTime % 60000) / 1000) + "s"
            : (highestTime / 1000) + "s";

        String maxDistanceDisplay = ((highestDistance/40) >= 1000)
            ? String.format("%.2f km", (highestDistance/40) / 1000.0)
            : highestDistance/40 + " m";

        g.drawString("Distance: " + distanceDisplay, 20, 80);
        // g.drawString("Time: " + timeDisplay, 20, 110);
        g.drawString("Coins: " + totalCoinsCollected, 20, 110);

        String highScoreText = "Best Score: " + highScore;
        int textWidth = g.getFontMetrics().stringWidth(highScoreText);
        g.setColor(Color.red.darker().darker());
        g.drawString(highScoreText, frameWidth - textWidth - 60, 50);
        g.drawString("Max Dist: " + maxDistanceDisplay, frameWidth - textWidth - 60, 80);
        // g.drawString("Max Time: " + maxTimeDisplay, frameWidth - textWidth - 60, 110);
    }

    
    private void drawBackground(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (backgroundImages != null && backgroundImages.length > 0) {
            g.drawImage(backgroundImages[currentBackgroundIndex], 0, 0, frameWidth, frameHeight, null);
        } else {
            g.setColor(Color.cyan);
            g.fillRect(0, 0, frameWidth, frameHeight);
        }
        if (mountainImage != null) {
        g2d.drawImage(mountainImage, mountainX, 100, frameWidth, 300, null);
        g2d.drawImage(mountainImage, mountainX + frameWidth, 100, frameWidth, 300, null);
    }
        if (cloudImage != null) {
        g2d.drawImage(cloudImage, cloudX, 50, frameWidth, 200, null);
        g2d.drawImage(cloudImage, cloudX + frameWidth, 50, frameWidth, 200, null);
    }
        g.setColor(Color.orange);
        g.fillRect(0, frameHeight - 100, frameWidth, 100);
        g.setColor(Color.green);
        g.fillRect(0, frameHeight - 110, frameWidth, 10);
    }
    
    private void drawBird(Graphics g) {
        if (birdImage != null) {
            g.drawImage(birdImage, bird.x, bird.y, bird.width*(2), bird.height*2, this);
        } else {
            
            g.setColor(Color.red);
            g.fillRect(bird.x, bird.y, bird.width, bird.height);
            
        }
    }
    
    private void drawSpinningStar(Graphics g) {
        if (starImage != null && gameOver) {
            Graphics2D g2d = (Graphics2D) g.create();
            int starSize = 30;
            int centerX = bird.x + bird.width / 2 + starOffsetX / 2;
            int centerY = bird.y - starSize;
            g2d.rotate(starAngle, centerX, centerY);
            g2d.drawImage(starImage, centerX - starSize / 2, centerY - starSize / 2, starSize, starSize, null);
            g2d.dispose();
        }
    }
    private void drawCoin(Graphics g) {
        if(coinVisible && !coinCollected){
            if (coinImage != null) {
                g.drawImage(coinImage, coin.x, coin.y, coin.width*2, coin.height*2, this);
            } else {
                g.setColor(Color.YELLOW);
                g.fillOval(coin.x, coin.y, coin.width, coin.height);
            }
        }
    }

    private void drawPipes(Graphics g) {
        g.setColor(Color.green.darker().darker().darker().darker().darker().brighter().brighter().brighter());
        for (Rectangle pipe : pipes) {
            if (pipeImage != null) {
                g.drawImage(pipeImage, pipe.x, pipe.y, pipe.width, pipe.height, this);
            } else {
                g.setColor(Color.green.darker());
                g.fillRect(pipe.x, pipe.y, pipe.width, pipe.height);
            }

        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.black.darker().darker().darker());
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Score: " + score, 20, 50);

        

    }

    private void drawGameOver(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 80));
        g.setColor(Color.red);
        g.drawString("Game Over", frameWidth / 2 - 200, frameHeight / 2 - 50);
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.setColor(Color.blue);
        g.drawString("Press SPACE to Restart", frameWidth / 2 - 150, frameHeight / 2);
    }

    public void actionPerformed(ActionEvent e) {
       
        if (System.currentTimeMillis() - lastBackgroundChange > 20000) {
            currentBackgroundIndex = (currentBackgroundIndex + 1) % backgroundImages.length;
            lastBackgroundChange = System.currentTimeMillis();
        }

            if (!gameOver) {
                moveBackground();
                movePipes();
                applyGravity();
                checkCollisions();
                updateScore();

                totalDistance += pipeSpeed;
                elapsedTime = System.currentTimeMillis() - startTime;

            if (coinVisible && !coinCollected) {
                coin.x -= pipeSpeed;
                if (coin.intersects(bird)) {
                    score += 5;
                    totalCoinsCollected++;
                    coinCollected = true;
                    coinVisible=false;
                    nextCoinScore+=10;  
                    
                }
             
            coinSound.setFramePosition(0);  
            coinSound.start(); 
            
          
                if (coin.x + coin.width < 0) {
                    coinVisible = false;
                    coinCollected=false;
                    nextCoinScore+=10;
                }
            }
            if(!coinVisible && score >= nextCoinScore){
                spawnCoin();
            }

        

        } else {
            starAngle += 0.1;
            starOffsetX += starDirection * 2;
            if (starOffsetX > 30 || starOffsetX < -30) {
                starDirection *= -1;
            }
            if (totalDistance > highestDistance) highestDistance = totalDistance;
            if (elapsedTime > highestTime) highestTime = elapsedTime;
        }

        repaint();
    }

    private void moveBackground() {
        bgX -= 2;
        cloudX -=1;
        mountainX -=0.5;
        if ( bgX <= -frameWidth) {
            bgX = 0;

        if (bgX <= -frameWidth) bgX = 0;
        if (cloudX <= -frameWidth) cloudX = 0;
        if (mountainX <= -frameWidth) mountainX = 0;

        }
    }

    private void movePipes() {
        ArrayList<Rectangle> toRemove = new ArrayList<>();
        for (Rectangle pipe : pipes) {
            pipe.x -= pipeSpeed;
            
            if (pipe.x + pipe.width < 0) {
                toRemove.add(pipe);
            }

        }

        pipes.removeAll(toRemove);
        if (pipes.size() < 6) {
            addPipe(false);

        }
    }

    private void applyGravity() {
        velocity += gravity;
        bird.y += velocity;
    }

    private void checkCollisions() {
        for (Rectangle pipe : pipes) {
            if (pipe.intersects(bird)) {
                gameOver = true;
                bird.x = pipe.x - bird.width;
                if(score>highScore){
                    highScore=score;
                }
                playSound(collisionSound);
                if (backgroundMusic != null && backgroundMusic.isRunning()) {
                    backgroundMusic.stop(); 
                }
                return;
            }

        }

        if (bird.y > frameHeight - 100 || bird.y < 0) {
            gameOver = true;
            if(score>highScore){
                highScore=score;
            }
            playSound(collisionSound);
            if (backgroundMusic != null && backgroundMusic.isRunning()) {
                backgroundMusic.stop(); 
            }
        }

    }

    
    private void updateScore() {
        for (Rectangle pipe : pipes) {
            if (pipe.y == 0 && pipe.x + pipe.width == bird.x) {
                score++;
                if (score % 10 == 0 && score != lastScoreCheck) {
                pipeSpeed += 3 ;
                if (pipeGap > 120) pipeGap -= 10; 
                lastScoreCheck = score;
            }
                
            }
        }
    }
    private void spawnCoin() {
        int coinSize = 30;
        int x = frameWidth;

        
        for (int i = 0; i < pipes.size(); i += 2) {
            Rectangle lower = pipes.get(i);
            Rectangle upper = pipes.get(i + 1);
            if (lower.x > frameWidth) {
                int centerY = upper.y + upper.height + (lower.y - (upper.y + upper.height)) / 2 - coinSize / 2;
                coin = new Rectangle(lower.x + pipeWidth / 2 - coinSize / 2, centerY, coinSize, coinSize);
                coinVisible = true;
                coinCollected = false;
                break;
            }
        }
    }

    public void jump() {
        if (!gameOver) {
            if (velocity > 0) {
                velocity = 0;

            }
            velocity -= 10;
        } else {
            restartGame();
        }
    }

    private void restartGame() {
        bird = new Rectangle(birdX, birdY, birdWidth, birdHeight);
        pipes.clear();
        velocity = 0;
        score = 0;
        gameOver = false;
        coinVisible = false;
        coinCollected = false;
        starOffsetX = 0;
        starAngle = 0;
        nextCoinScore=5;
        totalCoinsCollected = 0;
        currentBackgroundIndex=0;
        lastBackgroundChange=System.currentTimeMillis();
        totalDistance = 0;
        startTime = System.currentTimeMillis();
        elapsedTime =0;
        addPipe(true);
        addPipe(true);
        addPipe(true);
        addPipe(true);
        if (backgroundMusic != null) {
            backgroundMusic.setFramePosition(0);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY); 
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            jump();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird - Aditya Edition");
            FlappyBird game = new FlappyBird();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
        });
    }

    public void extraMethodsToPadCode() {
        for (int i = 0; i < 450; i++) {
            dummyMethod(i);
        }
    }

    public void dummyMethod(int index) {
        System.out.print("");
    }
}
