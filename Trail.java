
/**
 * Write a description of class Trail here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
import java.awt.geom.*;
import java.awt.*;
public class Trail extends Line2D.Double //woohoo mini-class; just there to keep track of color and age
{
    private int age;
    private Color color;
    public Trail(Point2D start, Point2D end, Color c) {
        super(start, end);
        color = c;
        age = 0;
    }
    public int getAge() {
        return age;
    }
    public void age() {
        age++;
    }
    public void paint(Graphics2D g2d) { //I figured this would be easier here than in the main PSystem class
        g2d.setColor(color);
        g2d.draw(this);
    }
}
