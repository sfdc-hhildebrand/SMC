//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2003 Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// Name
//  TrafficCanvas
//
// Description
//  Responsible for coordinating the stoplights and vehicles.
//
// RCS ID
// $Id: TrafficCanvas.java,v 1.8 2009/03/27 09:41:46 cwrapp Exp $
//
// CHANGE LOG
// $Log: TrafficCanvas.java,v $
// Revision 1.8  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.7  2009/03/01 18:20:38  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.6  2007/02/21 13:38:38  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:04:00  charlesr
// Initial revision
//

package example_4;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class TrafficCanvas extends Canvas {
    //---------------------------------------------------------------
    // Member methods.
    //

    // Nested classes - implement timer listeners.
    private final class NewVehicleTimeoutListener implements ActionListener {
        private TrafficCanvas _owner;

        public NewVehicleTimeoutListener(TrafficCanvas owner) {
            _owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            _owner.handleNewVehicleTimeout();
            return;
        }
    }

    private final class RepaintTimeoutListener implements ActionListener {
        private TrafficCanvas _owner;

        public RepaintTimeoutListener(TrafficCanvas owner) {
            _owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            _owner.handleRepaintTimeout();
            return;
        }
    }

    /**
     * 
     */
    private static final long        serialVersionUID          = 1L;

    //----------------------------------------
    // DYNANMIC DATA
    //
    // The stop light at the center of it all.
    private Stoplight                _stopLight;

    // List of all existing vehicles.
    private List<Vehicle>            _vehicleList;

    // List of all defunct vehicles.
    private List<Vehicle>            _removeList;

    // Timer data.
    private int                      _newVehicleTimerDuration;

    private long                     _nextNewVehicleTimeout;

    private javax.swing.Timer        _newVehicleTimer;

    private javax.swing.Timer        _repaintTimer;

    // How fast vehicles move.
    private int                      _vehicleSpeed;

    // Set to true when the light has changed and
    // so needs to be repainted.
    private boolean                  _lightChanged;

    // For each light, keep a list of vehicles waiting for that
    // light to turn green. When the light does turn green, tell
    // the vehicles that it is okay to continue.
    private List<Vehicle>[]          _stoplightQueue;

    //----------------------------------------
    // STATIC DATA
    //
    // Directions.
    public static final Point        NORTH;

    public static final Point        EAST;

    public static final Point        SOUTH;

    public static final Point        WEST;

    public static final int          YELLOW_LIGHT              = 0;

    public static final int          EW_LIGHT                  = 1;

    public static final int          NS_LIGHT                  = 2;

    // The canvas dimensions.
    private static final Point       CANVAS_CORNER;

    private static final Dimension   CANVAS_SIZE;

    // The "field" dimensions.
    private static final Point[]     RECT_CORNERS;

    private static final Dimension   FIELD_SIZE;

    // Road dimensions.
    private static final int         ROAD_WIDTH;

    private static final int         LANE_WIDTH;

    private static final int         CURB_OFFSET;

    // Vehicle dimensions.
    private static final Dimension   VEHICLE_SIZE;

    //---------------------------------------------------------------
    // Member data.
    //

    // Minimum distance between vehicles.
    private static final int         VEHICLE_SEPARATION        = 3;

    // Lane markings.
    private static final float       DASH1[]                   = { 10.0f };

    private static final BasicStroke DASHED;

    // Timers.
    private static final int         REPAINT_TIME              = 16;
    private static final long        INITIAL_NEW_VEHICLE_DELAY = 250;
    private static final int         NORTHLIGHT                = 0;
    private static final int         SOUTHLIGHT                = 1;

    private static final int         EASTLIGHT                 = 2;

    private static final int         WESTLIGHT                 = 3;

    static {
        // Speed is built into the direction.
        NORTH = new Point(0, -1);
        SOUTH = new Point(0, 1);
        EAST = new Point(1, 0);
        WEST = new Point(-1, 0);

        CANVAS_CORNER = new Point(0, 0);
        CANVAS_SIZE = new Dimension(250, 250);

        FIELD_SIZE = new Dimension((int) (CANVAS_SIZE.width * 0.4),
                                   (int) (CANVAS_SIZE.height * 0.4));

        ROAD_WIDTH = CANVAS_SIZE.width - FIELD_SIZE.width * 2;
        LANE_WIDTH = ROAD_WIDTH / 2;
        VEHICLE_SIZE = new Dimension(6, 6);
        CURB_OFFSET = (LANE_WIDTH - VEHICLE_SIZE.width) / 2;

        RECT_CORNERS = new Point[5];
        RECT_CORNERS[0] = new Point(0, 0);
        RECT_CORNERS[1] = new Point(FIELD_SIZE.width, FIELD_SIZE.height);
        RECT_CORNERS[2] = new Point((int) (CANVAS_SIZE.width * 0.5),
                                    (int) (CANVAS_SIZE.height * 0.5));
        RECT_CORNERS[3] = new Point(CANVAS_SIZE.width - FIELD_SIZE.width,
                                    CANVAS_SIZE.height - FIELD_SIZE.height);
        RECT_CORNERS[4] = new Point(CANVAS_SIZE.width, CANVAS_SIZE.height);

        DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                                 BasicStroke.JOIN_MITER, 10.0f, DASH1, 0.0f);
    }

    @SuppressWarnings("unchecked")
    public TrafficCanvas() {
        setSize(CANVAS_SIZE.width, CANVAS_SIZE.height);

        // Create the stop light object.
        _stopLight = new Stoplight(CANVAS_SIZE.width, CANVAS_SIZE.height, this);

        // Create an empty vehicle list. Vehicles will be added
        // when the "NewVehicle" timer expires.
        _vehicleList = new LinkedList<Vehicle>();

        _vehicleSpeed = 2;

        // When vehicles move off the canvas, add them to this
        // remove list for later removal.
        _removeList = new LinkedList<Vehicle>();

        _newVehicleTimerDuration = 4000;
        _lightChanged = false;

        _stoplightQueue = new List[4];
        for (int i = 0; i < 4; ++i) {
            _stoplightQueue[i] = new LinkedList<Vehicle>();
        }

        _stopLight.start();
    }

    public synchronized void continueDemo() {
        // Get the timers up and running again.
        startNewVehicleTimer();
        startRepaintTimer();

        _stopLight.continueDemo();

        return;
    }

    public int getDistanceToIntersection(Point location, Point direction) {
        int retval = 0;
        int queueSize;
        int endOfLine;

        if (direction.x == EAST.x && direction.y == EAST.y) {
            queueSize = _stoplightQueue[WESTLIGHT].size();
            endOfLine = RECT_CORNERS[1].x - (queueSize + 1)
                        * VEHICLE_SIZE.width - queueSize * VEHICLE_SEPARATION;
            retval = endOfLine - location.x;
        } else if (direction.x == SOUTH.x && direction.y == SOUTH.y) {
            queueSize = _stoplightQueue[NORTHLIGHT].size();
            endOfLine = RECT_CORNERS[1].y - (queueSize + 1)
                        * VEHICLE_SIZE.height - queueSize * VEHICLE_SEPARATION;
            retval = endOfLine - location.y;
        } else if (direction.x == WEST.x && direction.y == WEST.y) {
            queueSize = _stoplightQueue[EASTLIGHT].size();
            endOfLine = RECT_CORNERS[3].x + queueSize * VEHICLE_SIZE.width
                        + queueSize * VEHICLE_SEPARATION;
            retval = location.x - endOfLine;
        } else if (direction.x == NORTH.x && direction.y == NORTH.y) {
            queueSize = _stoplightQueue[SOUTHLIGHT].size();
            endOfLine = RECT_CORNERS[3].y + queueSize * VEHICLE_SIZE.height
                        + queueSize * VEHICLE_SEPARATION;
            retval = location.y - endOfLine;
        }

        return retval;
    }

    // Get the stoplight's location and size.
    public void getLightDimensions(Point direction, Point lightLocation,
                                   Dimension lightSize) {
        _stopLight.getLightDimensions(direction, lightLocation, lightSize);

        return;
    }

    // Return the current stop light timers.
    public int getLightDuration(int direction) {
        // Convert the duration from milliseconds to seconds.
        return _stopLight.getLightDuration(direction) / 1000;
    }

    public Color getLightsColor(Point direction) {
        Color retval = Color.black;

        if (direction.x == EAST.x && direction.y == EAST.y) {
            retval = _stopLight.getLightsColor(Stoplight.WEST);
        } else if (direction.x == SOUTH.x && direction.y == SOUTH.y) {
            retval = _stopLight.getLightsColor(Stoplight.NORTH);
        } else if (direction.x == WEST.x && direction.y == WEST.y) {
            retval = _stopLight.getLightsColor(Stoplight.EAST);
        } else if (direction.x == NORTH.x && direction.y == NORTH.y) {
            retval = _stopLight.getLightsColor(Stoplight.SOUTH);
        }

        return retval;
    }

    public int getMaxX() {
        return CANVAS_CORNER.x + CANVAS_SIZE.width - VEHICLE_SIZE.width;
    }

    public int getMaxY() {
        return CANVAS_CORNER.y + CANVAS_SIZE.height - VEHICLE_SIZE.height;
    }

    public int getMinX() {
        return CANVAS_CORNER.x;
    }

    public int getMinY() {
        return CANVAS_CORNER.y;
    }

    public int getNewVehicleRate() {
        return _newVehicleTimerDuration / 1000;
    }

    public int getVehicleSpeed() {
        return _vehicleSpeed;
    }

    public synchronized void handleNewVehicleTimeout() {
        Point startingPoint = new Point();
        Vehicle vehicle;

        // Figure out the time to the next timeout.
        _nextNewVehicleTimeout = System.currentTimeMillis()
                                 + _newVehicleTimerDuration;

        // Create a new vehicle for each size and start them
        // on their way. Tell the vehicle where it will
        // begin, in what direction it is to travel and on
        // what canvas it appears.
        // Start with the east-bound vehicle starting from
        // the west edge.
        startingPoint.setLocation(0, RECT_CORNERS[2].y + CURB_OFFSET);
        vehicle = new Vehicle(startingPoint, EAST, _vehicleSpeed, VEHICLE_SIZE,
                              this);

        // Have the vehicle paint itself on the canvas.
        vehicle.paint((Graphics2D) getGraphics());

        // Put this new vehicle on the list.
        _vehicleList.add(vehicle);

        vehicle.start();

        // South-bound starting on north edge.
        startingPoint.setLocation(RECT_CORNERS[1].x + CURB_OFFSET, 0);
        vehicle = null;
        vehicle = new Vehicle(startingPoint, SOUTH, _vehicleSpeed,
                              VEHICLE_SIZE, this);
        vehicle.paint((Graphics2D) getGraphics());
        _vehicleList.add(vehicle);
        vehicle.start();

        // West-bound starting on east edge.
        startingPoint.setLocation(RECT_CORNERS[4].x - VEHICLE_SIZE.width,
                                  RECT_CORNERS[1].y + CURB_OFFSET);
        vehicle = new Vehicle(startingPoint, WEST, _vehicleSpeed, VEHICLE_SIZE,
                              this);
        vehicle.paint((Graphics2D) getGraphics());
        _vehicleList.add(vehicle);
        vehicle.start();

        // North-bound starting on south edge.
        startingPoint.setLocation(RECT_CORNERS[2].x + CURB_OFFSET,
                                  RECT_CORNERS[4].y - VEHICLE_SIZE.height);
        vehicle = new Vehicle(startingPoint, NORTH, _vehicleSpeed,
                              VEHICLE_SIZE, this);
        vehicle.paint((Graphics2D) getGraphics());
        _vehicleList.add(vehicle);
        vehicle.start();

        return;
    }

    public synchronized void handleRepaintTimeout() {
        Iterator<Vehicle> it;

        // Tell each vehicle to move.
        for (Vehicle vehicle : _vehicleList) {
            vehicle.move((Graphics2D) getGraphics());
        }

        // Tell the stop light to paint itself only
        // if it needs to be.
        if (_lightChanged == true) {
            _lightChanged = false;
            _stopLight.paint(getGraphics());
        }

        // After the move, some vehicles may now be off the
        // canvas. Go through the "vehicle gone" list and
        // remove them from the vehicle list. The reason for
        // the two separate vehicle lists is to avoid
        // removing items from a list which is being
        // iterated over.
        for (it = _removeList.iterator(); it.hasNext() == true;) {
            _vehicleList.remove(it.next());
        }

        // Clear the remove list as its contents are no
        // longer needed.
        _removeList.clear();

        return;
    }

    public synchronized void lightChanged(int lightDirection) {
        // Tell all the vehicles stopped at the light that they
        // can go.
        switch (lightDirection) {
            case EW_LIGHT:
                for (Vehicle vehicle : _stoplightQueue[EASTLIGHT]) {
                    vehicle.lightGreen();
                }
                _stoplightQueue[EASTLIGHT].clear();

                for (Vehicle vehicle : _stoplightQueue[WESTLIGHT]) {
                    vehicle.lightGreen();
                }
                _stoplightQueue[WESTLIGHT].clear();
                break;

            case NS_LIGHT:
                for (Vehicle vehicle : _stoplightQueue[NORTHLIGHT]) {
                    vehicle.lightGreen();
                }
                _stoplightQueue[NORTHLIGHT].clear();

                for (Vehicle vehicle : _stoplightQueue[SOUTHLIGHT]) {
                    vehicle.lightGreen();
                }
                _stoplightQueue[SOUTHLIGHT].clear();
                break;

            case YELLOW_LIGHT:
            default:
                break;
        }

        // The stop light is informing us that it has
        // changed. If the repaint timer is not running,
        // turn it one now so that the stop light can
        // be repainted.
        _lightChanged = true;

        return;
    }

    public boolean mayKeepGoing(Point location, int speed, Point direction) {
        boolean retval;
        Color lightsColor;
        int distance;

        // The vehicle may *not* keep going if 1) the light is
        // not green and 2) it has reached the light's stopping
        // point. The stopping point is the intersection +
        // (Number of vehicles already waiting at the light *
        // vehicle size) + (number of waiting vehicles - 1 *
        // distance between vehicles).
        lightsColor = getLightsColor(direction);
        distance = getDistanceToIntersection(location, direction);
        if (lightsColor != Color.green && distance >= 0 && distance <= speed) {
            retval = false;
        } else {
            retval = true;
        }

        return retval;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // First, lay down a white rectangle.
        g2.setPaint(Color.white);
        g2.fill(new Rectangle2D.Double(CANVAS_CORNER.getX(),
                                       CANVAS_CORNER.getY(),
                                       CANVAS_SIZE.getWidth(),
                                       CANVAS_SIZE.getHeight()));

        // Draw the four green rectangles in each corner.
        // The rectangle's width and height will be 40% of the
        // canvas area. The coordinates are:
        //
        //      x[0]     x[1] x[2] x[3]    x[4]
        // y[0]  +--------+        +--------+
        //       |        |        |        |
        // y[1]  +--------+        +--------+
        // y[2]
        // y[3]  +--------+        +--------+
        //       |        |        |        |
        // y[4]  +--------+        +--------+
        //

        // Draw each of the four rectangles.
        g2.setPaint(Color.green);
        g2.fill(new Rectangle2D.Double(RECT_CORNERS[0].x, RECT_CORNERS[0].y,
                                       FIELD_SIZE.width, FIELD_SIZE.height));
        g2.fill(new Rectangle2D.Double(RECT_CORNERS[3].x, RECT_CORNERS[0].y,
                                       FIELD_SIZE.width, FIELD_SIZE.height));
        g2.fill(new Rectangle2D.Double(RECT_CORNERS[0].x, RECT_CORNERS[3].y,
                                       FIELD_SIZE.width, FIELD_SIZE.height));
        g2.fill(new Rectangle2D.Double(RECT_CORNERS[3].x, RECT_CORNERS[3].y,
                                       FIELD_SIZE.width, FIELD_SIZE.height));

        // Now draw the lane markings. Start with the line on the
        // north side and go clockwise.
        g2.setPaint(Color.black);
        g2.setStroke(DASHED);
        g2.draw(new Line2D.Double(RECT_CORNERS[2].x, RECT_CORNERS[0].y,
                                  RECT_CORNERS[2].x, RECT_CORNERS[1].y));
        g2.draw(new Line2D.Double(RECT_CORNERS[3].x, RECT_CORNERS[2].y,
                                  RECT_CORNERS[4].x, RECT_CORNERS[2].y));
        g2.draw(new Line2D.Double(RECT_CORNERS[2].x, RECT_CORNERS[3].y,
                                  RECT_CORNERS[2].x, RECT_CORNERS[4].y));
        g2.draw(new Line2D.Double(RECT_CORNERS[0].x, RECT_CORNERS[2].y,
                                  RECT_CORNERS[1].x, RECT_CORNERS[2].y));

        // Paint the vehicles.
        for (Vehicle vehicle : _vehicleList) {
            vehicle.paint(g2);
        }

        // Have the stop light draw itself.
        _stopLight.paint(g);

        return;
    }

    public synchronized void pauseDemo() {
        // Kill the timers for now but leave the graphic items
        // displayed.
        if (_newVehicleTimer != null && _newVehicleTimer.isRunning() == true) {
            long currTime = System.currentTimeMillis();
            long timeLeft;

            _newVehicleTimer.stop();

            // Figure out the number of milliseconds to the
            // next new vehicle timeout and set the timer's
            // initial delay to that.
            timeLeft = _nextNewVehicleTimeout - currTime;
            if (timeLeft < INITIAL_NEW_VEHICLE_DELAY) {
                timeLeft = INITIAL_NEW_VEHICLE_DELAY;
            }
            _newVehicleTimer.setInitialDelay((int) timeLeft);
        }

        if (_repaintTimer != null && _repaintTimer.isRunning() == true) {
            _repaintTimer.stop();
        }

        _stopLight.pauseDemo();

        return;
    }

    public void setLightDuration(int direction, int duration) {
        // Convert the duration from seconds to milliseconds.
        _stopLight.setLightDuration(direction, duration * 1000);
        return;
    }

    public void setNewVehicleRate(int duration) {
        _newVehicleTimerDuration = duration * 1000;

        // Stop the timer, reset its value and start it again.
        if (_newVehicleTimer != null) {
            long currTime;

            _newVehicleTimer.stop();
            currTime = System.currentTimeMillis();
            _newVehicleTimer.setDelay(_newVehicleTimerDuration);
            _newVehicleTimer.start();

            _nextNewVehicleTimeout = currTime + _newVehicleTimerDuration;
        }

        return;
    }

    public void setVehicleSpeed(int speed) {
        _vehicleSpeed = speed;

        // Tell all vehicles their new speed.
        for (Vehicle vehicle : _vehicleList) {
            vehicle.setSpeed(speed);
        }

        return;
    }

    // Start the demo running by starting the timers.
    public void startDemo() {
        // Have the stop light go to its initial settings.
        _stopLight.startDemo();

        // Start the timers.
        startNewVehicleTimer();
        startRepaintTimer();

        return;
    }

    public synchronized void stopDemo() {
        // Stop the timers and delete all vehicles.
        if (_newVehicleTimer != null && _newVehicleTimer.isRunning() == true) {
            _newVehicleTimer.stop();
        }

        if (_repaintTimer != null && _repaintTimer.isRunning() == true) {
            _repaintTimer.stop();
        }

        for (Vehicle vehicle : _vehicleList) {
            vehicle.stopDemo(getGraphics());
        }

        _vehicleList.clear();

        _stopLight.stopDemo();

        return;
    }

    public synchronized void vehicleGone(Vehicle vehicle) {
        _removeList.add(vehicle);
        return;
    }

    public void watchingLight(Point direction, Vehicle vehicle) {
        // If heading east, then watching west light.
        if (direction.x == EAST.x && direction.y == EAST.y) {
            _stoplightQueue[WESTLIGHT].add(vehicle);
        } else if (direction.x == SOUTH.x && direction.y == SOUTH.y) {
            _stoplightQueue[NORTHLIGHT].add(vehicle);
        } else if (direction.x == WEST.x && direction.y == WEST.y) {
            _stoplightQueue[EASTLIGHT].add(vehicle);
        } else if (direction.x == NORTH.x && direction.y == NORTH.y) {
            _stoplightQueue[SOUTHLIGHT].add(vehicle);
        }

        return;
    }

    private void startNewVehicleTimer() {
        long currTime;

        // If the timer does not exist, create one.
        if (_newVehicleTimer == null) {
            // Use an inner class to receive the timeout.
            _newVehicleTimer = new javax.swing.Timer(
                                                     _newVehicleTimerDuration,
                                                     new NewVehicleTimeoutListener(
                                                                                   this));

            // Start creating new vehicle right away.
            _newVehicleTimer.setInitialDelay((int) INITIAL_NEW_VEHICLE_DELAY);
            currTime = System.currentTimeMillis();
            _newVehicleTimer.start();

            _nextNewVehicleTimeout = currTime + INITIAL_NEW_VEHICLE_DELAY;
        } else if (_newVehicleTimer.isRunning() == false) {
            currTime = System.currentTimeMillis();
            _newVehicleTimer.restart();

            _nextNewVehicleTimeout = currTime + _newVehicleTimerDuration;
        }

        return;
    }

    private void startRepaintTimer() {
        // If the repaint timer does not exist, make one.
        if (_repaintTimer == null) {
            // Use an inner class to receive the timeout.
            _repaintTimer = new javax.swing.Timer(
                                                  REPAINT_TIME,
                                                  new RepaintTimeoutListener(
                                                                             this));
        }

        if (_repaintTimer.isRunning() == false) {
            _repaintTimer.start();
        }

        return;
    }
}
