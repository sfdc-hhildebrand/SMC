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
//  DatagramSocketListener.java
//
// Description
//  The datagram socket listener interface. Handles two events:
//  + handleReceive: A UDP packet has arrived.
//  + handleError: An exception occurred while waiting for a
//    packet.
//
// RCS ID
// $Id: DatagramSocketListener.java,v 1.5 2007/12/28 12:34:40 cwrapp Exp $
//
// CHANGE LOG
// $Log: DatagramSocketListener.java,v $
// Revision 1.5  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:17:29  charlesr
// Initial revision
//

package example_6;

import java.net.DatagramPacket;

public interface DatagramSocketListener {
    public void handleError(Exception e, AsyncDatagramSocket datagram_socket);

    public void handleReceive(DatagramPacket packet,
                              AsyncDatagramSocket datagram_socket);
}
