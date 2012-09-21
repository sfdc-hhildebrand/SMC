//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
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
//  Vehicle
//
// Description
//  Every vehicle which appears on the canvas is an instance of
//  this class.
//
// RCS ID
// $Id: Vehicle.java,v 1.7 2009/03/27 09:41:46 cwrapp Exp $
//
// CHANGE LOG
// $Log: Vehicle.java,v $
// Revision 1.7  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.6  2009/03/01 18:20:38  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:04:44  charlesr
// Initial revision
//

package example_4;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

public final class Vehicle {
    // Member methods.

    //----------------------------------------
    // Dynamic data.
    //
    // Where am I?
    private Point          _position;

    // Where am I going?
    private Point          _direction;

    // How fast am I going?
    private int            _speed;

    // On what canvas do I appear?
    private TrafficCanvas  _owner;

    // How large am I?
    private Dimension      _size;

    // What am I doing?
    private VehicleContext _fsm;

    // Where am I being drawn?
    private Graphics2D     _graphic;

    public Vehicle(Point startingPoint, Point direction, int speed,
                   Dimension size, TrafficCanvas owner) {
        _position = new Point(startingPoint);
        _direction = new Point(direction);
        _speed = speed;
        _size = new Dimension(size);
        _owner = owner;

        _fsm = new VehicleContext(this);

        // Uncomment to see debug output.
        // _fsm.setDebugFlag(true);
    }

    //----------------------------------------
    // State Machine Actions
    //

    public void Advance() {
        int distance = _owner.getDistanceToIntersection(_position, _direction);
        Color lightColor = _owner.getLightsColor(_direction);
        int dx;
        int dy;

        // Cover over the vehicle's last position.
        draw(_graphic, Color.white);

        // Figure out the vehicle's new position.
        // If the light is red, then advance to intersection
        // and stop. Otherwise, keep going.
        if (lightColor == Color.red && distance <= _size.width + _speed) {
            dx = _direction.x * distance;
            dy = _direction.y * distance;
        } else {
            dx = _direction.x * _speed;
            dy = _direction.y * _speed;
        }

        _position.translate(dx, dy);

        // Draw the vehicle at its new location.
        draw(_graphic, Color.black);
        _graphic = null;

        return;
    }

    public void EndTrip() {
        // Cover over the vehicle's last position.
        draw(_graphic, Color.white);
        _graphic = null;

        _owner.vehicleGone(this);

        return;
    }

    public int getDistanceToIntersection() {
        return _owner.getDistanceToIntersection(_position, _direction);
    }

    public Color getLightsColor() {
        return _owner.getLightsColor(_direction);
    }

    public int getMaxX() {
        return _owner.getMaxX();
    }

    public int getMaxY() {
        return _owner.getMaxY();
    }

    public int getMinX() {
        return _owner.getMinX();
    }

    public int getMinY() {
        return _owner.getMinY();
    }

    public int getSpeed() {
        return _speed;
    }

    public int getXPos() {
        Point newPosition = new Point(_position);

        newPosition.translate(_direction.x * _speed, _direction.y * _speed);

        return newPosition.x;
    }

    public int getYPos() {
        Point newPosition = new Point(_position);

        newPosition.translate(_direction.x * _speed, _direction.y * _speed);

        return newPosition.y;
    }

    public void lightGreen() {
        _fsm.LightGreen();
        return;
    }

    public boolean MayKeepGoing() {
        return _owner.mayKeepGoing(_position, _speed, _direction);
    }

    // Member data.

    public void move(Graphics2D g2) {
        _graphic = g2;
        _fsm.Move();
        return;
    }

    public void paint(Graphics2D g2) {
        draw(g2, Color.black);
        return;
    }

    public void setSpeed(int speed) {
        _speed = speed;
        return;
    }

    public void start() {
        _fsm.enterStartState();
        _fsm.Start();

        return;
    }

    public void stopDemo(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        _fsm.Stop();

        // Cover over the vehicle's last position.
        draw(g2, Color.white);

        return;
    }

    public void WatchLight() {
        _owner.watchingLight(_direction, this);
        return;
    }

    private void draw(Graphics2D g2, Paint color) {
        Point p1;
        Point p2;
        Point l1;
        Point l2;
        Point position;
        Dimension size;
        Dimension lightSize;
        p1 = new Point(_position);
        p2 = new Point();
        position = new Point();
        size = new Dimension(_size);
        l1 = new Point();
        l2 = new Point();
        lightSize = new Dimension();

        // Check if the vehicle is moving through a
        // stoplight. Only paint that part of the
        // vehicle that is not under the stop light.

        // Get the stoplight's position, size and direction based
        // on the vehicle's direction.
        _owner.getLightDimensions(_direction, l1, lightSize);
        p2.x = _position.x + _size.width;
        p2.y = _position.y + _size.height;
        l2.x = l1.x + lightSize.width;
        l2.y = l1.y + lightSize.height;

        // Is the vehicle passing under the stoplight? It depends
        // on what direction this vehicle is moving.
        // For east and west, only check the x axis. For north
        // and south, only check the y axis.
        if (_direction.x == TrafficCanvas.EAST.x
            && _direction.y == TrafficCanvas.EAST.y
            || _direction.x == TrafficCanvas.WEST.x
            && _direction.y == TrafficCanvas.WEST.y) {
            // Is entire vehicle clear? It is if the vehicle's
            // left is to the light's right or the vehicle's
            // right is to the light's left or 
            if (p1.x > l2.x || p2.x < l1.x) {
                position.x = p1.x;
                position.y = p1.y;
                size.width = _size.width;
                size.height = _size.height;
            }
            // Is the vehicle's right side covered?
            else if (p1.x < l1.x) {
                // Yes. Figure out by how much.
                position.x = p1.x;
                position.y = p1.y;
                size.width = l1.x - p1.x - 1;
                size.height = _size.height;
            }
            // Is the vehicle's left side covered?
            else if (p2.x > l2.x) {
                position.x = l2.x + 1;
                position.y = p1.y;
                size.width = p2.x - l2.x - 1;
                size.height = _size.height;
            } else {
                // The vehicle is entirely covered.
                // Set size to 0.
                size.width = 0;
                size.height = 0;
            }
        }
        // Otherwise, we are heading north or south.
        else {
            // Check if the vehicle is entirely clear of the
            // stoplight.
            if (p1.y > l2.y || p2.y < l1.y) {
                position.x = p1.x;
                position.y = p1.y;
                size.width = _size.width;
                size.height = _size.height;
            }
            // Top part clear.
            else if (p1.y < l1.y) {
                position.x = p1.x;
                position.y = p1.y;
                size.width = _size.width;
                size.height = l1.y - p1.y - 1;
            } else if (p2.y > l2.y) {
                position.x = p1.x;
                position.y = l2.y + 1;
                size.width = _size.width;
                size.height = p2.y - l2.y - 1;
            } else {
                size.width = 0;
                size.height = 0;
            }
        }

        // Do the painting only if there is a rectangle to
        // paint, that is both the width and height are greater
        // than 0.
        if (size.width > 0 && size.height > 0) {
            g2.setPaint(color);
            g2.fill(new Rectangle2D.Double(position.getX(), position.getY(),
                                           size.getWidth(), size.getHeight()));
        }

        return;
    }
}
