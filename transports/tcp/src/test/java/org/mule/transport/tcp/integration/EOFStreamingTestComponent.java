/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import org.mule.tck.functional.FunctionalStreamingTestComponent;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Extends the FunctionalStreamingTestComponent to wait for data in a non
 * blocking fashion for the StreamingProtocol.
 *
 * @see org.mule.tck.functional.EventCallback
 */
public class EOFStreamingTestComponent extends FunctionalStreamingTestComponent
{
    protected int read(InputStream is, byte[] buffer) throws IOException
    {
        int len;
        try
        {
            do
            {
                len = is.read(buffer, 0, buffer.length);
                if (0 == len)
                {
                    // wait for non-blocking input stream
                    // use new lock since not expecting notification
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e)
                    {
                        // no-op
                    }
                }
            }
            while (0 == len);
            return len;
        }
        catch (SocketException e)
        {
            // do not pollute the log with a stacktrace, log only the message
            logger.info("Socket exception occured: " + e.getMessage());
            return -1;
        }
        catch (SocketTimeoutException e)
        {
            logger.debug("Socket timeout.");
            return -1;
        }
    }

}
