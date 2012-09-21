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
//  TcpConnection.java
//
// Description
//  Both TCP client and server sockets are TCP connections.
//
// RCS ID
// $Id: TcpConnection.java,v 1.9 2009/03/27 09:41:46 cwrapp Exp $
//
// CHANGE LOG
// $Log: TcpConnection.java,v $
// Revision 1.9  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.8  2009/03/01 18:20:39  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.7  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.6  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.5  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:19:31  charlesr
// Initial revision
//

package example_6;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class TcpConnection implements DatagramSocketListener,
        TimerListener {
    //---------------------------------------------------------------
    // Member methods.
    //

    protected TcpConnectionListener _listener;

    private TcpConnectionContext    _fsm;

    protected AsyncDatagramSocket   _async_socket;

    private int                     _sequence_number;

    // The port to which a client socket is connected.
    protected InetAddress           _address;

    protected int                   _port;

    // The server which accepted this connection.
    protected TcpServer             _server;

    private String                  _errorMessage;

    // The Initial Sequence Number.
    private static final int        ISN           = 1415531521;

    // Wait only so long for an ACK (in milliseconds).
    /* package */static final long ACK_TIMEOUT   = 2000;

    // Wait a while before reusing this port (in milliseconds).
    /* package */static final long CLOSE_TIMEOUT = 10000;

    /* package */static final long MIN_TIMEOUT   = 1;

    // Use this table to translate received segment flags into
    // state map transitions.
    private static Method[]         _transition_table;

    static {
        try {
            Class<?> context = TcpConnectionContext.class;
            Class<?>[] parameters = new Class[1];
            Method undefined;
            int i;

            // All "TCP flag" transitions take a DatagramPacket as
            // a parameter.
            parameters[0] = TcpSegment.class;

            _transition_table = new Method[TcpSegment.FLAG_MASK + 1];

            // First, set all transitions to undefined.
            undefined = context.getDeclaredMethod("UNDEF", parameters);
            for (i = 0; i < _transition_table.length; ++i) {
                _transition_table[i] = undefined;
            }

            // Now go back and set the known transitions.
            _transition_table[TcpSegment.FIN] = context.getDeclaredMethod("FIN",
                                                                          parameters);
            _transition_table[TcpSegment.SYN] = context.getDeclaredMethod("SYN",
                                                                          parameters);
            _transition_table[TcpSegment.RST] = context.getDeclaredMethod("RST",
                                                                          parameters);
            _transition_table[TcpSegment.PSH] = context.getDeclaredMethod("PSH",
                                                                          parameters);
            _transition_table[TcpSegment.ACK] = context.getDeclaredMethod("ACK",
                                                                          parameters);
            _transition_table[TcpSegment.URG] = context.getDeclaredMethod("URG",
                                                                          parameters);
            _transition_table[TcpSegment.FIN_ACK] = context.getDeclaredMethod("FIN_ACK",
                                                                              parameters);
            _transition_table[TcpSegment.SYN_ACK] = context.getDeclaredMethod("SYN_ACK",
                                                                              parameters);
            _transition_table[TcpSegment.PSH_ACK] = context.getDeclaredMethod("PSH_ACK",
                                                                              parameters);
        } catch (Exception jex) {
        }
    }

    // "Accepted" socket constructor.
    protected TcpConnection(InetAddress address, int port,
                            DatagramSocket socket, int sequence_number,
                            TcpServer server, TcpConnectionListener listener) {
        _async_socket = new AsyncDatagramSocket(socket, this);
        _address = address;
        _port = port;
        _sequence_number = sequence_number;
        _server = server;
        _errorMessage = null;
        _listener = listener;
        _fsm = new TcpConnectionContext(this);

        // REFLECTION
        // Turn on FSM debugging.
        // _fsm.setDebugFlag(true);

        _async_socket.start();

        return;
    }

    // Server socket constructor.
    protected TcpConnection(TcpConnectionListener listener) {
        _listener = listener;
        _fsm = new TcpConnectionContext(this);
        _sequence_number = 0;
        _async_socket = null;
        _address = null;
        _port = -1;
        _server = null;
        _errorMessage = null;

        // REFLECTION
        // Turn on FSM debugging.
        // _fsm.setDebugFlag(true);

        return;
    }

    public final void close() {
        synchronized (this) {
            try {
                // REFLECTION
                // Uncomment the following line to output
                // transitions.
                // _outputTransitions();

                _fsm.Close();

                // Wait for the connection to close before
                // returning.
                while (_async_socket != null) {
                    try {
                        this.wait();
                    } catch (InterruptedException interrupt) {
                    }
                }
            } finally {
                notify();
            }
        }

        return;
    }

    @Override
    public final void handleError(Exception e, AsyncDatagramSocket dgram_socket) {
        // TODO
        // Generate the appropriate transition.
    }

    @Override
    public final void handleReceive(DatagramPacket packet,
                                    AsyncDatagramSocket dgram_socket) {
        synchronized (this) {
            try {
                TcpSegment segment = new TcpSegment(packet);
                Object[] args = new Object[1];

                // Generate the appropriate transition based on
                // the header flags.
                args[0] = segment;

                // DEBUG
                //                 System.out.println(
                //                     "Receive event from " +
                //                     packet.getAddress() +
                //                     ":" +
                //                     Integer.toString(packet.getPort()) +
                //                     ":\n" +
                //                     segment);

                // REFLECTION
                // Uncomment the following line to output
                // transitions.
                // _outputTransitions();

                _transition_table[segment.getFlags()].invoke(_fsm, args);
            } catch (Exception jex) {
                System.err.println(jex);
                jex.printStackTrace();
            } finally {
                notify();
            }
        }

        return;
    }

    @Override
    public final void handleTimeout(String name) {
        synchronized (this) {
            try {
                // REFLECTION
                // Uncomment the following line to output
                // transitions.
                // _outputTransitions();

                if (name.compareTo("CONN_ACK_TIMER") == 0) {
                    _fsm.ConnAckTimeout();
                } else if (name.compareTo("TRANS_ACK_TIMER") == 0) {
                    _fsm.TransAckTimeout();
                } else if (name.compareTo("CLOSE_ACK_TIMER") == 0) {
                    _fsm.CloseAckTimeout();
                } else if (name.compareTo("CLOSE_TIMER") == 0) {
                    _fsm.CloseTimeout();
                } else if (name.compareTo("SERVER_OPENED") == 0) {
                    _fsm.Opened();
                } else if (name.compareTo("CLIENT_OPENED") == 0) {
                    _fsm.Opened(_address, _port);
                } else if (name.compareTo("OPEN_FAILED") == 0) {
                    _fsm.OpenFailed(_errorMessage);
                    _errorMessage = null;
                }
            } finally {
                notify();
            }
        }

        return;
    }

    public final void start() {
        _fsm.enterStartState();
        return;
    }

    private int getAck(TcpSegment segment) {
        int retval;

        // The ack # depends on the segment's flags.
        switch (segment.getFlags()) {
            case TcpSegment.FIN:
            case TcpSegment.SYN:
            case TcpSegment.FIN_ACK:
            case TcpSegment.SYN_ACK:
                retval = segment.getSequenceNumber() + 1;
                break;

            case TcpSegment.PSH:
            case TcpSegment.PSH_ACK:
                retval = segment.getSequenceNumber() + segment.getDataSize();
                break;

            case TcpSegment.ACK:
            default:
                retval = segment.getSequenceNumber();
                break;
        }

        return retval;
    }

    protected final void acceptOpen(TcpSegment segment) {
        synchronized (this) {
            try {
                // REFLECTION
                // Uncomment the following line to output
                // transitions.
                // _outputTransitions();

                _fsm.AcceptOpen(segment);
            } finally {
                notify();
            }
        }

        return;
    }

    protected final void activeOpen(InetAddress address, int port) {
        synchronized (this) {
            try {
                // REFLECTION
                // Uncomment the following line to output
                // transitions.
                // _outputTransitions();

                _fsm.ActiveOpen(address, port);
            } finally {
                notify();
            }
        }

        return;
    }

    protected final void passiveOpen(int port) {
        synchronized (this) {
            try {
                // REFLECTION
                // Uncomment the following line to output
                // transitions.
                // _outputTransitions();

                _fsm.PassiveOpen(port);
            } finally {
                notify();
            }
        }

        return;
    }

    protected final void setListener(TcpConnectionListener listener)
                                                                    throws IllegalStateException {
        if (_listener != null) {
            throw new IllegalStateException("Socket listener already set");
        } else {
            _listener = listener;
        }

        return;
    }

    protected void transmit(byte[] data, int offset, int length) {
        synchronized (this) {
            try {
                // REFLECTION
                // Uncomment the following lines to output
                // transitions.
                // _outputTransitions();

                _fsm.Transmit(data, offset, length);
            } finally {
                notify();
            }
        }

        return;
    }

    // Create a client socket to handle a new connection.
    /* package */void accept(TcpSegment segment) {
        TcpClient accept_client;
        DatagramSocket dgram_socket;

        try {
            _address = segment.getSourceAddress();
            _port = segment.getSourcePort();

            // Create a new client socket to handle this side of
            // the socket pair.
            dgram_socket = new DatagramSocket();
            accept_client = new TcpClient(_address, _port, dgram_socket,
                                          _sequence_number, (TcpServer) this,
                                          _listener);

            ((TcpConnection) accept_client).acceptOpen(segment);
        } catch (Exception jex) {
            // If the open fails, send a reset to the peer.
            send(TcpSegment.RST, null, 0, 0, segment);
        }

        return;
    }

    /* package */void accepted() {
        TcpServer server = _server;
        TcpConnectionListener listener = _listener;

        // Tell the server listener that a new connection has
        // been accepted. Then clear the server listener because
        // this socket is now truly a client socket. Clear the
        // listener member data now because the callback method
        // will be resetting it and the reset will fail if we
        // don't do it.
        _server = null;
        _listener = null;
        listener.accepted((TcpClient) this, server);

        return;
    }

    /* package */void clearListener() {
        _listener = null;
        return;
    }

    /* package */void closed(String reason) {
        if (_listener != null) {
            _listener.closed(reason, this);
            _listener = null;
        }

        return;
    }

    /* package */void closeSocket() {
        _async_socket.closeDatagramSocket();
        _async_socket = null;
        _address = null;
        _port = -1;
        return;
    }

    //-----------------------------------------------------------
    // State Map Actions.
    //
    /* package */InetAddress getFarAddress() {
        return _address;
    }

    /* package */int getFarPort() {
        return _port;
    }

    /* package */int getSequenceNumber() {
        return _sequence_number;
    }

    /*
     * REFLECTION
     * Uncomment the following method to output transitions.
     *
    private void _outputTransitions()
    {
        if (_fsm.getDebugFlag() == true)
        {
            java.io.PrintStream str = _fsm.getDebugStream();
            TcpConnectionContext.TcpConnectionState state =
                _fsm.getState();
            java.util.Iterator it;
            String sep;

            str.print("State ");
            str.print(state.getName());
            str.print(" has transitions ");

            for (it = state.getTransitions().iterator(), sep = "{";
                 it.hasNext();
                 sep = ", ")
            {
                str.print(sep);
                str.print(it.next());
            }

            str.println("}");
        }

        return;
    }
     *
     */

    //---------------------------------------------------------------
    // Member data.
    //

    /* package */void halfClosed() {
        if (_listener != null) {
            _listener.halfClosed(this);
        }

        return;
    }

    /* package */void openClientSocket(InetAddress address, int port) {
        DatagramSocket socket;

        try {
            socket = new DatagramSocket();

            _address = address;
            _port = port;
            _async_socket = new AsyncDatagramSocket(socket, this);
            _async_socket.start();

            // Set the sequence number.
            _sequence_number = ISN;

            startTimer("CLIENT_OPENED", MIN_TIMEOUT);
        } catch (Exception jex) {
            // Do not issue a transition now since we are already
            // in a transition. Set a 1 millisecond timer and
            // issue transition when timer expires.
            _errorMessage = jex.toString();
            startTimer("OPEN_FAILED", MIN_TIMEOUT);
        }

        return;
    }

    /* package */void openFailed(String reason) {
        _listener.openFailed(reason, this);
        return;
    }

    /* package */void openServerSocket(int port) {
        DatagramSocket socket;

        try {
            // Create the asynchronous datagram socket listener and
            // start it running.
            socket = new DatagramSocket(port);
            _async_socket = new AsyncDatagramSocket(socket, this);
            _async_socket.start();

            // Set the sequence number.
            _sequence_number = ISN;

            startTimer("SERVER_OPENED", MIN_TIMEOUT);

        } catch (Exception jex) {
            _errorMessage = jex.getMessage();
            startTimer("OPEN_FAILED", MIN_TIMEOUT);
        }

        return;
    }

    /* package */void openSuccess() {
        _listener.opened(this);
        return;
    }

    /* package */void receive(TcpSegment segment) {
        // Send the TCP segment's data to the socket listener.
        if (_listener != null) {
            _listener.receive(segment.getData(), this);
        }

        return;
    }

    /* package */void send(int flags, byte[] data, int offset, int size,
                            InetAddress address, int port,
                            TcpSegment recv_segment) {
        // Quietly quit if there is no socket.
        if (_async_socket != null
            && (_async_socket.getDatagramSocket()) != null) {
            int local_port;
            int ack_number;
            TcpSegment send_segment;
            DatagramPacket packet = null;

            // If the address and port were not specified, then
            // send this segment to whatever client socket we are
            // currently speaking.
            if (address == null) {
                address = _address;
                port = _port;
            }

            // If there is a recv_segment, then use its
            // destination port as the local port. Otherwise, use
            // the local datagram socket's local port.
            if (recv_segment != null) {
                local_port = recv_segment.getDestinationPort();
            } else {
                local_port = _async_socket.getDatagramSocket().getLocalPort();
            }

            // Send the ack number only if the ack flag is set.
            if ((flags & TcpSegment.ACK) == 0) {
                ack_number = 0;
            } else {
                // Figure out the ack number based on the
                // received segment's sequence number and data
                // size.
                ack_number = getAck(recv_segment);
            }

            send_segment = new TcpSegment(local_port, address, port,
                                          _sequence_number, ack_number, flags,
                                          data, offset, size);

            // Advance the sequence number depending on the
            // message sent. Don't do this if message came from
            // an interloper.
            if (address.equals(_address) && port == _port) {
                _sequence_number = getAck(send_segment);
            }

            // Now send the data.
            try {
                packet = send_segment.packetize();

                // DEBUG
                //                 System.out.println(
                //                     "Sending packet to " +
                //                     packet.getAddress() +
                //                     ":" +
                //                     Integer.toString(packet.getPort()) +
                //                     ":\n" +
                //                     send_segment);

                _async_socket.getDatagramSocket().send(packet);
            } catch (IOException io_exception) {
                // Ignore - the ack timer will figure out this
                // packet was never sent.

                // DEBUG
                //                 System.out.println(
                //                     "Send to " +
                //                     packet.getAddress() +
                //                     ": " +
                //                     io_exception.getMessage());
            }
        }

        return;
    }

    /* package */void send(int flags, byte[] data, int offset, int size,
                            TcpSegment recv_segment) {
        send(flags, data, offset, size, recv_segment.getSourceAddress(),
             recv_segment.getSourcePort(), recv_segment);
        return;
    }

    // Send the SYN/ACK reply to the client's SYN.
    /* package */void sendAcceptSynAck(TcpSegment segment) {
        int client_port;
        byte[] port_bytes = new byte[2];

        // Tell the far-side client with what port it should now
        // communicate.
        client_port = _async_socket.getDatagramSocket().getLocalPort();

        port_bytes[0] = (byte) ((client_port & 0x0000ff00) >> 8);
        port_bytes[1] = (byte) (client_port & 0x000000ff);
        send(TcpSegment.SYN_ACK, port_bytes, 0, 2, null, -1, segment);

        return;
    }

    /* package */void setDestinationPort(TcpSegment segment) {
        byte[] data;

        // The server socket is telling us the accepted client's
        // port number. Reset the destination port to that.
        data = segment.getData();
        _port = (data[0] & 0x000000ff) << 8 | data[1] & 0x000000ff;

        // Modify the segment's source port so that the ack will
        // go to the correct destination.
        segment.setSourcePort(_port);

        return;
    }

    /* package */void startTimer(String name, long time) {
        AsyncTimer.startTimer(name, time, this);
        return;
    }

    /* package */void stopTimer(String name) {
        AsyncTimer.stopTimer(name);
        return;
    }

    /* package */void transmitFailed(String reason) {
        if (_listener != null) {
            _listener.transmitFailed(reason, this);
        }

        return;
    }

    /* package */void transmitted() {
        if (_listener != null) {
            _listener.transmitted(this);
        }

        return;
    }
}
