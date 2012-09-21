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
//  AsyncDatagramSocket.java
//
// Description
//  This thread receives UDP datagrams and passes them
//  asynchronously to the listener.
//
// RCS ID
// $Id: AsyncDatagramSocket.java,v 1.5 2007/12/28 12:34:40 cwrapp Exp $
//
// CHANGE LOG
// $Log: AsyncDatagramSocket.java,v $
// Revision 1.5  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:16:58  charlesr
// Initial revision
//

package example_6;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public final class AsyncDatagramSocket extends Thread {
    // Member methods

    private DatagramSocket         _dgram_socket;

    private DatagramSocketListener _listener;

    // Receive UDP input into this packet.
    private int                    _packet_size;

    private byte[]                 _buffer;

    private DatagramPacket         _packet;

    // Use these flags to control the threads running.
    private boolean                _is_running;

    private boolean                _continue_flag;

    // Default packet size. Note: UDP header provides only
    // 16 bits for data size - which means the maximum data
    // size is 0xffff (unsigned) or 65535.
    private static final int       DEFAULT_PACKET_SIZE = 512;

    private static final int       MAX_PACKET_SIZE     = 65535;

    // Member data

    public AsyncDatagramSocket(DatagramSocket dgram_socket,
                               DatagramSocketListener listener)
                                                               throws IllegalArgumentException {
        if (dgram_socket == null) {
            throw new IllegalArgumentException("null dgram_socket");
        } else if (listener == null) {
            throw new IllegalArgumentException("null listener");
        } else {
            _dgram_socket = dgram_socket;
            _packet_size = -1;
            _buffer = null;
            _packet = null;
            _listener = listener;

            _is_running = false;
            _continue_flag = false;
        }
    }

    public AsyncDatagramSocket(DatagramSocket dgram_socket, int packet_size,
                               DatagramSocketListener listener)
                                                               throws IllegalArgumentException {
        if (dgram_socket == null) {
            throw new IllegalArgumentException("null dgram_socket");
        } else if (listener == null) {
            throw new IllegalArgumentException("null listener");
        } else if (packet_size < 0 || packet_size > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("invalid packet size ("
                                               + Integer.toString(packet_size)
                                               + ")");
        } else {
            _dgram_socket = dgram_socket;
            _packet_size = packet_size;
            _buffer = null;
            _packet = null;
            _listener = listener;

            _is_running = false;
            _continue_flag = false;
        }
    }

    public synchronized void closeDatagramSocket() {
        if (_dgram_socket != null) {
            // Stop running if we are.
            stopRunning();

            _dgram_socket.close();
            _dgram_socket = null;
        }

        return;
    }

    public synchronized DatagramSocket getDatagramSocket() {
        return _dgram_socket;
    }

    public synchronized int getPacketSize() {
        return _packet_size;
    }

    public synchronized boolean isRunning() {
        return _is_running;
    }

    @Override
    public void run() {
        Exception exception = null;

        // If the packet size has not been set, then
        // 1 - Use the SO_RCVBUF size
        // 2 - Use the default size.
        if (_packet_size <= 0 || _packet_size > MAX_PACKET_SIZE) {
            try {
                _packet_size = _dgram_socket.getReceiveBufferSize();
            } catch (SocketException sockex) {
                _packet_size = DEFAULT_PACKET_SIZE;
            }
        }

        _continue_flag = true;
        _is_running = true;
        while (_continue_flag == true && exception == null) {
            try {
                // Allocate the packet.
                _buffer = new byte[_packet_size];
                _packet = new DatagramPacket(_buffer, _packet_size);

                _dgram_socket.receive(_packet);
                _listener.handleReceive(_packet, this);
            } catch (Exception e) {
                exception = e;
            }
        }

        // This thread has terminated.
        _is_running = false;

        // Don't send events if application stopped this thread.
        if (_continue_flag == true && exception != null) {
            _listener.handleError(exception, this);
        }

        return;
    }

    public synchronized void setPacketSize(int packet_size)
                                                           throws IllegalArgumentException {
        if (packet_size < 0 || packet_size > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("invalid packet size ("
                                               + Integer.toString(packet_size)
                                               + ")");
        } else {
            _packet_size = packet_size;
        }

        // Reallocate the packet *only* if the thread is running.
        // Otherwise, when the thread is started, the packet will
        // be allocated at that time.
        if (_is_running == true) {
            _buffer = new byte[_packet_size];
            _packet = new DatagramPacket(_buffer, _packet_size);
        }

        return;
    }

    public synchronized void stopRunning() {
        // Are we running?
        if (_continue_flag == true && _is_running == true) {
            // Yes, so then stop.
            _continue_flag = false;
            interrupt();
        }

        return;
    }
}
