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
//  TaskDialog.java
//
// Description
//  Dialog for creating new tasks.
//
// RCS ID
// $Id: TaskDialog.java,v 1.5 2007/02/21 13:40:48 cwrapp Exp $
//
// CHANGE LOG
// $Log: TaskDialog.java,v $
// Revision 1.5  2007/02/21 13:40:48  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:13:26  charlesr
// Initial revision
//

package example_5;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public final class TaskDialog {
    // Member Methods

    // There is only one dialog object. Initialize the task
    // creation dialog only once.
    private static boolean _isInitialized = false;

    // Dialog's GUI components.
    JFrame                 _frame;

    JTextField             _taskNameField;

    JLabel                 _taskNameLabel;

    JSlider                _taskRuntimeSlider;

    JLabel                 _taskRuntimeLabel;

    JSlider                _taskPrioritySlider;

    // Member Data

    JLabel                 _taskPriorityLabel;

    JButton                _okButton;
    JButton                _applyButton;
    JButton                _resetButton;
    JButton                _cancelButton;
    // The dialog's frame title.
    String                 _title;

    // Create the task creation dialog but don't display it.
    // Leave it hidden and make it visible when it is needed.
    // When the user quits the dialog, just hide it again.
    public TaskDialog() {
        // Create actual dialog if it has not been done already.
        if (_isInitialized == false) {
            _isInitialized = true;

            _title = new String("Task Creation");
            createDialog();
        }
    }

    public void activate() {
        _frame.setVisible(true);
        return;
    }

    public void deactivate() {
        resetComponents();
        _frame.setVisible(false);
        return;
    }

    private Component createComponents() {
        // Use a text field for the task name.
        _taskNameField = new JTextField(TaskMessages.NAME_LEN);
        _taskNameLabel = new JLabel("Task Name: ");
        _taskNameLabel.setLabelFor(_taskNameField);
        _taskNameLabel.setVerticalTextPosition(SwingConstants.CENTER);
        _taskNameLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

        // Use a slider for the task runtime.
        _taskRuntimeSlider = new JSlider(SwingConstants.HORIZONTAL, 10, 60, 10);
        _taskRuntimeSlider.setMajorTickSpacing(10);
        _taskRuntimeSlider.setMinorTickSpacing(1);
        _taskRuntimeSlider.setPaintTicks(true);
        _taskRuntimeSlider.setPaintLabels(true);
        _taskRuntimeSlider.setSnapToTicks(true);
        _taskRuntimeLabel = new JLabel("Runtime (secs.): ");
        _taskRuntimeLabel.setLabelFor(_taskRuntimeSlider);
        _taskRuntimeLabel.setVerticalTextPosition(SwingConstants.CENTER);
        _taskRuntimeLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

        // Use a slider for the task priority.
        _taskPrioritySlider = new JSlider(SwingConstants.HORIZONTAL, 0, 10, 0);
        _taskPrioritySlider.setMajorTickSpacing(5);
        _taskPrioritySlider.setMinorTickSpacing(1);
        _taskPrioritySlider.setPaintTicks(true);
        _taskPrioritySlider.setPaintLabels(true);
        _taskPrioritySlider.setSnapToTicks(true);
        _taskPriorityLabel = new JLabel("Task Priority: ");
        _taskPriorityLabel.setLabelFor(_taskPrioritySlider);
        _taskPriorityLabel.setVerticalTextPosition(SwingConstants.CENTER);
        _taskPriorityLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

        // Create four buttons: OK, Apply, Reset and Cancel.
        _okButton = new JButton("OK");
        _okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the values from the components and
                // create a task from them.
                createTask(_taskNameField.getText(),
                           _taskRuntimeSlider.getValue(),
                           _taskPrioritySlider.getValue());

                // Then hide this canvas.
                _frame.setVisible(false);
            }
        });
        _okButton.setEnabled(true);

        _applyButton = new JButton("Apply");
        _applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createTask(_taskNameField.getText(),
                           _taskRuntimeSlider.getValue(),
                           _taskPrioritySlider.getValue());
            }
        });
        _applyButton.setEnabled(true);

        _resetButton = new JButton("Reset");
        _resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetComponents();
            }
        });
        _resetButton.setEnabled(true);

        _cancelButton = new JButton("Cancel");
        _cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deactivate();
            }
        });

        // Use the gridbag layout for the dialog
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gridConstraints = new GridBagConstraints();

        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEmptyBorder(30, // top
                                                       30, // left
                                                       30, // bottom
                                                       30) // right
        );
        pane.setLayout(gridbag);

        // Place components into gridbag and so into the pane.
        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 0;
        gridConstraints.gridwidth = 1;
        gridbag.setConstraints(_taskNameLabel, gridConstraints);
        pane.add(_taskNameLabel);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 0;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(_taskNameField, gridConstraints);
        pane.add(_taskNameField);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 1;
        gridConstraints.gridwidth = 1;
        gridbag.setConstraints(_taskRuntimeLabel, gridConstraints);
        pane.add(_taskRuntimeLabel);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 1;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(_taskRuntimeSlider, gridConstraints);
        pane.add(_taskRuntimeSlider);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 2;
        gridConstraints.gridwidth = 1;
        gridbag.setConstraints(_taskPriorityLabel, gridConstraints);
        pane.add(_taskPriorityLabel);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 2;
        gridConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(_taskPrioritySlider, gridConstraints);
        pane.add(_taskPrioritySlider);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 3;
        gridConstraints.gridwidth = 1;
        gridbag.setConstraints(_okButton, gridConstraints);
        pane.add(_okButton);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 3;
        gridbag.setConstraints(_applyButton, gridConstraints);
        pane.add(_applyButton);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 3;
        gridbag.setConstraints(_resetButton, gridConstraints);
        pane.add(_resetButton);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 3;
        gridConstraints.gridy = 3;
        gridbag.setConstraints(_cancelButton, gridConstraints);
        pane.add(_cancelButton);

        return pane;
    }

    private void createDialog() {
        // Create the dialog's frame.
        _frame = new JFrame(_title);

        // Create the dialog's components (text boxes, sliders
        // and buttons) and place them into the content pane.
        Component contents = createComponents();

        // Add the contents to the frame.
        _frame.getContentPane().add(contents, BorderLayout.CENTER);

        // When this dialog is closed, simply undisplay it.
        _frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                deactivate();
            }
        });

        _frame.pack();

        // This dialog is hidden until the "Task Create..."
        // button is clicked.
        _frame.setVisible(false);

        return;
    }

    private void createTask(String name, int runtime, int priority) {
        Map<String, Object> args = new HashMap<String, Object>();
        TaskController control = new TaskController();

        args.put("Task Name", name);
        args.put("Runtime", new Integer(runtime));
        args.put("Priority", new Integer(priority));
        control.postMessage("Task Manager", "Create Task", args);
        return;
    }

    private void resetComponents() {
        _taskNameField.setText(null);
        _taskRuntimeSlider.setValue(10);
        _taskPrioritySlider.setValue(0);

        return;
    }
}
