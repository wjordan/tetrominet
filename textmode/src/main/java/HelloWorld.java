/*
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
*/

import charva.awt.*;
import charva.awt.event.ActionEvent;
import charva.awt.event.KeyAdapter;
import charva.awt.event.KeyEvent;
import charva.awt.event.KeyListener;
import charvax.swing.JFrame;
import charvax.swing.JLabel;
import charvax.swing.JPanel;

/**
 * Title:        GUI Example
 * Description:  An example to demonstrate the portability of the Charva library.
 * Copyright:    Copyright (c) 2001
 * Company:      Pitman Computer Consulting
 * @author
 * @version 1.0
 */

public class HelloWorld extends JFrame {
    JPanel contentPane;
    JPanel centerPanel = new JPanel();
    static HelloWorld appFrame1;
    JLabel hello = new JLabel("Hello world!");
    final Tetrion tetrion = new Tetrion();

    public HelloWorld() {
        super();
        try {
            super._insets = new Insets(0,0,0,0);
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("charva.color","");
        final Toolkit tk = Toolkit.getDefaultToolkit();
        appFrame1 = new HelloWorld();
        appFrame1.pack();
        appFrame1.setVisible(true);
    }

    private void jbInit() throws Exception {
        this.setTitle("CHARVA Example");
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                this_keyPressed(e);
            }
        });
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.red);
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setVisible(true);
        centerPanel.setBounds(5,25,0,0);
        centerPanel.add(hello);
        contentPane.add(centerPanel, BorderLayout.CENTER);
        final Dimension zero = new Dimension(0,0);
        tetrion.setBackground(Color.red);

        contentPane.add(tetrion);
        tetrion.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent event) {
                hello.setText("PRESSED KEY="+event.getKeyCode()+ "i="+ (++i));
                this_keyPressed(event);
            }

            public void keyTyped(KeyEvent event) {
            }

            public void keyReleased(KeyEvent event) {
                hello.setText("released key="+event.getKeyCode() + "i="+ (++i));
            }
        });
    }

    class Block extends Component {
        Point point;
        int value = getCursesColor();
        public Block(Point p) {
            point = p;
        }

        public void draw() {
            final Toolkit tk = Toolkit.getDefaultToolkit();
            tk.setCursor(point);
            int colorpair = value;
            tk.addChar('[',0,colorpair);
            tk.addChar(']',0,colorpair);
        }

        public Dimension getSize() { return new Dimension(2,1); }
        public int getWidth() { return 2; }
        public int getHeight() { return 1; }
        public Dimension minimumSize() { return new Dimension(2,1); }

        public void debug(int i) {}
    }

    class Tetrion extends Container {
        public Tetrion() {
            for(int y=0;y<20;y++) {
                for(int x=0;x<10;x++) {
                    array[y][x] = new Block(new Point(x*2,y));
                    add(array[y][x]);
                }
            }
        }

        private Block[][] array = new Block[20][10];

        public void put(Point p) {
            try {
                array[p.y][p.x].value = Toolkit.getDefaultToolkit().getColorPairIndex(new ColorPair(Color.red, Color.green));
            } catch (TerminfoCapabilityException e) {
                e.printStackTrace();
            }
        }

        public Dimension getSize() { return new Dimension(40, 40); }
        public int getWidth() { return 20; }
        public int getHeight() { return 20; }
        public Dimension minimumSize() { return new Dimension(40, 40); }

        public void debug(int i) {}
    }

    int i = 0;

    void exitMenuItem_actionPerformed(ActionEvent e) {
        terminate();
    }

    void this_keyPressed(KeyEvent e) {
//        hello.setText("Goodbye world!");
        int keycode = e.getKeyCode();
        if (keycode == 'q' || keycode == KeyEvent.VK_ENTER)
            terminate();
        if (keycode == 't') tetrion.put(new Point(5,5));
            
    }

    void cancelButton_actionPerformed(ActionEvent e) {
        terminate();
    }

    private void terminate() {
        System.exit(0);
    }
}
