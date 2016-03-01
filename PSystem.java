
/**
 *PSystem
 *This class runs a planetary system.
 *
 *Version 0.2 (Fixed a memory leak, improved integradion, changed the trails)
 *If I were to upgrade this further, I'd make it work with Java.util.concurrent to avoid all the ConcurrentModificationExceptions
 *that show up. 
 *
 *The CMEs don't break the program though (most of the time), so it's fine.
 **/
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.event.*;
public class PSystem extends JPanel
{
    private ArrayList<Body> allBodies;
    private ArrayList<Body> movableBodies;
    private ArrayList<Body> fakeBodies;
    private ArrayList<Trail> traceLines;
    static boolean showTrails = true;    //right now only used for debugging but can be implemented as a checkbox or something
    final static int INTERVAL = 10; //time (in ms) between update calls
    final static int TRAILSIZE = 2048; //length of trail; should reduce memory leaking
    JFrame frame;
    Graphics2D g;

    boolean orbiting = false, fixed = false;

    static double mass = 1E3;
    static double G = 1;
    static int speed = 1;
    static Color color = Color.RED;

    double scale = 1;
    int movedX = 0;
    int movedY = 0;
    Point2D xCenter = new Point2D.Double(0, 0);
    AffineTransform at = new AffineTransform();
    public PSystem() {
        allBodies = new ArrayList<Body>();
        movableBodies = new ArrayList<Body>();
        fakeBodies = new ArrayList<Body>();
        traceLines = new ArrayList<Trail>();


        MouseAdapter ma = new MouseAdapter() { //click and drag to place new Body with a starting velocity
                Body b;
                boolean hasBeenClicked = false;
                Point2D p; 
                public void mouseExited(MouseEvent e){
                    hasBeenClicked = false;
                    fakeBodies.remove(b);
                }

                public void mouseEntered(MouseEvent e) {
                    requestFocus();
                    hasBeenClicked = false; 
                }

                public void mousePressed(MouseEvent e) {
                    if(hasBeenClicked) //stops left clicking and then right clicking without releasing left click from breaking stuff
                        return;
                    if (!e.isShiftDown()) {
                        b = new Body(0, (int)Math.log(mass), (e.getX() + movedX) / scale, (e.getY() + movedY) / scale, 0, 0, color);

                        fakeBodies.add(b);
                        hasBeenClicked = true;
                    }
                    p = e.getPoint();
                }

                public void mouseDragged(MouseEvent e) {
                    if (e.isShiftDown()) { //click and drag to move viewport
                        movedX += -e.getX() + p.getX();
                        movedY += -e.getY() + p.getY();
                        p = e.getPoint();
                    }
                    
                }

                public void mouseClicked(MouseEvent e) {
                    requestFocus();
                }

                public void mouseReleased(MouseEvent e) {
                    if(hasBeenClicked)
                    {
                        b.setMass(mass);

                        if(!fixed) {
                            addBody(b);
                            if(!orbiting)
                                b.setVel(e.getX()-p.getX(), e.getY() - p.getY());
                            else {
                                Body c = b.findHighestGravity(allBodies);
                                double dx = b.getX() - c.getX();
                                double dy = b.getY() - c.getY();
                                
                                double v0 = Math.sqrt(G * c.getMass()/Math.sqrt(dx * dx + dy * dy));
                                double theta = Math.atan2(dy,dx); //getting the tangent line to circle
                                if(Math.random() < 0.5) {
                                    b.setVel(Math.sin(-theta)*v0 + c.getVX(), Math.cos(theta) * v0 + c.getVY());
                                }
                                else {
                                    b.setVel(-Math.sin(-theta)*v0 + c.getVX(), -Math.cos(theta) * v0 + c.getVY());
                                }
                            }
                        }
                        else
                            addStationaryBody(b);
                        fakeBodies.remove(b);
                    }
                    b = null;
                    hasBeenClicked = false;
                }

                public void mouseMoved(MouseEvent e) {}

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    //scale += e.getWheelRotation() * -0.01;
                    xCenter = e.getPoint();
                    //movedX += e.getX() * scale - e.getX();
                    //movedY += e.getY() * scale - e.getY();
                    repaint();
                }

            };

        addMouseMotionListener(ma);
        addMouseListener(ma);
        addMouseWheelListener(ma);
        setSimpleSystem();
    }

    public static void main(String[] args)  {

        PSystem system = new PSystem();
        JOptionPane.showMessageDialog(system.frame, "This is a work in progress.\n Click to create a body. Click and drag to create"
            +" a body with a starting velocity.\n Leapfrog integration.  Radius is log(mass) * 2. \n Java runs out of heap space if"
            +" it runs for more than a couple minutes. \n Shift+click and drag to pan.");
        system.initUI();

        while(true)
        {
            system.move();
            system.checkForCollisions();
            system.repaint();
            try {
                Thread.sleep(INTERVAL);
            }
            catch(InterruptedException e) 
            {
                System.out.println(e);
            }

        }    

    }

    public void initUI() {
        JFrame frame = new JFrame("PSystem");
        frame.setResizable(true);
        this.setPreferredSize(new Dimension(600, 600));
        this.setFocusable(true);
        frame.setSize(800, 800);

        JPanel controls = new JPanel();
        controls.setMaximumSize(new Dimension(800, 100));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel jLabel1 = new JLabel();
        jLabel1.setText("Mass: ");
        final JTextField textField1 = new JTextField("1E3", 6);
        textField1.addFocusListener( new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {
                    String text = textField1.getText();
                    mass = Double.parseDouble(text);
                }

                @Override
                public void focusGained(FocusEvent e) {
                }
            });

        JLabel jLabel2 = new JLabel();
        jLabel2.setText("Universal Gravitational Constant: ");
        final JTextField textField2 = new JTextField("1", 6);
        textField2.addFocusListener( new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {
                    String text = textField2.getText();
                    G = Double.parseDouble(text);
                }

                @Override
                public void focusGained(FocusEvent e) {
                }
            });

        JLabel jLabel3 = new JLabel();
        jLabel3.setText("Color: ");
        JComboBox colors = makeComboBox();

        JRadioButton fixedButton = new JRadioButton("Fixed");
        fixedButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    orbiting = false; 
                    fixed = true;
                }
            });
        JRadioButton orbitButton = new JRadioButton("Orbiting");
        orbitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    orbiting = true; 
                    fixed = false;
                }
            });
        JRadioButton freeButton = new JRadioButton("Free", true);
        freeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    orbiting = false;
                    fixed = false;
                }
            });
        ButtonGroup bg = new ButtonGroup();
        bg.add(fixedButton);
        bg.add(orbitButton);
        bg.add(freeButton);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
        buttonPanel.add(fixedButton);
        buttonPanel.add(orbitButton);
        buttonPanel.add(freeButton);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
                @Override 
                public void actionPerformed(ActionEvent e) {
                    reset();
                }
            });

        JButton resetZoomButton = new JButton("Fix Zoom");
        resetZoomButton.addActionListener(new ActionListener() {
                @Override 
                public void actionPerformed(ActionEvent e) {
                    movedX = 0;
                    movedY = 0;
                    scale = 1;
                }
            });

        JButton preset1 = new JButton("Simple System");
        preset1.addActionListener(new ActionListener() {
                @Override 
                public void actionPerformed(ActionEvent e) {
                    reset();
                    setSimpleSystem();
                }
            });

        JButton preset2 = new JButton("Binary System");
        preset2.addActionListener(new ActionListener() {
                @Override 
                public void actionPerformed(ActionEvent e) {
                    reset();
                    setBinarySystem();
                }
            });

        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));
        panel1.add(jLabel1);
        panel1.add(textField1);

        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
        panel2.add(jLabel2);
        panel2.add(textField2);

        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, BoxLayout.LINE_AXIS));
        panel3.add(jLabel3);
        panel3.add(colors);

        JPanel panel4 = new JPanel();
        panel4.setLayout(new BoxLayout(panel4, BoxLayout.PAGE_AXIS));
        panel4.add(panel1);
        panel4.add(panel2);

        JPanel panel5 = new JPanel();
        panel5.setLayout(new BoxLayout(panel5, BoxLayout.PAGE_AXIS));
        resetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, resetButton.getPreferredSize().height));
        resetZoomButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, resetButton.getPreferredSize().height));
        panel5.add(resetButton);
        panel5.add(resetZoomButton);

        JPanel panel6 = new JPanel();
        panel6.setLayout(new BoxLayout(panel6, BoxLayout.PAGE_AXIS));
        panel6.add(preset1);
        panel6.add(preset2);

        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(panel4);
        controls.add(panel3);
        controls.add(buttonPanel);
        controls.add(panel5);
        controls.add(panel6);

        Container cPane = frame.getContentPane();
        cPane.setLayout(new BoxLayout(cPane, BoxLayout.PAGE_AXIS));

       
        frame.getContentPane().add(this, BorderLayout.NORTH);
        frame.getContentPane().add(controls);
        
        setOpaque(true);
        setBackground(Color.BLACK);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                
        frame.setVisible(true);
    }    

    public void move() {//uses leapfrog (Verlet) integration.  Eventually should change to Runge-Kutta, 
        //but I don't know how to implement that without just copying from somewhere.
        for(Body a : movableBodies.toArray(new Body[movableBodies.size()])) {
            double accX = 0;
            double accY = 0;
            for(Body b : allBodies.toArray(new Body[allBodies.size()])) {
                if(a == b) 
                    continue;
                double dx = b.getX() - a.getX();
                double dy = b.getY() - a.getY();
                double acc0 = G * b.getMass() / (dx * dx + dy * dy);
                double theta = Math.atan2(dy, dx);
                accX += acc0 * Math.cos(theta);
                accY += acc0 * Math.sin(theta);
            }
            double timestep = PSystem.INTERVAL/1000.0 * PSystem.speed;
            a.posX += timestep * (a.velX + timestep * accX / 2); // x = x + vt + at^2
            a.posY += timestep * (a.velY + timestep * accY / 2);
            double newAccX = 0;
            double newAccY = 0;
            for(Body b : allBodies.toArray(new Body[allBodies.size()])) {
                if(a == b) 
                    continue;
                double dx = b.getX() - a.getX();
                double dy = b.getY() - a.getY();
                double acc0 = G * b.getMass() / (dx * dx + dy * dy);
                double theta = Math.atan2(dy, dx);
                accX += acc0 * Math.cos(theta);
                accY += acc0 * Math.sin(theta);
            }
            a.velX += timestep * (newAccX + accX)/2;
            a.velY += timestep * (newAccY + accY)/2;

            if(PSystem.showTrails)
                a.addPoint(new Point2D.Double(a.posX, a.posY));
        }

    }

    public void checkForCollisions() {
        boolean remove = false;
        ArrayList<Body> del = new ArrayList<Body>();
        ArrayList<Body> toAdd = new ArrayList<Body>();
        ArrayList<Body> toAddMovable = new ArrayList<Body>();
        for(int i = 0; i < allBodies.size(); i++) {
            Body a = allBodies.get(i);
            for(int j = i+1; j < allBodies.size(); j++) {
                Body b = allBodies.get(j);
                if(a != b && a.pointOfCollision(b) != null) {
                    Point2D p = a.pointOfCollision(b);

                    double newVX = (a.getVX() * a.getMass() + b.getVX() * b.getMass()) / (a.getMass() + b.getMass());
                    double newVY = (a.getVY() * a.getMass() + b.getVY() * b.getMass()) / (a.getMass() + b.getMass());
                    double newMass = a.getMass() + b.getMass();

                    Color averageColor = new Color((int)(Math.sqrt((Math.pow(a.getColor().getRed(), 2)*a.getMass() + Math.pow(b.getColor().getRed(), 2)*b.getMass())/newMass)), 
                            (int)(Math.sqrt((Math.pow(a.getColor().getGreen(), 2)*a.getMass() + Math.pow(b.getColor().getGreen(), 2)*b.getMass())/newMass)), 
                            (int)(Math.sqrt((Math.pow(a.getColor().getBlue(), 2)*a.getMass() + Math.pow(b.getColor().getBlue(), 2)*b.getMass())/newMass)));
                    //https://www.youtube.com/watch?v=LKnqECcg6Gw

                    //eventually I should change to HSV colorspace but that's out of my area of expertise

                    if(movableBodies.indexOf(a) > -1 && movableBodies.indexOf(b) > -1){
                        Body c = new Body(a.getMass()+b.getMass(), p, newVX, 
                                newVY, averageColor);
                        toAddMovable.add(c);
                        toAdd.add(c);
                    }
                    else if(movableBodies.indexOf(a) > -1 && movableBodies.indexOf(b) <= -1)
                    {
                        Body c = new Body(a.getMass()+b.getMass(), b.getCenter(), newVX, 
                                newVY, averageColor);
                        toAdd.add(c);
                    }
                    else {
                        Body c = new Body(a.getMass()+b.getMass(), a.getCenter(), newVX, 
                                newVY, averageColor);
                        toAdd.add(c);
                    }
                    del.add(a);
                    del.add(b);
                }
            }
        }

        allBodies.addAll(toAdd); //do all this afterwards to avoid ConcurrentModificationError
        movableBodies.addAll(toAddMovable);
        allBodies.removeAll(del);
        movableBodies.removeAll(del);
    }

    @Override
    public void paintComponent(Graphics g) {  //this is weird and confusing and I mostly copied it from the Java tutorials
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        AffineTransform saveXform = g2d.getTransform();
        AffineTransform at = new AffineTransform();

        at.translate(-movedX, -movedY);
        //at.scale(scale, scale);
        //at.translate(-(xCenter.getX() + movedX - xCenter.getX() / scale),-(xCenter.getY() + movedY - xCenter.getX() / scale));
        g2d.transform(at);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setBackground(Color.BLACK);
        if(showTrails) {
            ArrayList<Trail> del = new ArrayList<Trail>();
            for(Trail l : traceLines)
            {
                l.paint(g2d);
                l.age();
                if(l.getAge() > TRAILSIZE)
                    del.add(l);
            }
            traceLines.removeAll(del);

            for(Body b : fakeBodies)
                b.paint(g2d);

        }
        for(Body b : allBodies.toArray(new Body[allBodies.size()]))
        {
            b.paint(g2d);
            if(b.getPoints().size() > 2) {
                Trail trace = new Trail(b.getPoints().get(b.getPoints().size()-2), b.getPoints().get(b.getPoints().size()-1), b.getColor());
                traceLines.add(trace);
            }
        }

        xCenter = new Point2D.Double(0, 0);

        g2d.setTransform(saveXform);
        at.setToIdentity();
    }

    public void setSimpleSystem(){
        addStationaryBody(new Body(1E6, 300, 300, 0, 0, Color.YELLOW));
        addBody(new Body(1E5,500, 300, 0, 70, Color.BLUE));
        //addBody(new Body(1E3, 620, 425, -27, 100, Color.WHITE));
    }

    public void setBinarySystem() {
        addBody(new Body(1E6, 10, 300, 400, 0, 50, Color.RED));
        addBody(new Body(1E6, 10, 500, 400, 0, -50, Color.BLUE));
    }

    public void addBody(Body b){
        allBodies.add(b);
        movableBodies.add(b);
    }    

    public void addStationaryBody(Body b) {
        allBodies.add(b);
    }

    public void reset() {
        scale = 1;
        movedX = 0;
        movedY = 0;
        xCenter = new Point2D.Double(0, 0);

        ArrayList<Body> oldBodies = allBodies;
        allBodies = new ArrayList<Body>();
        movableBodies = new ArrayList<Body>();
        traceLines = new ArrayList<Trail>();

        for (Body b : oldBodies) 
        {
            for (Point2D p : b.tracePoints)
            {
                p = null;
            }
            b = null;
        }

    }

    private JComboBox makeComboBox() {
        Integer[] intarray = {0, 1, 2, 3, 4, 5, 6, 7, 8};

        String[] colorstrings = {"Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Magenta", "White", "Black"};
        JComboBox returnbox = new JComboBox(intarray);
        ComboBoxRenderer renderer = new ComboBoxRenderer();
        returnbox.setRenderer(renderer);
        returnbox.setSelectedIndex(0);
        returnbox.addActionListener( new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JComboBox cb = (JComboBox)e.getSource();
                    Color[] colorarray = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, 
                            Color.WHITE, Color.BLACK};
                    color = colorarray[((Integer)cb.getSelectedItem()).intValue()];
                }
            });

        return returnbox;
    }
}
class ComboBoxRenderer extends JLabel implements ListCellRenderer{
    public ComboBoxRenderer() {
        setOpaque(true);
        setVerticalAlignment(CENTER);   
    }

    public Component getListCellRendererComponent( // I'm really not sure how this works; again, copied from Java official tutorials
    JList list,
    Object value,
    int index,
    boolean isSelected,
    boolean cellHasFocus) {
        int selectedIndex = ((Integer)value).intValue();

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        //i have no idea what this does, I just copied from the java tutorials
        Color[] colorarray = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, 
                Color.WHITE, Color.BLACK};
        String[] colorstrings = {"Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Magenta", "White", "Black"};
        Color selectedColor = colorarray[selectedIndex];
        String name = colorstrings[selectedIndex];

        setText(name);
        setBackground(selectedColor);

        return this;
    }
}

