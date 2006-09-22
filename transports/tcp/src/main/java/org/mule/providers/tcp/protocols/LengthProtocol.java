/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.providers.tcp.TcpProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The LengthProtocol is an application level tcp protocol that can be used to
 * transfer large amounts of data without risking some data to be loss. The
 * protocol is defined by sending / reading an integer (the packet length) and
 * then the data to be transfered.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class LengthProtocol implements TcpProtocol
{

    public byte[] read(InputStream is) throws IOException
    {
        // Use a mark / reset here so that an exception
        // will not be thrown is the read times out.
        // So use the read(byte[]) method that returns 0
        // if no data can be read and reset the mark.
        // This is necessary because when no data is available
        // reading an int would throw a SocketTimeoutException.
        DataInputStream dis = new DataInputStream(is);
        byte[] buffer = new byte[32];
        int length;
        dis.mark(32);
        while ((length = dis.read(buffer)) == 0) {
            // wait
        }
        if (length == -1) {
            return null;
        }
        dis.reset();
        length = dis.readInt();
        buffer = new byte[length];
        dis.readFully(buffer);
        return buffer;
    }

    public void write(OutputStream os, byte[] data) throws IOException
    {
        // Write the length and then the data.
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
    }

}
