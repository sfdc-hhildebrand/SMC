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
//  TaskMessages.java
//
// Description
//  Displays task log messages.
//
// RCS ID
// $Id: TaskMessages.java,v 1.5 2007/02/21 13:41:00 cwrapp Exp $
//
// CHANGE LOG
// $Log: TaskMessages.java,v $
// Revision 1.5  2007/02/21 13:41:00  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:14:54  charlesr
// Initial revision
//

package example_5;

import java.awt.Component;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

// This class displays the posted messages IF the message's
// priority passes the filter.
public final class TaskMessages implements TaskEventListener {
    // Member Methods

    // Currently acceptable message level.
    private int              _level;

    // Put messages in this text component.
    private JTextArea        _messageArea;

    // The messages are scrollable.
    private JScrollPane      _pane;

    // Formats the date/time string.
    private SimpleDateFormat _timestampFormat;

    // The number of rows and columns in the text area.
    private final static int ROW_COUNT = 10;

    private final static int COL_COUNT = 82;

    // Member Data

    // Each field's length.
    public final static int  NAME_LEN  = 15;

    // The padding and new line strings are used to create the
    // final message output.
    private static String    _padding;

    private static String    _newLine;

    static {
        _padding = new String("                ");
        _newLine = new String("\n");
    }

    public TaskMessages(int level) {
        TaskController control = new TaskController();
        Locale currentLocale = new Locale("en", "US");

        // Put together the date/time output format.
        _timestampFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss",
                                                currentLocale);

        // Set the initial level.
        _level = level;

        // Create the text area which displays the messages.
        _messageArea = new JTextArea(ROW_COUNT, COL_COUNT);
        _messageArea.setEditable(false);

        // Use a 8pt Helvetica font.
        _messageArea.setFont(new Font("Courier", Font.PLAIN, 10));

        // Place this text area inside a scrollable pane.
        _pane = new JScrollPane(_messageArea);

        control.register("Message GUI", this);
    }

    public Component getComponents() {
        return _pane;
    }

    public int getLevel() {
        return _level;
    }

    @Override
    public void handleEvent(String event, Map<String, Object> args) {
        Integer level;
        String name;
        String message;

        // Post a message to the text area.
        if (event.compareTo("Post Message") == 0) {
            level = (Integer) args.get("level");
            name = (String) args.get("object");
            message = (String) args.get("message");
            logMessage(level.intValue(), name, message);
        } else if (event.compareTo("Set Level") == 0) {
            level = (Integer) args.get("level");
            setLevel(level.intValue());
        }

        return;
    }

    public void logMessage(int level, String name, String message) {
        int stringLength;
        Date currTime = new Date();
        String timestamp;
        StringBuffer output = new StringBuffer(80);
        // Display the message only if it passes the level
        // filter.
        if (level <= _level) {
            // Put together the message line:
            // MM/DD/YYYY hh:mm:ss name message
            // where:
            // + hh is in military time
            // + name is 15 characters
            // + message is at most 44 characters.
            timestamp = _timestampFormat.format(currTime);

            output.append(timestamp + " ");

            // Make certain that the object name does not exceed
            // max size.
            if ((stringLength = name.length()) > NAME_LEN) {
                stringLength = NAME_LEN;
            }

            output.append(name.substring(0, stringLength));

            // Pad to the message field's beginning.
            output.append(_padding.substring(0, NAME_LEN - stringLength + 1));

            output.append(message);
            output.append(_newLine);

            // Put the log message up on display.
            _messageArea.append(output.toString());
        }

        return;
    }

    public void setLevel(int level) {
        _level = level;
        return;
    }
}
