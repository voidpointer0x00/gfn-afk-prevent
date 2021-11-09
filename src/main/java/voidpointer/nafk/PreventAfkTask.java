/*
 * Copyright (C) 2021 Vasiliy Petukhov <void.pointer@ya.ru>
 */

package voidpointer.nafk;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

public final class PreventAfkTask implements Runnable {
    /*
     * The direction of movement on a screen is reversed, so
     *  we go from 360 to 270 deg and reflect coordinates to other
     *  quarters to generate the full circle. The explanation is below,
     *  but it's hard to imagine without actually writing+visualising it.
     *
     * Mouse should go in circle starting from 0 rad to 2 PI rad;
     *  as screen's coordinate is "half-reversed" (top left corner [0;0]
     *  and y and the value of the variable increases with moving
     *  down the coordinate system) the angle 360 deg. is located
     *  at habitual right-mid location (0 rad), while 270 goes up
     *  instead of down (PI/2 rad instead of PI 3/2 rad)
     * */
    private static final int GEN_START_ANGLE = 360;
    private static final int GEN_END_ANGLE = 270;
    private static final int CIRCLE_RADIUS = 150;

    private final Robot robot;
    private final GraphicsDevice screen;
    private final Random randomDelay;
    private LinkedList<Point> circle;

    private boolean isStopped = false;

    public PreventAfkTask(final Robot robot) {
        this.robot = robot;
        randomDelay = new Random();
        screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        generateCircle();
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void start() {
        isStopped = false;
    }

    public void stop() {
        isStopped = true;
    }

    @Override public void run() {
        if (!isStopped)
            smoothlyMoveMouseInCircle();
    }

    private void generateCircle() {
        int midX = screen.getDisplayMode().getWidth() >> 1;
        int midY = screen.getDisplayMode().getHeight() >> 1;

        /* different data types bc of insertion order: in second and
         * forth quarters insertion order is reversed because of
         * reflection, so I use stack to make the order sequential */
        Queue<Point> firstQuarter = new LinkedList<>();
        Stack<Point> secondQuarter = new Stack<>();
        Queue<Point> thirdQuarter = new LinkedList<>();
        Stack<Point> forthQuarter = new Stack<>();

        int x, y;
        for (double angle = GEN_START_ANGLE; angle >= GEN_END_ANGLE; angle--) {
            /* x = x0 + r * cos(φ); y = y0 + r * sin(φ)
             * (x0, y0) - circle center, φ - angle. */
            x = (int) Math.round(midX + CIRCLE_RADIUS * Math.cos(Math.toRadians(angle)));
            y = (int) Math.round(midY + CIRCLE_RADIUS * Math.sin(Math.toRadians(angle)));
            firstQuarter.add(new Point(x, y));
            /* reflect point about axis' to other circle's quarters */
            secondQuarter.add(new Point(midX + (midX-x), y));
            thirdQuarter.add(new Point(midX + (midX-x), midY + (midY-y)));
            forthQuarter.add(new Point(x, midY + (midY-y)));
        }

        /* merge quarters into one circle */
        circle = new LinkedList<>(firstQuarter);
        while (!secondQuarter.empty())
            circle.add(secondQuarter.pop());
        circle.addAll(thirdQuarter);
        while (!forthQuarter.empty())
            circle.add(forthQuarter.pop());
    }

    private void smoothlyMoveMouseInCircle() {
        Point screenCenter = new Point(screen.getDisplayMode().getWidth() >> 1,
                                    screen.getDisplayMode().getHeight() >> 1);
        robot.mousePress(InputEvent.getMaskForButton(MouseEvent.BUTTON2));
        moveMouseToPoint(screenCenter);
        moveMouseToPoint(circle.getFirst());
        for (Point p : circle) {
            if (isStopped)
                break;
            robot.mouseMove(p.x, p.y);
            robot.delay(randomDelay.nextInt(5) + 1);
        }
        if (!isStopped)
            moveMouseToPoint(screenCenter);
        robot.mouseRelease(InputEvent.getMaskForButton(MouseEvent.BUTTON2));
    }

    private void moveMouseToPoint(final Point endPoint) {
        Point mousePosition = MouseInfo.getPointerInfo().getLocation();

        /* cursor should move on the longest path for smoothness
         * (otherwise it will be just instantly moved) */
        int dx = Math.abs(endPoint.x - mousePosition.x);
        int dy = Math.abs(endPoint.y - mousePosition.y);

        robot.setAutoDelay(1);
        if (dx > dy) {
            moveMouseByX(robot, mousePosition, endPoint);
        } else {
            moveMouseByY(robot, mousePosition, endPoint);
        }
    }

    private void moveMouseByX(final Robot robot, final Point p1, final Point p2) {
        int x = p1.x;
        do {
            robot.mouseMove(x, getLinearYForX(x, p1, p2));
            if (x < p2.x)
                x++;
            else
                x--;
        } while (x != p2.x);
    }

    private void moveMouseByY(final Robot robot, final Point p1, final Point p2) {
        int y = p1.y;
        do {
            robot.mouseMove(getLinearXForY(y, p1, p2), y);
            if (y < p2.y)
                y++;
            else
                y--;
        } while (y != p2.y);
    }

    private int getLinearYForX(final int x, final Point p1, final Point p2) {
        /* y = y1 + (x-x1) * (y2-y1) / (x2-x1) */
        return p1.y + (x - p1.x) * (p2.y - p1.y) / (p2.x - p1.x);
    }

    private int getLinearXForY(final int y, final Point p1, final Point p2) {
        /* x = x1 + (y-y1) * (x2-x1) / (y2-y1) */
        return p1.x + (y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y);
    }
}
