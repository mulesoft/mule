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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The DefaultProtocol class is an application level tcp protocol that does nothing.
 * The socket is read until no more bytes are (momentariy) available
 * (previously the transfer buffer also had to be full on the previous read, which made
 * stronger requirements on the underlying network).  On slow networks
 * {@link org.mule.providers.tcp.protocols.EOFProtocol} and
 * {@link org.mule.providers.tcp.protocols.LengthProtocol} may be more reliable.
 *
 * <p>Writing simply writes the data to the socket.</p>
 */
public class DefaultProtocol extends ByteProtocol
{
    public static final int UNLIMITED = -1;

    private static final Log logger = LogFactory.getLog(DefaultProtocol.class);
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    protected int bufferSize;

    public DefaultProtocol()
    {
        this(STREAM_OK, DEFAULT_BUFFER_SIZE);
    }

    public DefaultProtocol(boolean streamOk, int bufferSize)
    {
        super(streamOk);
        this.bufferSize = bufferSize;
    }

    public Object read(InputStream is) throws IOException
    {
        return read(is, UNLIMITED);
    }

    public Object read(InputStream is, int limit) throws IOException
    {
        // this can grow on repeated reads
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
        
        try
        {
            copy(is, baos, limit);
        }
        finally
        {
            baos.flush();
            baos.close();
        }
        return nullEmptyArray(baos.toByteArray());
    }

    protected void copy(InputStream is, OutputStream os, int limit) throws IOException
    {
        byte[] buffer = new byte[bufferSize];
        int len;
        int remain = remaining(limit, limit, 0);
        boolean repeat;
        do
        {

            len = copy(is, buffer, os, remain);
            remain = remaining(limit, remain, len);
            repeat = EOF != len && remain > 0 && isRepeat(len, is.available());

            if (logger.isDebugEnabled())
            {
                logger.debug(MessageFormat.format(
                        "len/limit/repeat: {0}/{1}/{2}",
                        new Object[] {new Integer(len), new Integer(limit), Boolean.valueOf(repeat)}));
            }
        }
        while (repeat);
    }

    protected int remaining(int limit, int remain, int len)
    {
        if (UNLIMITED == limit)
        {
            return bufferSize;
        }
        else if (EOF != len)
        {
            return remain - len;
        }
        else
        {
            return remain;
        }
    }

    /**
     * Decide whether to repeat transfer.  This implementation does so if
     * more data are available.  Note that previously, while documented as such,
     * there was also the additional requirement that the previous transfer
     * completely used the transfer buffer.
     *
     * @param len Amount transferred last call (-1 on EOF or socket error)
     * @param available Amount available
     * @return true if the transfer should continue
     */
    protected boolean isRepeat(int len, int available)
    {
        // previous logic - less reliable on slow networks
//        return len == bufferSize && available > 0;
        
        return available > 0;
    }

}
