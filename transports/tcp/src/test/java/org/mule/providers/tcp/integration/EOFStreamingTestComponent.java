/*
 * $Id: FunctionalStreamingTestComponent.java 8474 2007-09-18 16:44:27Z dandiep $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

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