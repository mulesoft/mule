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

import org.mule.providers.tcp.TcpInputStream;
import org.mule.providers.tcp.TcpProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamingProtocol extends EOFProtocol implements TcpProtocol
{

    public StreamingProtocol()
    {
        super();
    }

    public Object read(InputStream is) throws IOException
    {
        if (is instanceof TcpInputStream)
        {
            ((TcpInputStream) is).setStreaming(true);
        }
        
        return is;
    }

    /**
     * 
     * @param is
     * @param os
     * @throws IOException
     */
    protected void copyStream(InputStream is, OutputStream os) throws IOException
    {
        try
        {
            int limit = getLimit();
            byte[] buffer = new byte[bufferSize];
            int len;
            int remain = remaining(limit, limit, 0);
            int total = 0;
            boolean repeat;
            do
            {
                len = copy(is, buffer, os, remain);
                total += len;
                remain = remaining(limit, remain, len);
                repeat = EOF != len && remain > 0 && isRepeat(len, is.available());
                
                // Flush the data if we didn't fill up the whole buffer
                // in case we're at the end of the stream and the receiving
                // side is waiting for the end of the data before closing the socket
                if (len > 0 && len < buffer.length)
                {
                    os.flush();
                }
            }
            while (repeat);
        }
        finally
        {
            is.close();
        }
    }

    protected int getLimit()
    {
        return UNLIMITED;
    }

}


