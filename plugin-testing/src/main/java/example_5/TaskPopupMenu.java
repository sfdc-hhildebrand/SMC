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
//  TaskPopupMenu.java
//
// Description
//  Clicking on a TaskTable row causes this pop-up menu to
//  appear. The menu allows the user to suspend, block, unblock
//  or delete a task.
//
// RCS ID
// $Id: TaskPopupMenu.java,v 1.5 2007/02/21 13:41:04 cwrapp Exp $
//
// CHANGE LOG
// $Log: TaskPopupMenu.java,v $
// Revision 1.5  2007/02/21 13:41:04  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:15:13  charlesr
// Initial revision
//

package example_5;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class TaskPopupMenu implements ActionListener {
    // Member Methods

    // This pop-up menu was clicked over this task.
    String     _taskName;

    // GUI components.
    JPopupMenu _menu;

    JMenuItem  _suspendItem;

    JMenuItem  _blockItem;

    // Member Data

    JMenuItem  _unblockItem;

    JMenuItem  _deleteItem;

    public TaskPopupMenu() {
        _taskName = null;

        _menu = new JPopupMenu();

        // Add items to the task pop-up menu.
        _suspendItem = new JMenuItem("Suspend");
        _suspendItem.addActionListener(this);
        _suspendItem.setEnabled(false);
        _menu.add(_suspendItem);

        _blockItem = new JMenuItem("Block");
        _blockItem.addActionListener(this);
        _blockItem.setEnabled(false);
        _menu.add(_blockItem);

        _unblockItem = new JMenuItem("Unblock");
        _unblockItem.addActionListener(this);
        _unblockItem.setEnabled(false);
        _menu.add(_unblockItem);

        _deleteItem = new JMenuItem("Delete");
        _deleteItem.addActionListener(this);
        _deleteItem.setEnabled(true);
        _menu.add(_deleteItem);

        _menu.setVisible(false);
    }

    // Generate the task message associated with the selected
    // menu item.
    @Override
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem) e.getSource();
        TaskController controller = new TaskController();
        Map<String, Object> args = new HashMap<String, Object>();

        args.put("Task Name", _taskName);
        if (source.getText().equals(_suspendItem.getText()) == true) {
            controller.postMessage("Task Manager", "Suspend Task", args);
        } else if (source.getText().equals(_blockItem.getText()) == true) {
            controller.postMessage("Task Manager", "Block Task", args);
        } else if (source.getText().equals(_unblockItem.getText()) == true) {
            controller.postMessage("Task Manager", "Unblock Task", args);
        } else if (source.getText().equals(_deleteItem.getText()) == true) {
            controller.postMessage("Task Manager", "Delete Task", args);
        }

        return;
    }

    public void activate(String taskName, String status, MouseEvent e) {
        _taskName = taskName;

        // Which buttons are enabled or disabled depends on the
        // task's current state.
        // NOTE: the delete item is always enabled.
        if (status.compareTo("Running") == 0) {
            // If the task is running, enable suspend
            // and block, disable unblock.
            _suspendItem.setEnabled(true);
            _blockItem.setEnabled(true);
            _unblockItem.setEnabled(false);
        } else if (status.compareTo("Suspended") == 0) {
            // If the task is suspended, enable block
            // and disabled suspend and unblock.
            _suspendItem.setEnabled(false);
            _blockItem.setEnabled(true);
            _unblockItem.setEnabled(false);
        } else if (status.compareTo("Blocked") == 0) {
            // If the task is suspended, enable unblock
            // and disabled suspend and block.
            _suspendItem.setEnabled(false);
            _blockItem.setEnabled(false);
            _unblockItem.setEnabled(true);
        }
        // Else if the status is Done, we shouldn't even
        // get here.

        // All is ready. Make the menu visible where the mouse
        // was clicked.
        _menu.show(e.getComponent(), e.getX(), e.getY());
        _menu.setVisible(true);

        return;
    }

    public void deactivate() {
        _taskName = null;
        _menu.setVisible(false);
        return;
    }
}
