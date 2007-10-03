/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The LengthProtocol is an application level tcp protocol that can be used to
 * transfer large amounts of data without risking some data to be loss. The protocol
 * is defined by sending / reading an integer (the packet length) and then the data
 * to be transferred.
 *
 * <p>Note that use of this protocol must be symmetric - both the sending and receiving
 * connectors must use the same protocol.</p>
 */
public class LengthProtocol extends DirectProtocol
{
    private static final Log logger = LogFactory.getLog(LengthProtocol.class);
    // TODO - can we not get this from the API somewhere?
    private static final int SIZE_INT = 4;
    public static final int NO_MAX_LENGTH = -1;
    private int maxMessageLength;

    public LengthProtocol()
    {
        this(NO_MAX_LENGTH);
    }

    public LengthProtocol(int maxMessageLength)
    {
        super(NO_STREAM, SIZE_INT);
        this.setMaxMessageLength(maxMessageLength);
    }

    public Object read(InputStream is) throws IOException
    {
        // original comments indicated that we nede to use read(byte[]) rather than readInt()
        // to avoid socket timeouts - don't understand, but don't want to risk change.

        // first read the data necessary to know the length of the payload
        DataInputStream dis = new DataInputStream(is);
        dis.mark(SIZE_INT);
        // this pulls through SIZE_INT bytes
        if (null == super.read(dis, SIZE_INT))
        {
            return null; // eof
        }

        // reset and read the integer
        dis.reset();
        int length = dis.readInt();
        if (logger.isDebugEnabled())
        {
            logger.debug("length: " + length);
        }

        if (length < 0 || (getMaxMessageLength() > 0 && length > getMaxMessageLength()))
        {
            throw new IOException("Length " + length + " exceeds limit: " + getMaxMessageLength());
        }

        // finally read the rest of the data
        byte[] buffer = new byte[length];
        dis.readFully(buffer);
        if (logger.isDebugEnabled())
        {
            logger.debug("length read: " + buffer.length);
        }

        return buffer;
    }

    // @Override
    protected void writeByteArray(OutputStream os, byte[] data) throws IOException
    {
        // Write the length and then the data.
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
    }

    /**
     * Read all four bytes for initial integer (limit is set in read)
     *
     * @param len Amount transferred last call (-1 on EOF or socket error)
     * @param available Amount available
     * @return true if the transfer should continue
     */
    // @Override
    protected boolean isRepeat(int len, int available)
    {
        return true;
    }

    public int getMaxMessageLength()
    {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength)
    {
        this.maxMessageLength = maxMessageLength;
    }
    
}
