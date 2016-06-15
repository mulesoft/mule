/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that can be used by streaming functional tests. This service accepts an
 * EventCallback that can be used to assert the state of the current event.  To access the
 * service when embedded in an (XML) model, make sure that the descriptor sets the
 * singleton attribute true - see uses in TCP and FTP.
 *
 * Note that although this implements the full StreamingService interface, nothing is
 * written to the output stream - this is intended as a final sink.
 *
 * @see EventCallback
 */

public class FunctionalStreamingTestComponent implements Callable
{
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

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

    public Object onCall(MuleEventContext context) throws Exception
    {
        InputStream in = (InputStream) context.getMuleContext().getTransformationService().transform(context.getMessage(),
                                                                                                     DataTypeFactory.create(InputStream.class)).getPayload();
        try
        {
            logger.debug("arrived at " + toString());
            byte[] startData = new byte[STREAM_SAMPLE_SIZE];
            long startDataSize = 0;
            byte[] endData = new byte[STREAM_SAMPLE_SIZE]; // ring buffer
            long endDataSize = 0;
            long endRingPointer = 0;
            long streamLength = 0;
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];

            // throw data on the floor, but keep a record of size, start and end values
            long bytesRead = 0;
            while (bytesRead >= 0)
            {
                bytesRead = read(in, buffer);
                if (bytesRead > 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("read " + bytesRead + " bytes");
                    }
                    
                    streamLength += bytesRead;
                    long startOfEndBytes = 0;
                    for (long i = 0; startDataSize < STREAM_SAMPLE_SIZE && i < bytesRead; ++i)
                    {
                        startData[(int) startDataSize++] = buffer[(int) i];
                        ++startOfEndBytes; // skip data included in startData
                    }
                    startOfEndBytes = Math.max(startOfEndBytes, bytesRead - STREAM_SAMPLE_SIZE);
                    for (long i = startOfEndBytes; i < bytesRead; ++i)
                    {
                        ++endDataSize;
                        endData[(int) (endRingPointer++ % STREAM_SAMPLE_SIZE)] = buffer[(int) i];
                    }
                    if (streamLength >= targetSize)
                    {
                        doCallback(startData, startDataSize,
                                endData, endDataSize, endRingPointer,
                                streamLength, context);
                    }
                }
            }

            in.close();
        }
        catch (Exception e)
        {
            in.close();
            
            e.printStackTrace();
            if (logger.isDebugEnabled())
            {
                logger.debug("Error on test component", e);
            }
            throw e;
        }
        
        return null;
    }

    protected int read(InputStream in, byte[] buffer) throws IOException
    {
        return in.read(buffer);
    }

    private void doCallback(byte[] startData, long startDataSize,
                            byte[] endData, long endDataSize, long endRingPointer,
                            long streamLength, MuleEventContext context) throws Exception
    {
        // make a nice summary of the data
        StringBuilder result = new StringBuilder("Received stream");
        result.append("; length: ");
        result.append(streamLength);
        result.append("; '");

        for (long i = 0; i < startDataSize; ++i)
        {
            result.append((char) startData[(int) i]);
        }

        long endSize = Math.min(endDataSize, STREAM_SAMPLE_SIZE);
        if (endSize > 0)
        {
            result.append("...");
            for (long i = 0; i < endSize; ++i)
            {
                result.append((char) endData[(int) ((endRingPointer + i) % STREAM_SAMPLE_SIZE)]);
            }
        }
        result.append("'");

        summary = result.toString();

        String msg = StringMessageUtils.getBoilerPlate("Message Received in service: "
                + context.getFlowConstruct().getName() + ". " + summary
                + "\n callback: " + eventCallback,
                '*', 80);

        logger.info(msg);

        if (eventCallback != null)
        {
            eventCallback.eventReceived(context, this);
        }
    }

    @Override
    public String toString()
    {
        return ClassUtils.getSimpleName(getClass()) + "/" + number;
    }

}
