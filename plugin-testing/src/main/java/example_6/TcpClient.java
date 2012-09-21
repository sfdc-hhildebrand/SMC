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
//  TcpClient.java
//
// Description
//  A TCP client connection.
//
// RCS ID
// $Id: TcpClient.java,v 1.5 2007/12/28 12:34:40 cwrapp Exp $
//
// CHANGE LOG
// $Log: TcpClient.java,v $
// Revision 1.5  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:19:07  charlesr
// Initial revision
//

package example_6;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class TcpClient extends TcpConnection {
    // Member methods

    // Create an unopened client.
    public TcpClient(TcpConnectionListener listener) {
        super(listener);
        return;
    }

    // Create an "accepted" client connection. This constructor
    // may only be called by TcpConnection.
    /* package */TcpClient(InetAddress address, int port,
                            DatagramSocket socket, int sequence_number,
                            TcpServer server, TcpConnectionListener listener) {
        super(address, port, socket, sequence_number, server, listener);
        return;
    }

    // The address and port to which I am connected.
    public InetAddress getAddress() {
        return _address;
    }

    public int getPort() {
        return _port;
    }

    public void open(InetAddress address, int port) {
        activeOpen(address, port);
        return;
    }

    public void open(int port) {
        try {
            activeOpen(InetAddress.getLocalHost(), port);
        } catch (UnknownHostException netex) {
        }

        return;
    }

    // Member data
}
