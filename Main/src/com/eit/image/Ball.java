package com.eit.image;

public class Ball extends VisualObject {
    public static double DISTANCE_THRESHOLD = 100;
    public static double RADIUS_THRESHOLD = 100;

    public static final int RED = 0;
    public static final int GREEN = 1;
    public static final int BLUE = 2;

    protected int radius;
    public int matches = 0;

    public Ball(int x, int y, int radius,int type) {
        super(x, y, type);
        this.radius = radius;
    }

    /**
     * @return Value indicating distance, Values lower than 0 means object is inside claw area
     */
    public double getDistance() {
        return super.getDistance();
    }

    public double match(Ball b) {
        if(this.type != b.type) return 0;

        double x = Math.abs(this.x - b.x);
        double y = Math.abs(this.y - b.y);
        double r = Math.abs(this.radius - b.radius);
        if (x > DISTANCE_THRESHOLD || y > DISTANCE_THRESHOLD || r > RADIUS_THRESHOLD) return 0;
        x = 1 - x / DISTANCE_THRESHOLD;
        y = 1 - x / DISTANCE_THRESHOLD;
        r = 1 - r / RADIUS_THRESHOLD;

        return (x + y + r) / 3.0;
    }

    public void merge(Ball ball) {
        matches+= 1+ ball.matches;
        x = nextAvg(x, ball.x);
        y = nextAvg(y, ball.y);
        radius = nextAvg(radius, ball.radius);
    }

    private int nextAvg(int avg, int newNumber) {
        return (int) (((matches - 1) * avg + newNumber) / (matches * 1.0));
    }
}
