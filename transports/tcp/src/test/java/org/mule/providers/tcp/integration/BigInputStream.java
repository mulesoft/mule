/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import java.io.InputStream;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BigInputStream extends InputStream
{

    private static final int SUMMARY_SIZE = 4;
    private static final MessageFormat FORMAT  =
            new MessageFormat("Sent {0,number,#} bytes, {1,number,###.##}% (free {2,number,#}/{3,number,#})");
    private final Log logger = LogFactory.getLog(getClass());
    private long size;
    private int messages;

    private long sent = 0;
    private byte[] data;
    private int dataIndex = 0;
    private int printedMessages = 0;
    private int nextMessage = 0;


    /**
     * @param size Number of bytes to transfer
     * @param messages Number of mesagges logged as INFO
     */
    public BigInputStream(long size, int messages)
    {
        this.size = size;
        this.messages = messages;
        data = ("This message is repeated for " + size + " bytes. ").getBytes();
    }

    /**
     * @return String matching {@link org.mule.tck.functional.FunctionalStreamingTestComponent}
     */
    public String summary()
    {

        byte[] tail = new byte[SUMMARY_SIZE];
        for (int i = 0; i < SUMMARY_SIZE; ++i)
        {
            tail[i] = data[(int) ((sent - SUMMARY_SIZE + i) % data.length)];
        }
        return "Received stream; length: " + sent + "; '" +
                new String(data, 0, 4) + "..." + new String(tail) +
                "'";
    }

    public int read() throws IOException
    {
        if (sent == size)
        {
            return -1;
        }
        else
        {
            if (++sent > nextMessage)
            {
                double percent = 100l * sent / ((double) size);
                Runtime runtime = Runtime.getRuntime();
                logger.info(FORMAT.format(new Object[]{
                        new Long(sent), new Double(percent),
                        new Long(runtime.freeMemory()), new Long(runtime.maxMemory())}));
                nextMessage = ++printedMessages *
                        ((int) Math.floor(((double) size) / (messages - 1)) - 1);
            }
            if (dataIndex == data.length)
            {
                dataIndex = 0;
            }
            return data[dataIndex++];
        }
    }

}


