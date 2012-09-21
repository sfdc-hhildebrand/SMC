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
//  TaskManager.java
//
// Description
//  This singleton is responsible for scheduling the running
//  task.
//
// RCS ID
// $Id: TaskManager.java,v 1.9 2009/11/25 22:30:18 cwrapp Exp $
//
// CHANGE LOG
// $Log: TaskManager.java,v $
// Revision 1.9  2009/11/25 22:30:18  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.8  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.7  2009/03/01 18:20:39  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.6  2007/02/21 13:40:56  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:14:18  charlesr
// Initial revision
//

package example_5;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

public final class TaskManager implements TaskEventListener {
    //---------------------------------------------------------------
    // Member methods.
    //

    private final class TimerListener implements ActionListener {
        private String      _timerName;

        private TaskManager _owner;

        public TimerListener(String name, TaskManager owner) {
            _timerName = name;
            _owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Map<String, Object> args = new HashMap<String, Object>();

            _owner.handleEvent(_timerName, args);
            return;
        }
    }

    private TaskManagerFSM     _fsm;

    //===========================================================
    // These methods respond to viewer messages.
    //

    // Runnable task queue, sorted by priority.
    private List<Task>         _runnableTaskQueue;

    // Blocked task list.
    private List<Task>         _blockedTaskList;

    // The currently running task.
    private Task               _runningTask;

    // Task manager's various timers.
    private Map<String, Timer> _timerTable;

    // The application's exit code.
    private int                _exitCode;

    public TaskManager() {
        TaskController control = new TaskController();

        _runningTask = null;
        _runnableTaskQueue = new LinkedList<Task>();
        _blockedTaskList = new LinkedList<Task>();
        _timerTable = new HashMap<String, Timer>();
        _exitCode = 0;

        _fsm = new TaskManagerFSM(this);

        // Uncomment to see debug output.
        // _fsm.setDebugFlag(true);

        // Register with the controller.
        control.register("Task Manager", this);
    }

    //===========================================================
    // These methods handle task object messages.
    //

    // Check if there are any tasks to run.
    public boolean areTasksQueued() {
        return _runnableTaskQueue.size() > 0;
    }

    // Block the specified task. If that task is running,
    // then remove it.
    public void blockTask(String taskName) {
        Task task;

        if ((task = findTask(taskName)) != null) {
            TaskController control = new TaskController();

            sendMessage(2, "Task " + taskName + " is blocked.");

            // Tell the task to block.
            control.postMessage(taskName, "block");

            if (task == _runningTask) {
                _runningTask = null;
            } else {
                // Remove the task from the runnable queue.
                _runnableTaskQueue.remove(task);
            }

            _blockedTaskList.add(task);
        }

        return;
    }

    // Check if there are any tasks to run. If yes, then
    // asynchronously issue a RunTask transition using the
    // setTimer() method.
    public void checkTaskQueue() {
        if (_runnableTaskQueue.size() > 0) {
            // Create a timer which will expire immediately.
            setTimer("Run Task", 0);
        }

        return;
    }

    //===========================================================
    // State machine actions.
    //

    // Create a new task.
    public void createTask(String name, int time, int priority) {
        if (name == null || name.length() == 0) {
            sendMessage(0, "Cannot create task without a name.");
        } else if (taskExists(name) == true) {
            sendMessage(0, "Cannot create task named \"" + name
                           + "\" - a task with that name already exists.");
        } else {
            Task newTask = new Task(name, priority, time);

            _runnableTaskQueue.add(newTask);
            newTask.start();
            sendMessage(1,
                        "Created task " + name + "(priority: "
                                + Integer.toString(priority) + ", time: "
                                + Integer.toString(time) + ").");

            _fsm.TaskCreated();
        }

        return;
    }

    // Forcibly delete all existing tasks with extreme prejudice.
    public void deleteAllTasks() {
        _runningTask = null;
        _runnableTaskQueue.clear();
        _blockedTaskList.clear();

        return;
    }

    public void deleteRunningTask() {
        if (_runningTask != null) {
            _runningTask = null;
        }

        return;
    }

    public void deleteTask(String taskName) {
        if ((findTask(taskName)) != null) {
            TaskController control = new TaskController();

            // Tell the task to go and die.
            control.postMessage(taskName, "delete");
        }

        return;
    }

    public void exitApplication() {
        // Wait another 1.5 secs before actually dying.
        setTimer("Exit", 1500);
        return;
    }

    public Task findTask(String taskName) {
        Task retval = null;

        // Is the running task the one we are looking for?
        if (_runningTask != null
            && taskName.compareTo(_runningTask.getName()) == 0) {
            retval = _runningTask;
        } else {
            // Is the task in the runnable queue?
            for (Task task : _runnableTaskQueue) {
                if (taskName.compareTo(task.getName()) == 0) {
                    retval = task;
                    break;
                }
            }

            // Is this task in the blocked list?
            if (retval == null) {
                for (Task task : _blockedTaskList) {
                    if (taskName.compareTo(task.getName()) == 0) {
                        retval = task;
                        break;
                    }
                }
            }
        }

        return retval;
    }

    // Return the number of blocked tasks.
    public int getBlockedTaskCount() {
        return _blockedTaskList.size();
    }

    // Return the number of runnable tasks.
    public int getRunnableTaskCount() {
        return _runnableTaskQueue.size();
    }

    // Return the currently running task.
    public Task getRunningTask() {
        return _runningTask;
    }

    // Issue the state machine transition associated with this
    // timer name. Also, remove the now defunct timer from the
    // timer table.
    @Override
    public void handleEvent(String eventName, Map<String, Object> args) {
        String taskName;

        if (eventName.compareTo("Create Task") == 0) {
            Integer runtime;
            Integer priority;

            taskName = (String) args.get("Task Name");
            runtime = (Integer) args.get("Runtime");
            priority = (Integer) args.get("Priority");
            createTask(taskName, runtime.intValue(), priority.intValue());
        } else if (eventName.compareTo("Run Task") == 0) {
            _fsm.RunTask();
        } else if (eventName.compareTo("Slice Timeout") == 0) {
            _fsm.SliceTimeout();
        } else if (eventName.compareTo("Reply Timeout") == 0) {
            _fsm.ReplyTimeout();
        } else if (eventName.compareTo("Suspend Task") == 0) {
            suspendTask();
        } else if (eventName.compareTo("Block Task") == 0) {
            taskName = (String) args.get("Task Name");
            blockTask(taskName);
        } else if (eventName.compareTo("Unblock Task") == 0) {
            taskName = (String) args.get("Task Name");
            unblockTask(taskName);
        } else if (eventName.compareTo("Delete Task") == 0) {
            taskName = (String) args.get("Task Name");
            deleteTask(taskName);
        } else if (eventName.compareTo("Task Suspended") == 0) {
            taskName = (String) args.get("Task Name");
            sendMessage(2, "Task " + taskName + " has been suspended.");

            _fsm.TaskSuspended();
        } else if (eventName.compareTo("Task Done") == 0) {
            taskName = (String) args.get("Task Name");
            taskDone(taskName);
        } else if (eventName.compareTo("Task Stopped") == 0) {
            taskName = (String) args.get("Task Name");
            taskStopped(taskName);
        } else if (eventName.compareTo("Task Deleted") == 0) {
            taskName = (String) args.get("Task Name");
            taskDeleted(taskName);
        } else if (eventName.compareTo("Shutdown") == 0) {
            Integer exitCode;

            exitCode = (Integer) args.get("Exit Code");
            _exitCode = exitCode.intValue();

            _fsm.Shutdown();
        } else if (eventName.compareTo("Exit") == 0) {
            System.exit(_exitCode);
        } else if (eventName.compareTo("ShutdownTimeout") == 0) {
            _fsm.ShutdownTimeout();
        }

        return;
    }

    // Send a message to the GUI controller so it can be posted
    // on the message display.
    public void sendMessage(int level, String message) {
        TaskController control = new TaskController();
        Map<String, Object> args = new HashMap<String, Object>();

        args.put("level", new Integer(level));
        args.put("object", "TaskManager");
        args.put("message", message);
        control.postMessage("Message GUI", "Post Message", args);

        return;
    }

    // Create a timer for the specified period. When the timer
    // expires, issue the corresponding state machine transition.
    public void setTimer(String name, int period) {
        Timer timer;

        // Is there a timer with this name already?
        if (_timerTable.containsKey(name) == true) {
            // Yes, there is. Stop the current timer and then
            // start it again.
            stopTimer(name);
        }

        timer = new Timer(period, new TimerListener(name, this));
        timer.setRepeats(false);
        _timerTable.put(name, timer);

        // Start the timer running.
        timer.start();

        return;
    }

    // Shutting down the application.
    public void shutdown() {
        _fsm.Shutdown();
        return;
    }

    public void start() {
        _fsm.enterStartState();
        return;
    }

    // Task the highest priority task off the runnable queue
    // and have it start running.
    public void startTask() {
        Iterator<Task> taskIt;
        Task task;
        int index;
        int taskIndex;
        int taskPriority;
        int currentMinPriority;

        // Find the task with the lowest priority.
        for (taskIt = _runnableTaskQueue.iterator(), currentMinPriority = Integer.MAX_VALUE, index = 0, taskIndex = -1; taskIt.hasNext() == true; ++index) {
            task = taskIt.next();
            taskPriority = task.getDynamicPriority();

            // Is the new task's priority less than
            // the current task. 
            if (taskPriority < currentMinPriority) {
                taskIndex = index;
                currentMinPriority = taskPriority;
            }
        }

        // Was a task found?
        if (taskIndex >= 0) {
            TaskController control = new TaskController();

            _runningTask = _runnableTaskQueue.remove(taskIndex);
            sendMessage(2, "Attempting to run task " + _runningTask.getName()
                           + ".");

            control.postMessage(_runningTask.getName(), "start");
        }

        return;
    }

    public void stopAllTasks() {
        TaskController control = new TaskController();

        // Put all tasks into the blocked list. As they report
        // that they are stopped, remove the tasks.
        //
        // Do the blocked list first.
        for (Task task : _blockedTaskList) {
            sendMessage(3, "Stopping task " + task.getName() + ".");
            control.postMessage(task.getName(), "stop");
        }

        // Do the runnable tasks next.
        for (Task task : _runnableTaskQueue) {
            sendMessage(3, "Stopping task " + task.getName() + ".");
            control.postMessage(task.getName(), "stop");
            _blockedTaskList.add(task);
        }
        _runnableTaskQueue.clear();

        // Do the running task last.
        if (_runningTask != null) {
            sendMessage(3, "Stopping task " + _runningTask.getName() + ".");
            control.postMessage(_runningTask.getName(), "stop");
            _blockedTaskList.add(_runningTask);
            _runningTask = null;
        }

        return;
    }

    // Cancel all existing timers.
    public void stopAllTimers() {
        for (Timer timer : _timerTable.values()) {
            timer.stop();
        }

        _timerTable.clear();

        return;
    }

    //---------------------------------------------------------------
    // Member data.
    //

    // Stop the named timer if it is running.
    public void stopTimer(String name) {
        Timer timer;

        // Remove the timer from the table and stop it.
        if ((timer = _timerTable.remove(name)) != null) {
            timer.stop();
        }

        return;
    }

    // Suspend the currently running task - if there is one.
    public void suspendTask() {
        if (_runningTask != null) {
            TaskController control = new TaskController();

            sendMessage(2, "Suspending task " + _runningTask.getName() + ".");

            // Tell the task to suspend.
            control.postMessage(_runningTask.getName(), "suspend");

            // Put the task back on to the runnable queue.
            _runnableTaskQueue.add(_runningTask);
            _runningTask = null;
        }

        return;
    }

    // A task has stopped and is ready for deletion.
    public void taskDeleted(String taskName) {
        Task task;
        int taskIndex;

        if ((task = findTask(taskName)) != null) {
            sendMessage(1, "Task " + taskName + " deleted.");

            if (task == _runningTask) {
                _runningTask = null;
                _fsm.TaskDeleted();
            } else if ((taskIndex = _runnableTaskQueue.indexOf(task)) >= 0) {
                _runnableTaskQueue.remove(taskIndex);
            } else if ((taskIndex = _blockedTaskList.indexOf(task)) >= 0) {
                _blockedTaskList.remove(taskIndex);
            }
        }

        return;
    }

    // The running task has completed its work.
    public void taskDone(String taskName) {
        Task task;
        int taskIndex;

        if ((task = findTask(taskName)) != null) {
            sendMessage(1, "Task " + taskName + " has completed.");

            // Is this the running task?
            if (task == _runningTask) {
                _runningTask = null;
                _fsm.TaskDone();
            } else if ((taskIndex = _runnableTaskQueue.indexOf(task)) >= 0) {
                // I don't know how a suspended task managed to
                // complete. Remove it from a runnable list.
                _runnableTaskQueue.remove(taskIndex);
            } else if ((taskIndex = _blockedTaskList.indexOf(task)) >= 0) {
                // I don't know how a blocked task managed to
                // complete. Remove it from the blocked list.
                _blockedTaskList.remove(taskIndex);
            }
        }

        return;
    }

    // Does a task already exist with this name?
    public boolean taskExists(String name) {
        return findTask(name) == null ? false : true;
    }

    // A task has stopped and is ready for deletion.
    public void taskStopped(String taskName) {
        Task task;
        int taskIndex;

        if ((task = findTask(taskName)) != null
            && (taskIndex = _blockedTaskList.indexOf(task)) >= 0) {
            sendMessage(1, "Task " + taskName + " is stopped.");
            _blockedTaskList.remove(taskIndex);
            _fsm.TaskStopped();
        } else {
            sendMessage(4, "TaskManager::taskStopped: " + taskName
                           + " not on blocked list.");
        }

        return;
    }

    // Inner classes.

    public void unblockTask(String taskName) {
        Task task;
        int taskIndex;

        // Is there a task with this name?
        if ((task = findTask(taskName)) != null) {
            // Is this task on the blocked list?
            taskIndex = _blockedTaskList.indexOf(task);
            if (taskIndex >= 0) {
                TaskController control = new TaskController();

                sendMessage(2, "Task " + taskName + " is unblocked.");

                // Tell the task it is now unblocked.
                control.postMessage(task.getName(), "unblock");

                // Move the task from the blocked queue to the
                // runnable queue.
                _blockedTaskList.remove(taskIndex);
                _runnableTaskQueue.add(task);

                _fsm.TaskUnblocked();
            }
        }

        return;
    }
}
