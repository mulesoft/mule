/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.streaming;

import java.io.OutputStream;
import java.io.IOException;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

/**
 * Decrements the latch on close.
 */
public class CloseCountDownOutputStream extends OutputStream
{

    private OutputStream delegate;
    private CountDownLatch latch;

    public CloseCountDownOutputStream(OutputStream delegate, CountDownLatch latch)
    {
        this.delegate = delegate;
        this.latch = latch;
    }

    public void write(int b) throws IOException
    {
        delegate.write(b);
    }

    public void write(byte b[]) throws IOException
    {
        delegate.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        delegate.write(b, off, len);
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
