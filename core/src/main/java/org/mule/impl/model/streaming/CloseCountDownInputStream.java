/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.streaming;

import java.io.IOException;
import java.io.InputStream;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

/**
 * Decrements the latch on close.
 */
public class CloseCountDownInputStream extends InputStream
{

    private InputStream delegate;
    private CountDownLatch latch;

    public CloseCountDownInputStream(InputStream delegate, CountDownLatch latch)
    {
        this.delegate = delegate;
        this.latch = latch;
    }

    public int read() throws IOException
    {
        return delegate.read();
    }

    public int read(byte b[]) throws IOException
    {
        return delegate.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException
    {
        return delegate.read(b, off, len);
    }

    public void close() throws IOException
    {
        try
        {
            delegate.close();
        }
        finally
        {
            if (null != latch)
            {
                latch.countDown();
            }
        }
    }

}
