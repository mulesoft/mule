/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.impl.model.streaming.StreamingService;
import org.mule.umo.UMOEventContext;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.io.InputStream;
import java.io.OutputStream;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A component that can be used by streaming functional tests. This component accepts an
 * EventCallback that can be used to assert the state of the current event.  To access the
 * component when embedded in an (XML) model, make sure that the descriptor sets the
 * singleton attribute true - see uses in TCP and FTP.
 *
 * Note that although this implements the full StreamingService interface, nothing is
 * written to the output stream - this is intended as a final sink.
 *
 * @see org.mule.tck.functional.EventCallback
 */

public class FunctionalStreamingTestComponent implements StreamingService
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private static AtomicInteger count = new AtomicInteger(0);
    private int number = count.incrementAndGet();

    public static final int STREAM_SAMPLE_SIZE = 4;
    public static final int STREAM_BUFFER_SIZE = 4096;
    private EventCallback eventCallback;
    private String summary = null;
    private long targetSize = -1;

    public FunctionalStreamingTestComponent()
    {
        logger.debug("creating " + toString());
    }

    public void setEventCallback(EventCallback eventCallback, long targetSize)
    {
        logger.debug("setting callback: " + eventCallback + " in " + toString());
        this.eventCallback = eventCallback;
        this.targetSize = targetSize;
    }

    public String getSummary()
    {
        return summary;
    }

    public int getNumber()
    {
        return number;
    }

    public void call(InputStream in, OutputStream unused, UMOEventContext context) throws Exception
    {
        try
        {
            logger.debug("arrived at " + toString());
            byte[] startData = new byte[STREAM_SAMPLE_SIZE];
            int startDataSize = 0;
            byte[] endData = new byte[STREAM_SAMPLE_SIZE]; // ring buffer
            int endDataSize = 0;
            int endRingPointer = 0;
            long streamLength = 0;
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];

            // throw data on the floor, but keep a record of size, start and end values
            int bytesRead = 0;
            while (bytesRead >= 0)
            {
                bytesRead = in.read(buffer);
                if (bytesRead > 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("read " + bytesRead + " bytes");
                    }
                    streamLength += bytesRead;
                    int startOfEndBytes = 0;
                    for (int i = 0; startDataSize < STREAM_SAMPLE_SIZE && i < bytesRead; ++i)
                    {
                        startData[startDataSize++] = buffer[i];
                        ++startOfEndBytes; // skip data included in startData
                    }
                    startOfEndBytes = Math.max(startOfEndBytes, bytesRead - STREAM_SAMPLE_SIZE);
                    for (int i = startOfEndBytes; i < bytesRead; ++i)
                    {
                        ++endDataSize;
                        endData[endRingPointer++ % STREAM_SAMPLE_SIZE] = buffer[i];
                    }
                    if (streamLength >= targetSize)
                    {
                        doCallback(startData, startDataSize,
                                endData, endDataSize, endRingPointer,
                                streamLength, context);
                    }
                }
            }

        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
            throw e;
        }
    }

    private void doCallback(byte[] startData, int startDataSize,
                            byte[] endData, int endDataSize, int endRingPointer,
                            long streamLength, UMOEventContext context) throws Exception
    {
        // make a nice summary of the data
        StringBuffer result = new StringBuffer("Received stream");
        result.append("; length: ");
        result.append(streamLength);
        result.append("; '");

        for (int i = 0; i < startDataSize; ++i)
        {
            result.append((char) startData[i]);
        }

        int endSize = Math.min(endDataSize, STREAM_SAMPLE_SIZE);
        if (endSize > 0)
        {
            result.append("...");
            for (int i = 0; i < endSize; ++i)
            {
                result.append((char) endData[(endRingPointer + i) % STREAM_SAMPLE_SIZE]);
            }
        }
        result.append("'");

        summary = result.toString();

        String msg = StringMessageUtils.getBoilerPlate("Message Received in component: "
                + context.getComponent().getName() + ". " + summary
                + "\n callback: " + eventCallback,
                '*', 80);

        logger.info(msg);

        if (eventCallback != null)
        {
            eventCallback.eventReceived(context, this);
        }
    }

    public String toString()
    {
        return ClassUtils.getSimpleName(getClass()) + "/" + number;
    }

}