/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The DirectProtocol class is an application level tcp protocol that does nothing.
 * The socket is read until no more bytes are (momentarily) available
 * (previously the transfer buffer also had to be full on the previous read, which made
 * stronger requirements on the underlying network).  On slow networks
 * {@link org.mule.compatibility.transport.tcp.protocols.EOFProtocol} and
 * {@link org.mule.compatibility.transport.tcp.protocols.LengthProtocol} may be more reliable.
 *
 * <p>Writing simply writes the data to the socket.</p>
 */
public class DirectProtocol extends AbstractByteProtocol
{

    protected static final int UNLIMITED = -1;

    private static final Log logger = LogFactory.getLog(DirectProtocol.class);
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    protected int bufferSize;

    public DirectProtocol()
    {
        this(STREAM_OK, DEFAULT_BUFFER_SIZE);
    }

    public DirectProtocol(boolean streamOk, int bufferSize)
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
            byte[] buffer = new byte[bufferSize];
            int len;
            int remain = remaining(limit, limit, 0);
            boolean repeat;
            do
            {

                len = copy(is, buffer, baos, remain);
                remain = remaining(limit, remain, len);
                repeat = EOF != len && remain > 0 && isRepeat(len, is.available());

                if (logger.isDebugEnabled())
                {
                    logger.debug(MessageFormat.format(
                            "len/limit/repeat: {0}/{1}/{2}",
                            len, limit, repeat));
                }
            }
            while (repeat);
        }
        finally
        {
            baos.flush();
            baos.close();
        }
        return nullEmptyArray(baos.toByteArray());
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
