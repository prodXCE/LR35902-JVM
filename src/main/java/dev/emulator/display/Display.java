package dev.emulator.display;

import dev.emulator.hardware.Joypad;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Display extends JPanel {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 144;

    private final BufferedImage image;
    private final int[] pixels;
    private final JFrame frame;
    private Joypad joypad;

    public Display() {
        this.image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        frame = new JFrame("JavaBoy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        this.setPreferredSize(new Dimension(WIDTH * 3, HEIGHT * 3));

        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (joypad == null) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT: joypad.keyPressed(0); break;
                    case KeyEvent.VK_LEFT:  joypad.keyPressed(1); break;
                    case KeyEvent.VK_UP:    joypad.keyPressed(2); break;
                    case KeyEvent.VK_DOWN:  joypad.keyPressed(3); break;
                    case KeyEvent.VK_Z:     joypad.keyPressed(4); break;
                    case KeyEvent.VK_X:     joypad.keyPressed(5); break;
                    case KeyEvent.VK_SHIFT: joypad.keyPressed(6); break;
                    case KeyEvent.VK_ENTER: joypad.keyPressed(7); break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (joypad == null) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT: joypad.keyReleased(0); break;
                    case KeyEvent.VK_LEFT:  joypad.keyReleased(1); break;
                    case KeyEvent.VK_UP:    joypad.keyReleased(2); break;
                    case KeyEvent.VK_DOWN:  joypad.keyReleased(3); break;
                    case KeyEvent.VK_Z:     joypad.keyReleased(4); break;
                    case KeyEvent.VK_X:     joypad.keyReleased(5); break;
                    case KeyEvent.VK_SHIFT: joypad.keyReleased(6); break;
                    case KeyEvent.VK_ENTER: joypad.keyReleased(7); break;
                }
            }
        });
        frame.setFocusable(true);
        frame.requestFocus();
    }

    public void setJoypad(Joypad joypad) {
        this.joypad = joypad;
    }

    public void refreshFrame(int[] newPixels) {
        System.arraycopy(newPixels, 0, this.pixels, 0, newPixels.length);
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, WIDTH * 3, HEIGHT * 3, null);
    }
}
