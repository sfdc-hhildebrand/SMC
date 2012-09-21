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
//  Task.java
//
// Description
//  Each instance is a task to be run to completion.
//
// RCS ID
// $Id: Task.java,v 1.9 2009/11/25 22:30:18 cwrapp Exp $
//
// CHANGE LOG
// $Log: Task.java,v $
// Revision 1.9  2009/11/25 22:30:18  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.8  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.7  2009/03/01 18:20:39  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.6  2007/02/21 13:40:36  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:12:37  charlesr
// Initial revision
//

package example_5;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

public final class Task implements TaskEventListener {
    //---------------------------------------------------------------
    // Member methods.
    //

    private final class TaskTimerListener implements ActionListener {
        private String _name;

        private Task   _owner;

        private TaskTimerListener(String name, Task owner) {
            super();

            _name = name;
            _owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Map<String, Object> args = new HashMap<String, Object>();

            _owner.handleEvent(_name, args);
            return;
        }
    }

    private TaskFSM            _fsm;

    // The task's human readable name.
    private String             _name;

    // The task's fixed priority.
    private int                _priority;

    // The tasks's total run time in milliseconds.
    private int                _runtime;

    // How much run time the task has left.
    private int                _timeLeft;

    // When the task is told to run, remember at what time it
    // started. Then when the task stops running, it can figure
    // out for how long it was running and how much longer it
    // needs to run.
    private Date               _runStartTime;

    // When the task is suspended, remember at what time the
    // suspension started. This is necessary in order to
    // calculate the dynamic priority.
    private Date               _suspendStartTime;

    // Put internal timers here.
    private Map<String, Timer> _timerTable;

    public Task(String name, int priority, int time) {
        TaskController control = new TaskController();

        _name = name;
        _priority = priority;
        _runtime = time * 1000;
        _timeLeft = _runtime;
        _runStartTime = null;
        _timerTable = new HashMap<String, Timer>();

        _fsm = new TaskFSM(this);

        // Uncomment to see debug output.
        // _fsm.setDebugFlag(true);

        // Register with the task controller.
        control.register(name, this);

        // Since the task starts suspended, timestamp the suspend
        // start time.
        _suspendStartTime = new Date();

        // Have this task placed on the task display.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("name", name);
        args.put("status", "Suspended");
        args.put("priority", new Integer(priority));
        args.put("runtime", new Integer(time));

        control.postMessage("Task GUI", "Task Created", args);
    }

    //===========================================================
    // State Machine Actions.
    //

    // Deregister from the task controller.
    public void deregister() {
        TaskController control = new TaskController();
        control.deregister(_name);
        return;
    }

    public int getDynamicPriority() {
        int suspendTime;
        int retval;

        suspendTime = getSuspendTime();
        retval = _priority - suspendTime / 1000;

        return retval;
    }

    public String getName() {
        return _name;
    }

    public int getPercentComplete() {
        int retval;

        retval = (int) ((float) (_runtime - _timeLeft) / (float) _runtime * 100.0);
        return retval;
    }

    // Return this task's *static* priority.
    public int getPriority() {
        return _priority;
    }

    // Return how long this task has been suspended in
    // milliseconds.
    public int getSuspendTime() {
        Date currTime = new Date();
        long retval;

        retval = currTime.getTime() - _suspendStartTime.getTime();
        return (int) retval;
    }

    // Return the task's total run time in milliseconds.
    public int getTime() {
        return _runtime;
    }

    // Return task's remaining run time.
    public int getTimeLeft() {
        return _timeLeft;
    }

    @Override
    public void handleEvent(String eventName, Map<String, Object> args) {
        if (eventName.compareTo("start") == 0) {
            _fsm.Start();
        } else if (eventName.compareTo("suspend") == 0) {
            _fsm.Suspend();
        } else if (eventName.compareTo("block") == 0) {
            _fsm.Block();
        } else if (eventName.compareTo("unblock") == 0) {
            _fsm.Unblock();
        } else if (eventName.compareTo("stop") == 0) {
            _fsm.Stop();
        } else if (eventName.compareTo("delete") == 0) {
            _fsm.Delete();
        } else if (eventName.compareTo("Done") == 0) {
            _fsm.Done();
        } else if (eventName.compareTo("Update Time") == 0) {
            timeUpdate();
        } else if (eventName.compareTo("Stopped") == 0) {
            _fsm.Stopped();
        }

        return;
    }

    // Send a message to the GUI controller so it can be posted
    // on the message display.
    public void sendMessage(int level, String message) {
        TaskController control = new TaskController();
        Map<String, Object> args = new HashMap<String, Object>();

        args.put("level", new Integer(level));
        args.put("object", _name);
        args.put("message", message);
        control.postMessage("Message GUI", "Post Message", args);

        return;
    }

    public void setRunTimer() {
        if (_timeLeft > 0) {
            setTimer("Done", _timeLeft);
        } else {
            setTimer("Done", 0);
        }

        return;
    }

    //---------------------------------------------------------------
    // Member data.
    //

    public void setStartTime() {
        if (_timeLeft > 1000) {
            setTimer("Update Time", 1000);
        }

        _runStartTime = new Date();

        return;
    }

    public void setStopTime() {
        TaskController control = new TaskController();
        Date currTime = new Date();
        Map<String, Object> args = new HashMap<String, Object>();
        int percentComplete;

        _timeLeft = _timeLeft
                    - (int) (currTime.getTime() - _runStartTime.getTime());
        if (_timeLeft < 0) {
            _timeLeft = 0;
        }

        percentComplete = getPercentComplete();

        args.put("name", _name);
        args.put("percentComplete", new Integer(percentComplete));
        control.postMessage("Task GUI", "Task % Update", args);

        stopTimer("Update Time");
        return;
    }

    public void setSuspendTime() {
        _suspendStartTime = new Date();
        return;
    }

    // Create a timer for the specified period. When the timer
    // expires, issue the associated state machine transition.
    public void setTimer(String name, int period) {
        Timer timer;

        // Is there a timer with this name already?
        if (_timerTable.containsKey(name) == true) {
            // Yes, there is. Stop the current timer and then
            // start it again.
            stopTimer(name);
        }

        timer = new Timer(period, new TaskTimerListener(name, this));
        timer.setRepeats(false);
        _timerTable.put(name, timer);

        // Start the timer running.
        timer.start();

        return;
    }

    public void start() {
        _fsm.enterStartState();
        return;
    }

    // Tell the task manager about this tasks's change in status.
    public void stateUpdate(String change) {
        TaskController controller = new TaskController();
        Map<String, Object> args = new HashMap<String, Object>();

        args.put("name", _name);
        args.put("status", change);
        controller.postMessage("Task GUI", "Task State Update", args);
        return;
    }

    // Stop the specified timer if it is running.
    public void stopTimer(String name) {
        Timer timer;

        // Remove the timer from the table and stop it.
        if ((timer = _timerTable.remove(name)) != null) {
            timer.stop();
        }

        return;
    }

    // Update the task's percent complete display.
    public void timeUpdate() {
        TaskController control = new TaskController();
        int timeLeft;
        int percentComplete;
        Date currTime = new Date();
        Map<String, Object> args = new HashMap<String, Object>();

        timeLeft = _timeLeft
                   - (int) (currTime.getTime() - _runStartTime.getTime());

        if (timeLeft < 0) {
            timeLeft = 0;
        }

        percentComplete = (int) ((float) (_runtime - timeLeft)
                                 / (float) _runtime * 100.0);

        args.put("name", _name);
        args.put("percentComplete", new Integer(percentComplete));
        control.postMessage("Task GUI", "Task % Update", args);

        if (timeLeft > 1000) {
            setTimer("Update Time", 1000);
        }
    }

    // Inner Classes

    public void updateTaskMan(String state) {
        TaskController control = new TaskController();
        Map<String, Object> args = new HashMap<String, Object>();

        args.put("Task Name", _name);
        control.postMessage("Task Manager", state, args);

        return;
    }
}
