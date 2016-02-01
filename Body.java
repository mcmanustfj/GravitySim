
/**
 * 
 */
import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.LinkedList;
import java.util.List;
import java.awt.Color;
public class Body implements Comparable<Body>
{
    private double mass;
    private int radius;
    public double posX;
    public double posY;
    public double velX;
    public double velY;
    public double accX = 0, accY = 0;
    private Color color;
    public LinkedList<Point2D> tracePoints;
    public Body(double m, int r, double x, double y) {
        mass = m;
        radius = r;
        posX = x;
        posY = y;
        //TESTING
        java.lang.System.out.println(m);
        System.out.println(r);
        System.out.println(x);
        System.out.println(y);
        tracePoints = new LinkedList<Point2D>();
    }

    public Body(double m, int r, double x, double y, double vx, double vy) {
        mass = m;
        radius = r;
        posX = x;
        posY = y;
        velX = vx;
        velY = vy;
        tracePoints = new LinkedList<Point2D>();

        //         System.out.println(m);
        //         System.out.println(r);
        //         System.out.println(x);
        //         System.out.println(y);
        //         System.out.println(vx);
        //         System.out.println(vy);
    }

    public Body(double m, int r, double x, double y, double vx, double vy, Color c) {
        mass = m;
        radius = r;
        posX = x;
        posY = y;
        velX = vx;
        velY = vy;
        color = c;
        tracePoints = new LinkedList<Point2D>();
    }

    public Body(double m, Point2D p, double vx, double vy, Color c) {
        mass = m;
        radius = (int)Math.log(m);
        posX = p.getX();
        posY = p.getY();
        velX = vx;
        velY = vy;
        color = c;
        tracePoints = new LinkedList<Point2D>();
    }

    public Body(double m, double x, double y, double vx, double vy, Color c) {
        mass = m;
        radius = (int)Math.log(m);
        posX = x;
        posY = y;
        velX = vx;
        velY = vy;
        color = c;
        tracePoints = new LinkedList<Point2D>();
    }
    public void addPoint(Point2D p)
    {
        if(tracePoints.size() >= PSystem.TRAILSIZE)
            tracePoints.remove();
        tracePoints.add(p);
    }

    public String toString()
    {
        return mass + "\n" +
        posX + "\n" +
        posY + "\n" +
        velX + "\n" +
        velY + "\n";
    }

    public double getMass() {
        return mass;
    }

    public double getX() {
        return posX;
    }

    public double getY() {
        return posY;
    }

    public double getR() {
        return radius;
    }

    public Color getColor() {
        return color;
    }

    public void setMass(double m) {
        mass = m;
    }

    public double getVX() {
        return velX;
    }

    public double getVY() {
        return velY;
    }

    public void setVel(double vx, double vy) {
        velX = vx;
        velY = vy;
    }

    public void paint(Graphics2D g)
    {
        int x = (int)(posX-radius);
        int y = (int)(posY-radius);
        g.setColor(color);
        g.fillOval(x,y,radius * 2,radius * 2);

    }

    public List<Point2D> getPoints() {
        return tracePoints;
    }

    public Point2D pointOfCollision(Body b) {
        if((Math.pow(posX - b.getX(), 2) + Math.pow(posY -  b.getY(), 2))
        > Math.pow(radius + b.getR(), 2))
            return null;
        else
            return new Point2D.Double((posX * b.getR() + b.getX() * radius)/(radius + b.getR()),
                (posY * b.getR() + b.getY() * radius)/(radius + b.getR()));

    }
    
    public Point2D getCenter() {
        return new Point2D.Double(posX, posY);
    }

    public int compareTo(Body b) {
        return (int)(mass-b.mass);
    }
}
