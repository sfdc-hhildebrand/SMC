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
//  Traffic.java
//
// Description
//  Creates the main frame and starts the demo running.
//
// RCS ID
// $Id: Traffic.java,v 1.5 2005/05/28 13:51:24 cwrapp Exp $
//
// CHANGE LOG
// $Log: Traffic.java,v $
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:02:59  charlesr
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
import javax.swing.UIManager;

public final class Traffic {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        }

        // Create the top-level container and add contents to it.
        JFrame frame = new JFrame("Stoplight Demo");
        Traffic app = new Traffic();
        Component contents = app.createComponents();
        frame.getContentPane().add(contents, BorderLayout.CENTER);

        //Finish setting up the frame, and show it.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible(true);
    }

    // Member data.
    private TrafficCanvas _trafficCanvas;

    private ConfigDialog  _configDialog;

    private JButton       _configButton;

    private JButton       _startButton;
    private JButton       _pauseButton;
    private JButton       _continueButton;
    private JButton       _stopButton;

    // Member functions.
    public Traffic() {
    }

    public Component createComponents() {
        // Paint the traffic demo on this canvas.
        _trafficCanvas = new TrafficCanvas();

        // Put the canvas into a separate pane.
        JPanel canvasPane = new JPanel();
        canvasPane.setBorder(BorderFactory.createRaisedBevelBorder());
        canvasPane.add(_trafficCanvas);

        // Create the configuration dialog.
        _configDialog = new ConfigDialog(_trafficCanvas);

        // This button causes the configuration dialogue to be
        // displayed.
        _configButton = new JButton("Configure...");
        _configButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _configDialog.activate();
            }
        });
        _configButton.setEnabled(true);

        // This button starts the demo running.
        _startButton = new JButton("Start");
        _startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Have the canvas do its thing.
                _trafficCanvas.startDemo();

                // Now enable the pause and stop buttons and
                // disable the start button.
                _startButton.setEnabled(false);
                _pauseButton.setEnabled(true);
                _stopButton.setEnabled(true);
            }
        });
        _startButton.setEnabled(true);

        // This button pauses the demo.
        _pauseButton = new JButton("Pause");
        _pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Tell the canvas to pause its demo.
                _trafficCanvas.pauseDemo();

                // Now enable the continue button and
                // disable the pause button.
                _pauseButton.setEnabled(false);
                _continueButton.setEnabled(true);
            }
        });
        _pauseButton.setEnabled(false);

        // This button continues the paused demo.
        _continueButton = new JButton("Continue");
        _continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _trafficCanvas.continueDemo();

                // Enable the pause button and disable the
                // continue button.
                _continueButton.setEnabled(false);
                _pauseButton.setEnabled(true);
            }
        });
        _continueButton.setEnabled(false);

        // This buttons stops the demo.
        _stopButton = new JButton("Stop");
        _stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _trafficCanvas.stopDemo();

                // Enable the start button and disable the pause
                // and stop buttons.
                _stopButton.setEnabled(false);
                _pauseButton.setEnabled(false);
                _continueButton.setEnabled(false);
                _startButton.setEnabled(true);
            }
        });
        _stopButton.setEnabled(false);

        /*
         * An easy way to put space between a top-level container
         * and its contents is to put the contents in a JPanel
         * that has an "empty" border.
         */
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gridConstraints = new GridBagConstraints();

        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEmptyBorder(30, //top
                                                       30, //left
                                                       30, //bottom
                                                       30) //right
        );
        pane.setLayout(gridbag);

        // Set the button's grid constrains and then add it to
        // the pane.
        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 3;
        gridConstraints.gridy = 0;
        gridConstraints.gridwidth = 1;
        gridbag.setConstraints(_configButton, gridConstraints);
        pane.add(_configButton);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.fill = GridBagConstraints.BOTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 1;
        gridConstraints.gridwidth = 4;
        gridbag.setConstraints(canvasPane, gridConstraints);
        pane.add(canvasPane);

        gridConstraints.anchor = GridBagConstraints.SOUTH;
        gridConstraints.fill = GridBagConstraints.NONE;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 2;
        gridbag.setConstraints(_startButton, gridConstraints);
        pane.add(_startButton);

        gridConstraints.gridx = 1;
        gridConstraints.gridy = 2;
        gridbag.setConstraints(_pauseButton, gridConstraints);
        pane.add(_pauseButton);

        gridConstraints.gridx = 2;
        gridConstraints.gridy = 2;
        gridbag.setConstraints(_continueButton, gridConstraints);
        pane.add(_continueButton);

        gridConstraints.gridx = 3;
        gridConstraints.gridy = 2;
        gridbag.setConstraints(_stopButton, gridConstraints);
        pane.add(_stopButton);

        return pane;
    }
}
