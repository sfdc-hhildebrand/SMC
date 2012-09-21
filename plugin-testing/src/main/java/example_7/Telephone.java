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
//  Telephone.java
//
// Description
//  A simulation of an old fashioned touch-tone telephone.
//
// RCS ID
// $Id: Telephone.java,v 1.7 2009/03/27 09:41:46 cwrapp Exp $
//
// CHANGE LOG
// $Log: Telephone.java,v $
// Revision 1.7  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.6  2009/03/01 18:20:39  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.5  2007/02/21 13:45:08  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:22:40  charlesr
// Initial revision
//

package example_7;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIManager;

public final class Telephone {
    // Member methods.

    // When playing a series of audio clips, it is neccessary
    // to wait for one clip to finish before starting the next.
    // Since Java audio clips don't know how long they are, it
    // is necessary to store with each sound clip its duration.
    private final class AudioData {
        // Member methods.

        private URL       _url;

        private AudioClip _clip;

        private long      _duration;

        private AudioData(URL url, long duration) {
            _url = url;
            _clip = null;
            _duration = duration;
        }

        // Member data.

        public void loop() {
            if (_clip != null || (_clip = Applet.newAudioClip(_url)) != null) {
                _clip.loop();
            }

            return;
        }

        public void play() throws InterruptedException {
            if (_clip != null || (_clip = Applet.newAudioClip(_url)) != null) {
                _clip.play();

                try {
                    Thread.sleep(_duration);
                } catch (InterruptedException interrupt) {
                    // Stop the audio clip before rethrowing the
                    // intettupt.
                    _clip.stop();
                    _clip = null;

                    throw interrupt;
                }
            }

            return;
        }

        public void stop() {
            if (_clip != null) {
                _clip.stop();
                _clip = null;
            }

            return;
        }
    }

    // Call routing needs to be done asynchronouzly in order to
    // avoid issuing a transition within a transition.
    private final class CallRoutingThread extends Thread {
        // Member methods.

        private int       _callType;

        private String    _areaCode;

        // Member data.

        private String    _exchange;
        private String    _local;
        private Telephone _owner;

        private CallRoutingThread(int callType, String areaCode,
                                  String exchange, String local, Telephone owner) {
            _callType = callType;
            _areaCode = new String(areaCode);
            _exchange = new String(exchange);
            _local = new String(local);
            _owner = owner;
        }

        @Override
        public void run() {
            int route;

            if (_callType == Telephone.EMERGENCY) {
                route = Telephone.EMERGENCY;
            } else if (_callType == Telephone.LONG_DISTANCE
                       && _areaCode.compareTo("1212") == 0
                       && _exchange.compareTo("555") == 0
                       && _local.compareTo("1234") == 0) {
                route = Telephone.NYC_TEMP;
            } else if (_exchange.compareTo("555") == 0) {
                if (_local.compareTo("1212") == 0) {
                    route = Telephone.TIME;
                } else {
                    route = Telephone.LINE_BUSY;
                }
            } else if (_callType == Telephone.LOCAL) {
                route = Telephone.DEPOSIT_MONEY;
            } else {
                route = Telephone.INVALID_NUMBER;
            }

            _areaCode = null;
            _exchange = null;
            _local = null;

            // There is a race condition between this thread
            // and the main thread which contains the FSM.
            // Apparently this thread can complete while the
            // FSM is still in transition, causing an exception.
            // so sleep a bit before issuing the callback.
            try {
                Thread.sleep(1);
            } catch (InterruptedException interrupt) {
                // Ignore.
            }

            _owner._callRoute(route);

            return;
        }
    }

    // Play several audio clips, one after another in a
    // separate thread.
    private final class PlaybackThread extends Thread {
        // Member methods.

        private List<AudioData> _audioList;

        private Telephone       _owner;

        private Thread          _thread;

        // Member data.

        private PlaybackThread(List<AudioData> audioList, Telephone owner) {
            _audioList = audioList;
            _owner = owner;
            _thread = null;
        }

        public void halt() {
            // Since the telephone object is telling us to
            // stop, don't tell the telephone that we are
            // stopped.
            _owner = null;

            _thread.interrupt();

            return;
        }

        @Override
        public void run() {
            Iterator<AudioData> it;
            AudioData clip = null;

            _thread = Thread.currentThread();

            try {
                for (it = _audioList.iterator(); it.hasNext() == true;) {
                    clip = it.next();

                    clip.play();
                    clip = null;
                }
            } catch (InterruptedException interrupt) {
            }

            // Stop the currently playing sound.
            if (clip != null) {
                clip.stop();
            }

            _audioList.clear();

            if (_owner != null) {
                _owner._playbackDone(this);
            }

            return;
        }
    }

    private final class TelephoneTimer extends TimerTask {
        // Member methods.

        private String    _name;

        private long      _delay;

        private Telephone _owner;

        // Member data.

        private TelephoneTimer(String name, long delay, Telephone owner) {
            _name = name;
            _delay = delay;
            _owner = owner;
        }

        @Override
        public void run() {
            _owner.issueTimeout(_name);
        }

        private long getDelay() {
            return _delay;
        }
    }

    //-----------------------------------------------------------
    // State Machine Actions.
    //

    // Display the "telephone" user interface and run until
    // the user quits the window.
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        }

        // Create the top-level container and add contents to it.
        JFrame frame = new JFrame("Telephone Demo");
        Telephone telephone = new Telephone(frame.getContentPane());

        telephone.start();

        // Finish up setting the frame and show it.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // The window is going away NOW.
                // Just exit.
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible(true);
    }

    // The telphone state machine.
    private TelephoneContext            _fsm;

    // The type of call being dialed.
    private int                         _callType;

    // Store dialed numbers here.
    private String                      _areaCode;

    private String                      _exchange;

    private String                      _local;

    private String                      _display;

    // Display the dialed digits here.
    private JTextField                  _numberDisplay;

    // This button is used to pick-up/put-down the receiver.
    private JButton                     _receiverButton;

    // Dialing buttons.
    private JButton[]                   _dialButtons;

    // Zounds! It's sounds!
    private AudioClip[]                 _dtmf;

    private Map<String, AudioData>      _audioMap;

    private PlaybackThread              _playbackThread;

    // Timer objects.
    private Map<String, TelephoneTimer> _timerMap;

    private static Timer                _timer;

    private static Map<String, Method>  _timerTransitionMap;

    // The telephone's time display.
    private static SimpleDateFormat     _ClockFormatter   = null;

    // Constants.
    public static final int             LONG_DISTANCE     = 1;

    public static final int             LOCAL             = 2;

    public static final int             EMERGENCY         = 3;

    private static final int            NYC_TEMP          = 4;

    private static final int            TIME              = 5;

    private static final int            DEPOSIT_MONEY     = 6;

    private static final int            LINE_BUSY         = 7;

    private static final int            INVALID_NUMBER    = 8;

    private static final long           MILLIS_PER_MINUTE = 60000;

    static {
        _timer = new Timer(true);
        _timerTransitionMap = new HashMap<String, Method>();

        _ClockFormatter = new SimpleDateFormat("    HH:mm a    MMMM dd, yyyy");

        // Fill in the static associations between timer names
        // and their transition.
        Class<?> context = TelephoneContext.class;
        Class<?>[] parameters = new Class[0];

        try {
            _timerTransitionMap.put("ClockTimer",
                                    context.getDeclaredMethod("ClockTimer",
                                                              parameters));
            _timerTransitionMap.put("OffHookTimer",
                                    context.getDeclaredMethod("OffHookTimer",
                                                              parameters));
            _timerTransitionMap.put("LoopTimer",
                                    context.getDeclaredMethod("LoopTimer",
                                                              parameters));
            _timerTransitionMap.put("RingTimer",
                                    context.getDeclaredMethod("RingTimer",
                                                              parameters));
        } catch (NoSuchMethodException nsmex) {
            nsmex.printStackTrace(System.out);
        }
    }

    public Telephone(Container pane) {
        _areaCode = new String();
        _exchange = new String();
        _local = new String();
        _display = new String();

        _receiverButton = null;
        _dialButtons = new JButton[12];

        _timerMap = new HashMap<String, TelephoneTimer>();

        _playbackThread = null;

        _loadSounds();
        _loadUI(pane);

        // Create the state machine to drive this object.
        _fsm = new TelephoneContext(this);

        // DEBUG
        // _fsm.setDebugFlag(true);
    }

    public void addDisplay(String character) {
        _display += character;
        _numberDisplay.setText(_display);

        return;
    }

    public void clearDisplay() {
        // Clear the internal data store.
        _display = "";
        _areaCode = "";
        _local = "";
        _exchange = "";

        // Put up the current time and date on the display.
        _numberDisplay.setText("");

        return;
    }

    // Return the current area code.
    public String getAreaCode() {
        return _areaCode;
    }

    // Return the exchange.
    public String getExchange() {
        return _exchange;
    }

    // Return the local number.
    public String getLocal() {
        return _local;
    }

    public int getType() {
        return _callType;
    }

    public void issueTimeout(String timer) {
        TelephoneTimer task = _timerMap.remove(timer);

        if (task != null && _fsm != null) {
            try {
                Method transition = _timerTransitionMap.get(timer);
                Object[] args;

                if (transition != null) {
                    args = new Object[0];
                    transition.invoke(_fsm, args);
                }
            } catch (IllegalAccessException accex) {
            } catch (IllegalArgumentException argex) {
            } catch (InvocationTargetException targetex) {
            }
        }

        return;
    }

    // Member data.

    public void loop(String name) {
        AudioData audioData = _audioMap.get(name);

        if (audioData != null) {
            audioData.loop();
        } else {
            System.err.println("There is no audio clip named \"" + name + "\".");
        }

        return;
    }

    // Convert a string to a number. Return -1 if the parse
    // fails.
    public int parseInt(String n) {
        int retval;

        try {
            retval = Integer.parseInt(n);
        } catch (NumberFormatException formex) {
            retval = -1;
        }

        return retval;
    }

    public void play(String name) {
        AudioData audioData = _audioMap.get(name);

        if (audioData != null) {
            try {
                audioData.play();
            } catch (InterruptedException interrupt) {
            }
        } else {
            System.err.println("There is no audio clip named \"" + name + "\".");
        }

        return;
    }

    public void playDepositMoney() {
        AudioData audioData;
        List<AudioData> audioList = new LinkedList<AudioData>();

        audioData = _audioMap.get("50_cents_please");
        audioList.add(audioData);

        _playbackThread = new PlaybackThread(audioList, this);
        _playbackThread.setDaemon(true);
        _playbackThread.start();

        return;
    }

    public void playEmergency() {
        AudioData audioData;
        List<AudioData> audioList = new LinkedList<AudioData>();

        audioData = _audioMap.get("911");
        audioList.add(audioData);

        _playbackThread = new PlaybackThread(audioList, this);
        _playbackThread.setDaemon(true);
        _playbackThread.start();

        return;
    }

    public void playInvalidNumber() {
        AudioData audioData;
        List<AudioData> audioList = new LinkedList<AudioData>();

        audioData = _audioMap.get("you_dialed");
        audioList.add(audioData);

        _soundPhoneNumber(audioList);

        audioData = _audioMap.get("could_not_be_completed");
        audioList.add(audioData);

        // Play the message in a separate thread.
        _playbackThread = new PlaybackThread(audioList, this);
        _playbackThread.setDaemon(true);
        _playbackThread.start();

        return;
    }

    public void playNYCTemp() {
        AudioData audioData;
        List<AudioData> audioList = new LinkedList<AudioData>();

        audioData = _audioMap.get("NYC_temp");
        audioList.add(audioData);

        _playbackThread = new PlaybackThread(audioList, this);
        _playbackThread.setDaemon(true);
        _playbackThread.start();

        return;
    }

    public void playTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int am_pm = calendar.get(Calendar.AM_PM);
        AudioData clip;
        List<AudioData> clipList = new LinkedList<AudioData>();

        clip = _audioMap.get("the_time_is");
        clipList.add(clip);

        // 1. Read the hour.
        clip = _audioMap.get(Integer.toString(hour));
        clipList.add(clip);

        // Is this on the hour exactly?
        if (minute == 0 && seconds == 0) {
            clip = _audioMap.get("oclock");
            clipList.add(clip);

            _soundMeridian(am_pm, clipList);

            clip = _audioMap.get("exactly");
            clipList.add(clip);
        } else {
            // 2. Read the minute.
            _soundNumber(minute, true, clipList);

            _soundMeridian(am_pm, clipList);

            // 3. Read the seconds.
            if (seconds == 0) {
                clip = _audioMap.get("exactly");
                clipList.add(clip);
            } else {
                clip = _audioMap.get("and");
                clipList.add(clip);

                _soundNumber(seconds, false, clipList);

                if (seconds == 1) {
                    clip = _audioMap.get("second");
                } else {
                    clip = _audioMap.get("seconds");
                }
                clipList.add(clip);
            }
        }

        _playbackThread = new PlaybackThread(clipList, this);
        _playbackThread.setDaemon(true);
        _playbackThread.start();

        return;
    }

    public void playTT(String name) {
        int n;

        try {
            n = Integer.parseInt(name);
            if (_dtmf[n] != null) {
                _dtmf[n].play();
            } else {
                System.err.println("There is no audio clip named \"dtmf_"
                                   + name + "\".");
            }
        } catch (NumberFormatException formex) {
        }

        return;
    }

    public void resetTimer(String name) {
        TelephoneTimer task = _timerMap.get(name);
        long delay;

        if (task != null) {
            delay = task.getDelay();
            task.cancel();
            startTimer(name, delay);
        }

        return;
    }

    // Use a separate thread to route the call asynchronously.
    public void routeCall(int callType, String areaCode, String exchange,
                          String local) {
        CallRoutingThread thread = new CallRoutingThread(callType, areaCode,
                                                         exchange, local, this);

        thread.start();

        return;
    }

    public void saveAreaCode(String n) {
        _areaCode += n;
        addDisplay(n);

        return;
    }

    public void saveExchange(String n) {
        _exchange += n;
        addDisplay(n);

        return;
    }

    public void saveLocal(String n) {
        _local += n;
        addDisplay(n);

        return;
    }

    public void setReceiver(String command, String text) {
        if (_receiverButton != null) {
            _receiverButton.setActionCommand(command);
            _receiverButton.setText(text);
        }

        return;
    }

    public void setType(int type) {
        _callType = type;
        return;
    }

    public void start() {
        _fsm.enterStartState();
        return;
    }

    public void startClockTimer() {
        long currentTime = System.currentTimeMillis();
        long timeRemaining = MILLIS_PER_MINUTE - currentTime
                             % MILLIS_PER_MINUTE;

        // Figure out how long until the top of the minute
        // and set the timer for that amount.
        startTimer("ClockTimer", timeRemaining);

        return;
    }

    public void startTimer(String name, long delay) {
        TelephoneTimer task = new TelephoneTimer(name, delay, this);

        _timerMap.put(name, task);
        _timer.schedule(task, delay);

        return;
    }

    public void stopLoop(String name) {
        AudioData audioData = _audioMap.get(name);

        if (audioData != null) {
            audioData.stop();
        } else {
            System.err.println("There is no audio clip named \"" + name + "\".");
        }

        return;
    }

    public void stopPlayback() {
        if (_playbackThread != null) {
            _playbackThread.halt();
            _playbackThread = null;
        }

        return;
    }

    public void stopTimer(String name) {
        TelephoneTimer task = _timerMap.remove(name);

        if (task != null) {
            task.cancel();
        }

        return;
    }

    public void updateClock() {
        _numberDisplay.setText(_ClockFormatter.format(new Date()));
        return;
    }

    private void _callRoute(int route) {
        switch (route) {
            case EMERGENCY:
                _fsm.Emergency();
                break;

            case NYC_TEMP:
                _fsm.NYCTemp();
                break;

            case TIME:
                _fsm.Time();
                break;

            case DEPOSIT_MONEY:
                _fsm.DepositMoney();
                break;

            case LINE_BUSY:
                _fsm.LineBusy();
                break;

            case INVALID_NUMBER:
                _fsm.InvalidNumber();
                break;
        }

        return;
    }

    private void _loadSounds() {
        String directory = "/sounds/";
        String urlString = "";
        URL soundURL;
        AudioData audioData;
        int i;

        // Load in the touch tone clips.
        _dtmf = new AudioClip[12];
        for (i = 0; i < 12; ++i) {
            urlString = directory + "touch_tone_" + Integer.toString(i) + ".au";
            soundURL = getClass().getResource(urlString);
            _dtmf[i] = Applet.newAudioClip(soundURL);
        }

        // Load in sound URLs. 
        _audioMap = new HashMap<String, AudioData>();

        urlString = directory + "ring.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 4000);
        _audioMap.put("ringing", audioData);

        urlString = directory + "dialtone.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("dialtone", audioData);

        urlString = directory + "busy_signal.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("busy", audioData);

        urlString = directory + "fast_busy_signal.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("fast_busy", audioData);

        urlString = directory + "phone_off_hook.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 500);
        _audioMap.put("phone_off_hook", audioData);

        urlString = directory + "911.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 13000);
        _audioMap.put("911", audioData);

        urlString = directory + "and.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 750);
        _audioMap.put("and", audioData);

        urlString = directory + "50_cents_please.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 6000);
        _audioMap.put("50_cents_please", audioData);

        urlString = directory + "could_not_be_completed.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 4000);
        _audioMap.put("could_not_be_completed", audioData);

        urlString = directory + "exactly.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("exactly", audioData);

        urlString = directory + "oclock.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("oclock", audioData);

        urlString = directory + "oh.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("oh", audioData);

        urlString = directory + "second.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1200);
        _audioMap.put("second", audioData);

        urlString = directory + "seconds.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1400);
        _audioMap.put("seconds", audioData);

        urlString = directory + "the_number_you_have_dialed.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1800);
        _audioMap.put("you_dialed", audioData);

        urlString = directory + "the_time_is.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1100);
        _audioMap.put("the_time_is", audioData);

        urlString = directory + "nyctemp.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 18500);
        _audioMap.put("NYC_temp", audioData);

        urlString = directory + "AM.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("AM", audioData);

        urlString = directory + "PM.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("PM", audioData);

        urlString = directory + "0.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("0", audioData);

        urlString = directory + "1.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 900);
        _audioMap.put("1", audioData);

        urlString = directory + "2.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 900);
        _audioMap.put("2", audioData);

        urlString = directory + "3.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("3", audioData);

        urlString = directory + "4.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 900);
        _audioMap.put("4", audioData);

        urlString = directory + "5.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 900);
        _audioMap.put("5", audioData);

        urlString = directory + "6.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 850);
        _audioMap.put("6", audioData);

        urlString = directory + "7.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("7", audioData);

        urlString = directory + "8.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("8", audioData);

        urlString = directory + "9.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 900);
        _audioMap.put("9", audioData);

        urlString = directory + "10.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 750);
        _audioMap.put("10", audioData);

        urlString = directory + "11.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("11", audioData);

        urlString = directory + "12.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("12", audioData);

        urlString = directory + "13.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("13", audioData);

        urlString = directory + "14.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("14", audioData);

        urlString = directory + "15.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("15", audioData);

        urlString = directory + "16.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("16", audioData);

        urlString = directory + "17.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1500);
        _audioMap.put("17", audioData);

        urlString = directory + "18.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("18", audioData);

        urlString = directory + "19.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("19", audioData);

        urlString = directory + "20.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1250);
        _audioMap.put("20", audioData);

        urlString = directory + "30.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("30", audioData);

        urlString = directory + "40.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("40", audioData);

        urlString = directory + "50.au";
        soundURL = getClass().getResource(urlString);
        audioData = new AudioData(soundURL, 1000);
        _audioMap.put("50", audioData);

        return;
    }

    // Create the user interface but don't display it yet.
    private void _loadUI(Container pane) {
        String buttonText;
        String buttonCommand;
        int i;

        // Create the read-only phone number display.
        _numberDisplay = new JTextField(20);
        _numberDisplay.setEditable(false);
        _numberDisplay.setFont(new Font(null, Font.PLAIN, 10));

        // Create the off-hook/on-hook button.
        _receiverButton = new JButton("Pick up receiver ");
        _receiverButton.setActionCommand("off hook");
        _receiverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();

                if (command.compareTo("off hook") == 0) {
                    _fsm.OffHook();
                } else if (command.compareTo("on hook") == 0) {
                    _fsm.OnHook();
                } else {
                    System.out.println("Unknown receiver command: \"" + command
                                       + "\".");
                }

                return;
            }
        });
        _receiverButton.setEnabled(true);

        // Create the dialing buttons.
        for (i = 0; i < 12; ++i) {
            buttonCommand = Integer.toString(i);

            if (i < 10) {
                buttonText = buttonCommand;
            } else if (i == 10) {
                buttonText = "*";
            } else {
                buttonText = "#";
            }

            _dialButtons[i] = new JButton(buttonText);
            _dialButtons[i].setActionCommand(buttonCommand);
            _dialButtons[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    _fsm.Digit(e.getActionCommand());
                }
            });
            _dialButtons[i].setEnabled(true);
        }

        // Layout the components.
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gridConstraints = new GridBagConstraints();
        pane.setLayout(gridbag);

        // Put the number display at the top.
        gridConstraints.insets = new Insets(2, 2, 2, 2);

        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 0;
        gridConstraints.gridwidth = 3;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 5;
        gridConstraints.ipady = 5;
        gridConstraints.weighty = 1.0;
        gridbag.setConstraints(_numberDisplay, gridConstraints);
        pane.add(_numberDisplay);

        // The pick up/put down receiver button spans all columns
        // and two rows. It also sucks up all new verticle space.
        gridConstraints.anchor = GridBagConstraints.NORTH;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 1;
        gridConstraints.gridwidth = 3;
        gridConstraints.gridheight = 2;
        gridConstraints.ipadx = 5;
        gridConstraints.ipady = 5;
        gridConstraints.weighty = 1.0;
        gridbag.setConstraints(_receiverButton, gridConstraints);
        pane.add(_receiverButton);

        // Now put the dial buttons in place.
        gridConstraints.insets = new Insets(4, 4, 4, 4);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 3;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[1], gridConstraints);
        pane.add(_dialButtons[1]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 3;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[2], gridConstraints);
        pane.add(_dialButtons[2]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 3;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[3], gridConstraints);
        pane.add(_dialButtons[3]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 4;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[4], gridConstraints);
        pane.add(_dialButtons[4]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 4;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[5], gridConstraints);
        pane.add(_dialButtons[5]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 4;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[6], gridConstraints);
        pane.add(_dialButtons[6]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 5;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[7], gridConstraints);
        pane.add(_dialButtons[7]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 5;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[8], gridConstraints);
        pane.add(_dialButtons[8]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 5;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[9], gridConstraints);
        pane.add(_dialButtons[9]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 0;
        gridConstraints.gridy = 6;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[10], gridConstraints);
        pane.add(_dialButtons[10]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 1;
        gridConstraints.gridy = 6;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[0], gridConstraints);
        pane.add(_dialButtons[0]);

        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.gridx = 2;
        gridConstraints.gridy = 6;
        gridConstraints.gridwidth = 1;
        gridConstraints.gridheight = 1;
        gridConstraints.ipadx = 2;
        gridConstraints.ipady = 2;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        gridbag.setConstraints(_dialButtons[11], gridConstraints);
        pane.add(_dialButtons[11]);

        return;
    }

    // Inner classes.

    private void _playbackDone(PlaybackThread thread) {
        _playbackThread = null;
        _fsm.PlaybackDone();
    }

    private void _soundMeridian(int am_pm, List<AudioData> audioList) {
        AudioData clip;

        if (am_pm == Calendar.AM) {
            clip = _audioMap.get("AM");
        } else {
            clip = _audioMap.get("PM");
        }

        audioList.add(clip);

        return;
    }

    private void _soundNumber(int number, boolean zeroFlag,
                              List<AudioData> audioList) {
        if (number < 10 && zeroFlag == true) {
            audioList.add(_audioMap.get("oh"));
            audioList.add(_audioMap.get(Integer.toString(number)));
        } else if (number < 20) {
            audioList.add(_audioMap.get(Integer.toString(number)));
        } else {
            int onesDigit = number % 10;
            int tensDigit = number - onesDigit;

            // Read the ten's digit first and then the
            // remainder - if not zero.
            audioList.add(_audioMap.get(Integer.toString(tensDigit)));

            if (onesDigit != 0) {
                audioList.add(_audioMap.get(Integer.toString(onesDigit)));
            }
        }

        return;
    }

    private void _soundPhoneNumber(List<AudioData> audioList) {
        int i;
        String digit;

        // If this is a long distance number, sound out the
        // area code first.
        if (_callType == LONG_DISTANCE) {
            for (i = 0; i < _areaCode.length(); ++i) {
                digit = _areaCode.substring(i, i + 1);
                audioList.add(_audioMap.get(digit));
            }
        }

        // All types have an exchange.
        for (i = 0; i < _exchange.length(); ++i) {
            digit = _exchange.substring(i, i + 1);
            audioList.add(_audioMap.get(digit));
        }

        // Only long distance and local numbers have a local
        // portion.
        if (_callType == LONG_DISTANCE || _callType == LOCAL) {
            for (i = 0; i < _local.length(); ++i) {
                digit = _local.substring(i, i + 1);
                audioList.add(_audioMap.get(digit));
            }
        }

        return;
    }
}
