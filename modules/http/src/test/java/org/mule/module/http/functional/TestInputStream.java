/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
* An input stream that returns a chunk of data and then blocks on a latch to ensure streaming in http transfers.
*/
public class TestInputStream extends InputStream
{

    private static final int RECEIVE_TIMEOUT = 5000;

    private final Latch latch;
    private int chunkCount = 0;

    public TestInputStream(final Latch latch)
    {
        this.latch = latch;
    }

    @Override
    public int read() throws IOException
    {
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        chunkCount++;
        if(chunkCount==1)
        {
            return b.length;
        }
        waitOnLatch();
        return -1;
    }

    private void waitOnLatch() throws IOException
    {
        if(latch!=null)
        {
            try
            {
                if (!latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
                {
                    throw new IOException("Latch was never released");
                }
            }
            catch (InterruptedException e)
            {
                throw new IOException(e);
            }
        }
    }
}
