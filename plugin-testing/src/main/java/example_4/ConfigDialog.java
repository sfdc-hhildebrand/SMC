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
//  ConfigDialog.java
//
// Description
//  Constructs and handles the demo's configuration dialog.
//
// RCS ID
// $Id: ConfigDialog.java,v 1.5 2005/05/28 13:51:24 cwrapp Exp $
//
// CHANGE LOG
// $Log: ConfigDialog.java,v $
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:01:26  charlesr
// Initial revision
//

package example_4;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public final class ConfigDialog {
    // Member methods

    //========================================
    // Static Data
    //
    private static boolean       _isInitialized = false;

    private static TrafficCanvas _canvas;

    private static JFrame        _frame;

    private static JSlider       _nsSlider;

    private static JSlider       _ewSlider;

    private static JSlider       _yellowSlider;

    // Member data

    private static JSlider       _vehicleSlider;
    private static JSlider       _speedSlider;
    private static JButton       _okButton;
    private static JButton       _applyButton;
    private static JButton       _resetButton;
    private static JButton       _cancelButton;
    static {
        _canvas = null;
        _frame = null;
        _nsSlider = null;
        _ewSlider = null;
        _yellowSlider = null;
        _vehicleSlider = null;
        _speedSlider = null;
        _okButton = null;
        _applyButton = null;
        _resetButton = null;
        _cancelButton = null;
    }

    // Create the demo configuration dialog but don't display it.
    // Leave it hidden and make it visible when it is needed.
    // When the user quits the dialog, just hide it again.
    public ConfigDialog(TrafficCanvas canvas) {
        // Create actual dialog if it has not been done already.
        if (_isInitialized == false) {
            _isInitialized = true;

            // This dialog works with the canvas.
            _canvas = canvas;

            new String("Demo Configuration");

            createDialogue();
        }
    }

    public void activate() {
        _frame.setVisible(true);
        return;
    }

    public void deactivate() {
        _frame.setVisible(false);
        return;
    }

    private Component createComponents() {
        Border loweredbevel;
        TitledBorder titledBorder;

        // Use this border to create, in turn, titled borders.
        loweredbevel = BorderFactory.createLoweredBevelBorder();

        // Create configuration sliders.
        // There are sliders for setting:
        //  + North/South green light duration.
        //  + East/West green light duration.
        //  + Yellow light duration.
        //  + Vehicle appearance rate.
        //  + Vehicle speed.
        titledBorder = BorderFactory.createTitledBorder(loweredbevel,
                                                        "N/S green light duration (seconds)");
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        titledBorder.setTitlePosition(TitledBorder.ABOVE_TOP);
        _nsSlider = new JSlider(SwingConstants.HORIZONTAL, 5, 20, 5);
        _nsSlider.setMajorTickSpacing(5);
        _nsSlider.setMinorTickSpacing(1);
        _nsSlider.setPaintTicks(true);
        _nsSlider.setPaintLabels(true);
        _nsSlider.setSnapToTicks(true);
        _nsSlider.setBorder(titledBorder);

        titledBorder = BorderFactory.createTitledBorder(loweredbevel,
                                                        "E/W green light duration (seconds)");
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        titledBorder.setTitlePosition(TitledBorder.ABOVE_TOP);
        _ewSlider = new JSlider(SwingConstants.HORIZONTAL, 5, 20, 5);
        _ewSlider.setMajorTickSpacing(5);
        _ewSlider.setMinorTickSpacing(1);
        _ewSlider.setPaintTicks(true);
        _ewSlider.setPaintLabels(true);
        _ewSlider.setSnapToTicks(true);
        _ewSlider.setBorder(titledBorder);

        titledBorder = BorderFactory.createTitledBorder(loweredbevel,
                                                        "Yellow light duration (seconds)");
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        titledBorder.setTitlePosition(TitledBorder.ABOVE_TOP);
        _yellowSlider = new JSlider(SwingConstants.HORIZONTAL, 2, 8, 2);
        _yellowSlider.setMajorTickSpacing(2);
        _yellowSlider.setMinorTickSpacing(1);
        _yellowSlider.setPaintTicks(true);
        _yellowSlider.setPaintLabels(true);
        _yellowSlider.setSnapToTicks(true);
        _yellowSlider.setBorder(titledBorder);

        titledBorder = BorderFactory.createTitledBorder(loweredbevel,
                                                        "Vehicle appearance rate (seconds)");
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        titledBorder.setTitlePosition(TitledBorder.ABOVE_TOP);
        _vehicleSlider = new JSlider(SwingConstants.HORIZONTAL, 5, 15, 5);
        _vehicleSlider.setMajorTickSpacing(2);
        _vehicleSlider.setMinorTickSpacing(1);
        _vehicleSlider.setPaintTicks(true);
        _vehicleSlider.setPaintLabels(true);
        _vehicleSlider.setBorder(titledBorder);
        _vehicleSlider.setSnapToTicks(true);

        titledBorder = BorderFactory.createTitledBorder(loweredbevel,
                                                        "Vehicle speed (in pixels/refresh)");
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        titledBorder.setTitlePosition(TitledBorder.ABOVE_TOP);
        _speedSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 5, 1);
        _speedSlider.setMajorTickSpacing(1);
        _speedSlider.setMinorTickSpacing(1);
        _speedSlider.setPaintTicks(false);
        _speedSlider.setPaintLabels(true);
        _speedSlider.setSnapToTicks(true);
        _speedSlider.setBorder(titledBorder);

        // Create the OK, Apply and Cancel buttons.
        _okButton = new JButton("OK");
        _okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the values from the sliders and
                // update the canvas with them.
                updateCanvas();

                // Then hide this canvas.
                _frame.setVisible(false);
            }
        });
        // Enable this button only after a slider has changed.
        _okButton.setEnabled(true);

        _applyButton = new JButton("Apply");
        _applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCanvas();
            }
        });
        // Enable this button only after a slider has changed.
        _applyButton.setEnabled(true);

        _resetButton = new JButton("Reset");
        _resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSliders();
            }
        });
        _resetButton.setEnabled(true);

        _cancelButton = new JButton("Cancel");
        _cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSliders();

                _frame.setVisible(false);
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

        // Place buttons and sliders into gridbag and so into the
        // pane.
        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 0;
        gridConstraints.gridwidth = 4;
        gridbag.setConstraints(_nsSlider, gridConstraints);
        pane.add(_nsSlider);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 1;
        gridbag.setConstraints(_ewSlider, gridConstraints);
        pane.add(_ewSlider);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 3;
        gridbag.setConstraints(_yellowSlider, gridConstraints);
        pane.add(_yellowSlider);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 4;
        gridbag.setConstraints(_vehicleSlider, gridConstraints);
        pane.add(_vehicleSlider);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 5;
        gridbag.setConstraints(_speedSlider, gridConstraints);
        pane.add(_speedSlider);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 6;
        gridConstraints.gridwidth = 1;
        gridbag.setConstraints(_okButton, gridConstraints);
        pane.add(_okButton);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 6;
        gridbag.setConstraints(_applyButton, gridConstraints);
        pane.add(_applyButton);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 6;
        gridbag.setConstraints(_resetButton, gridConstraints);
        pane.add(_resetButton);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.gridx = 3;
        gridConstraints.gridy = 6;
        gridbag.setConstraints(_cancelButton, gridConstraints);
        pane.add(_cancelButton);

        return pane;
    }

    private void createDialogue() {
        // Create the dialog's frame.
        _frame = new JFrame();

        // Create the dialog's sliders, borders and button
        // components and place them into the content pane.
        Component contents = createComponents();

        // Add the content components to the frame.
        _frame.getContentPane().add(contents, BorderLayout.CENTER);

        // When this dialog is closed, simply undisplay it.
        _frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Set the sliders to the current app
                // configuration.
                setSliders();

                // Treat this like a cancel. Hide the frame.
                _frame.setVisible(false);
            }
        });

        _frame.pack();

        // This dialog is hidden until the app's configure button
        // is clicked.
        _frame.setVisible(false);

        // Set the sliders to their original values.
        setSliders();

        return;
    }

    private void setSliders() {
        // Get the current timer settings and set the sliders
        // accordingly.
        _nsSlider.setValue(_canvas.getLightDuration(TrafficCanvas.NS_LIGHT));
        _ewSlider.setValue(_canvas.getLightDuration(TrafficCanvas.EW_LIGHT));
        _yellowSlider.setValue(_canvas.getLightDuration(TrafficCanvas.YELLOW_LIGHT));
        _vehicleSlider.setValue(_canvas.getNewVehicleRate());
        _speedSlider.setValue(_canvas.getVehicleSpeed());

        return;
    }

    private void updateCanvas() {
        // Get the slider settings and set the demo timers
        // accordingly.
        _canvas.setLightDuration(TrafficCanvas.NS_LIGHT, _nsSlider.getValue());
        _canvas.setLightDuration(TrafficCanvas.EW_LIGHT, _ewSlider.getValue());
        _canvas.setLightDuration(TrafficCanvas.YELLOW_LIGHT,
                                 _yellowSlider.getValue());
        _canvas.setNewVehicleRate(_vehicleSlider.getValue());
        _canvas.setVehicleSpeed(_speedSlider.getValue());

        return;
    }
}
