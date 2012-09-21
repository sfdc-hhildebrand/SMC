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
//  TcpSegment.java
//
// Description
//  This class encapsulates transmitted TCP data.
//
// RCS ID
// $Id: TcpSegment.java,v 1.5 2007/12/28 12:34:40 cwrapp Exp $
//
// CHANGE LOG
// $Log: TcpSegment.java,v $
// Revision 1.5  2007/12/28 12:34:40  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 20:20:59  charlesr
// Initial revision
//

package example_6;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class TcpSegment {
    /* package */static String flagsToString(int flags) {
        String separator = "{";
        String retval = "";

        if ((flags & FIN) == FIN) {
            retval += separator + "FIN";
            separator = ", ";
        }

        if ((flags & SYN) == SYN) {
            retval += separator + "SYN";
            separator = ", ";
        }

        if ((flags & RST) == RST) {
            retval += separator + "RST";
            separator = ", ";
        }

        if ((flags & PSH) == PSH) {
            retval += separator + "PSH";
            separator = ", ";
        }

        if ((flags & ACK) == ACK) {
            retval += separator + "ACK";
            separator = ", ";
        }

        if ((flags & URG) == URG) {
            retval += separator + "URG";
            separator = ", ";
        }
        retval += "}";

        return retval;
    }

    /* package */InetAddress      _src_address;

    /* package */int              _src_port;

    /* package */InetAddress      _dest_address;

    /* package */int              _dest_port;

    /* package */int              _sequence_number;

    /* package */int              _ack_number;

    /* package */int              _flags;

    /* package */byte[]           _data;

    /* package */int              _data_size;

    // TCP header flags.
    public static final int        FIN             = 0x01;

    public static final int        SYN             = 0x02;

    public static final int        RST             = 0x04;

    public static final int        PSH             = 0x08;

    public static final int        ACK             = 0x10;

    // Member data

    public static final int        URG             = 0x20;
    public static final int        FIN_ACK         = FIN | ACK;
    public static final int        SYN_ACK         = SYN | ACK;
    public static final int        RST_ACK         = RST | ACK;
    public static final int        PSH_ACK         = PSH | ACK;
    public static final int        FLAG_MASK       = FIN | SYN | RST | PSH
                                                     | ACK | URG;
    // Use this static byte array to store a generic
    // TCP header. Copy and modify for TCP transmissions.
    /* package */static final int TCP_HEADER_SIZE = 16;

    // Member methods.
    public TcpSegment(int source_port, InetAddress destination_address,
                      int destination_port, int sequence_number,
                      int ack_number, int flags, byte[] data, int offset,
                      int size) {
        try {
            _src_address = InetAddress.getLocalHost();
        } catch (UnknownHostException hex) {
        }

        _src_port = source_port;
        _dest_address = destination_address;
        _dest_port = destination_port;
        _sequence_number = sequence_number;
        _ack_number = ack_number;
        _flags = flags & FLAG_MASK;

        // Copy in the data.
        if (data == null || data.length == 0) {
            _data = null;
            _data_size = 0;
        } else {
            _data = new byte[size];
            _data_size = size;
            System.arraycopy(data, offset, _data, 0, size);
        }

        return;
    }

    /* package */TcpSegment(DatagramPacket packet) {
        byte[] segment = packet.getData();

        _src_address = packet.getAddress();
        try {
            _dest_address = InetAddress.getLocalHost();
        } catch (UnknownHostException hex) {
        }

        _src_port = (segment[0] & 0x000000ff) << 8 | segment[1] & 0x000000ff;
        _dest_port = (segment[2] & 0x000000ff) << 8 | segment[3] & 0x000000ff;
        _sequence_number = (segment[4] & 0x000000ff) << 24
                           | (segment[5] & 0x000000ff) << 16
                           | (segment[6] & 0x000000ff) << 8 | segment[7]
                           & 0x000000ff;
        _ack_number = (segment[8] & 0x000000ff) << 24
                      | (segment[9] & 0x000000ff) << 16
                      | (segment[10] & 0x000000ff) << 8 | segment[11]
                      & 0x000000ff;
        _flags = (segment[12] & 0x000000ff) << 8 | segment[13] & 0x000000ff;
        _data_size = (segment[14] & 0x000000ff) << 8 | segment[15] & 0x000000ff;

        if (_data_size == 0) {
            _data = null;
        } else {
            _data = new byte[_data_size];
            System.arraycopy(segment, TCP_HEADER_SIZE, _data, 0, _data_size);
        }

        return;
    }

    @Override
    public String toString() {
        String data_string;
        String retval;

        if (_data_size == 0) {
            data_string = "";
        } else {
            data_string = new String(_data);
        }

        retval = "\tSource       : " + _src_address + ":"
                 + Integer.toString(_src_port) + "\n\tDestination  : "
                 + _dest_address + ":" + Integer.toString(_dest_port)
                 + "\n\tSequence #   : " + Integer.toString(_sequence_number)
                 + "\n\tAcknowledge #: " + Integer.toString(_ack_number)
                 + "\n\tFlags        : " + flagsToString(_flags)
                 + "\n\tData size    : " + Integer.toString(_data_size)
                 + "\n\tData         : \"" + data_string + "\"";

        return retval;
    }

    /* package */int getAcknowledgeNumber() {
        return _ack_number;
    }

    /* package */byte[] getData() {
        return _data;
    }

    /* package */int getDataSize() {
        return _data_size;
    }

    /* package */InetAddress getDestinationAddress() {
        return _dest_address;
    }

    /* package */int getDestinationPort() {
        return _dest_port;
    }

    /* package */int getFlags() {
        return _flags;
    }

    /* package */int getSequenceNumber() {
        return _sequence_number;
    }

    /* package */InetAddress getSourceAddress() {
        return _src_address;
    }

    /* package */int getSourcePort() {
        return _src_port;
    }

    /* package */DatagramPacket packetize() {
        int segment_size;
        byte[] segment;
        DatagramPacket retval;

        // The segment's size is: TCP header size + data size
        // (options are not supported).
        segment_size = TCP_HEADER_SIZE + _data_size;

        segment = new byte[segment_size];
        segment[0] = (byte) ((_src_port & 0x0000ff00) >> 8);
        segment[1] = (byte) (_src_port & 0x000000ff);
        segment[2] = (byte) ((_dest_port & 0x0000ff00) >> 8);
        segment[3] = (byte) (_dest_port & 0x000000ff);
        segment[4] = (byte) (_sequence_number >> 24);
        segment[5] = (byte) ((_sequence_number & 0x00ff0000) >> 16);
        segment[6] = (byte) ((_sequence_number & 0x0000ff00) >> 8);
        segment[7] = (byte) (_sequence_number & 0x000000ff);
        segment[8] = (byte) (_ack_number >> 24);
        segment[9] = (byte) ((_ack_number & 0x00ff0000) >> 16);
        segment[10] = (byte) ((_ack_number & 0x0000ff00) >> 8);
        segment[11] = (byte) (_ack_number & 0x000000ff);
        segment[12] = (byte) (_flags >> 8);
        segment[13] = (byte) (_flags & 0x000000ff);
        segment[14] = (byte) (_data_size >> 8);
        segment[15] = (byte) (_data_size & 0x000000ff);

        // Copy in the data, if there is any.
        if (_data_size > 0) {
            System.arraycopy(_data, 0, segment, TCP_HEADER_SIZE, _data_size);
        }

        retval = new DatagramPacket(segment, segment_size, _dest_address,
                                    _dest_port);

        return retval;
    }

    /* package */void setSourcePort(int port) {
        _src_port = port;
        return;
    }
}
