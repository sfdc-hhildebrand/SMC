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
//  TaskController.java
//
// Description
//  A singleton responsible for routing messages between objects.
//
// RCS ID
// $Id: TaskController.java,v 1.6 2007/02/21 13:40:45 cwrapp Exp $
//
// CHANGE LOG
// $Log: TaskController.java,v $
// Revision 1.6  2007/02/21 13:40:45  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:13:10  charlesr
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

public final class TaskController {
    // Member Methods.

    private final class Message {
        private String              _recepient;

        private String              _event;
        private Map<String, Object> _args;

        private Message(String recepient, String event, Map<String, Object> args) {
            super();

            _recepient = recepient;
            _event = event;
            _args = args;
        }
    }

    private final class SendTimerListener implements ActionListener {
        TaskController _owner;

        private SendTimerListener(TaskController owner) {
            super();

            _owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            _owner.sendMessage();
        }
    }

    // Each object has a unique name. Map each name to an object.
    private static Map<String, TaskEventListener> _objectMap;

    // Messages yet to be sent.
    private static List<Message>                  _messageQueue;

    // When this timer expires, it is time to send another
    // message.
    private static Timer                          _sendTimer;

    static {
        _objectMap = new HashMap<String, TaskEventListener>();
        _messageQueue = new LinkedList<Message>();
        _sendTimer = null;
    }

    // Member Data.

    public TaskController() {
    }

    public void deregister(String name) {
        Iterator<Message> messIt;
        Message message;

        _objectMap.remove(name);

        // Remove all messages queued for this object.
        for (messIt = _messageQueue.iterator(); messIt.hasNext() == true;) {
            message = messIt.next();
            if (message._recepient.compareTo(name) == 0) {
                messIt.remove();
            }
        }

        return;
    }

    public void postMessage(String recepient, String event) {
        Map<String, Object> args = new HashMap<String, Object>();

        postMessage(recepient, event, args);

        return;
    }

    public void postMessage(String recepient, String event,
                            Map<String, Object> args) {
        // Is there a known recepient?
        if (_objectMap.containsKey(recepient) == true) {
            // Yes, enqueue the message for later delivery.
            Message message = new Message(recepient, event, args);
            _messageQueue.add(message);

            // Asynchronously send the message.
            if (_sendTimer == null) {
                _sendTimer = new Timer(1, new SendTimerListener(this));
                _sendTimer.start();
            } else if (_sendTimer.isRunning() == false) {
                _sendTimer.restart();
            }
        }

        return;
    }

    // Inner Classes.

    public void register(String name, TaskEventListener listener) {
        // Has an object already registered under this name?
        if (_objectMap.containsKey(name) == false) {
            _objectMap.put(name, listener);
        }

        return;
    }

    private void sendMessage() {
        boolean notSent = true;
        Message message;
        TaskEventListener listener;

        // Send a message only if there is a message to send.
        while (notSent == true && _messageQueue.size() > 0) {
            message = _messageQueue.remove(0);
            listener = _objectMap.get(message._recepient);
            if (listener != null) {
                notSent = false;
                listener.handleEvent(message._event, message._args);
            }
        }

        // If there are no more messages, stop the timer.
        if (_messageQueue.size() == 0) {
            _sendTimer.stop();
        }

        return;
    }
}
