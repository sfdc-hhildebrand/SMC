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
// Copyright (C) 2000 - 2007. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// Name
//  AsyncTimer.java
//
// Description
//  Non-swing based timing. Not needed for Java 1.3.
//
// RCS ID
// $Id: AsyncTimer.java,v 1.6 2007/12/28 12:34:40 cwrapp Exp $
//
// CHANGE LOG
// $Log: AsyncTimer.java,v $
// Revision 1.6  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.5  2007/02/21 13:42:53  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:17:13  charlesr
// Initial revision
//

package example_6;

import java.util.HashMap;
import java.util.Map;

public final class AsyncTimer {
    // The timer thread class.
    private static final class TimerThread extends Thread {
        // Each timer has a name, a duration and a listener
        // who is informed when the timer has expired.
        private String        _name;

        private long          _duration;

        private TimerListener _listener;

        // When the AsyncTimer stops this timer, this
        // boolean is set to true.
        private boolean       _wasStopped;

        // When this timer is reset, this boolean is
        // set to true.
        private boolean       _wasReset;

        // When the timer has successfully done its
        // task, this boolean is set to true.
        private boolean       _isDone;

        public TimerThread(String name, long millisecs, TimerListener listener) {
            _name = name;
            _duration = millisecs;
            _listener = listener;
            _wasStopped = false;
            _wasReset = false;
            _isDone = false;
        }

        @Override
        public void run() {
            // Keep doing this until the timer has either
            // successfully completed a sleep or it is
            // stopped.
            while (_isDone == false && _wasStopped == false) {
                // Go to sleep. When the timer has expired, first
                // tell the listener and then the alarm clock.
                try {
                    sleep(_duration);

                    // Only issue this timeout if the timer
                    // was not stopped.
                    if (_wasStopped == false && _wasReset == false) {
                        // The timer has successfully done
                        // its task.
                        _isDone = true;

                        // Tell the alarm clock to delete this
                        // timer. Do this before issuing the
                        // callback in case the callback tries
                        // to create this timer again.
                        AsyncTimer.timerDone(_name);

                        // Issue the callback to the interested
                        // party.
                        _listener.handleTimeout(_name);
                    }
                } catch (InterruptedException e) {
                    // If this timer has been reset, then
                    // reset the flag to false and go to sleep
                    // again.
                    _wasReset = false;
                }
            }

            return;
        }

        @Override
        public String toString() {
            return _name;
        }

        private void resetTimer() {
            _wasReset = true;
            return;
        }

        private void resetTimer(long millisecs) {
            _wasReset = true;
            _duration = millisecs;
            return;
        }

        private void stopTimer() {
            _wasStopped = true;
            return;
        }
    }

    // Keep track of all the currently running timers.
    private static Map<String, TimerThread> _timerMap;

    static {
        _timerMap = new HashMap<String, TimerThread>();
    }

    // Reset a timer, reusing its current duration.
    public static boolean resetTimer(String name) {
        boolean Retcode;
        TimerThread Timer;

        // If there is no such timer, fail this request.
        if ((Timer = _timerMap.get(name)) == null) {
            Retcode = false;
        } else {
            Retcode = true;

            Timer.resetTimer();
            Timer.interrupt();
        }

        return Retcode;
    }

    // Reset a timer to the new timeout.
    public static boolean resetTimer(String name, long millisecs) {
        boolean Retcode;
        TimerThread Timer;

        // If there is no such timer, fail this request.
        if ((Timer = _timerMap.get(name)) == null) {
            Retcode = false;
        } else {
            Retcode = true;

            Timer.resetTimer(millisecs);
            Timer.interrupt();
        }

        return Retcode;
    }

    // Create a new timer. Let the caller know when it expires.
    public static boolean startTimer(String name, long millisecs,
                                     TimerListener listener) {
        boolean Retcode;

        // Fail if there already exists a timer by this name.
        if (_timerMap.containsKey(name) == true) {
            Retcode = false;
        } else {
            // Create a new thread and pass in the necessary
            // info.
            TimerThread NewTimer = new TimerThread(name, millisecs, listener);

            _timerMap.put(name, NewTimer);

            // Start the timer.
            NewTimer.start();

            Retcode = true;
        }

        return Retcode;
    }

    // Stop all running timers.
    public static void stopAllTimers() {
        for (TimerThread timer : _timerMap.values()) {
            timer.stopTimer();
            timer.interrupt();
        }

        // Remove all the timer threads from the map.
        _timerMap.clear();

        return;
    }

    // Stop a running timer.
    public static boolean stopTimer(String name) {
        TimerThread timer;

        // First, tell the timer to stop. Then remove
        // the timer from the map.
        if ((timer = _timerMap.get(name)) != null) {
            timer.stopTimer();
            _timerMap.remove(name);
            timer.interrupt();
        }

        // This method always succeeds because even if the timer
        // doesn't exist, it was successfully "stopped".
        return true;
    }

    private static synchronized void timerDone(String name) {
        // Remove the timer from the list and delete it.
        _timerMap.remove(name);
        return;
    }

    // This is a singleton object.
    public AsyncTimer() {
    }
}
