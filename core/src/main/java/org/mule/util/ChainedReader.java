/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.IOException;
import java.io.Reader;

/**
 * <code>ChainedReader</code> allows Reader objects to be chained together. Useful
 * for concatenating data from multiple Reader objects.
 */
public class ChainedReader extends Reader
{
    private final Reader first;
    private final Reader second;
    private boolean firstRead = false;

    public ChainedReader(Reader first, Reader second)
    {
        this.first = first;
        this.second = second;
    }

    public void close() throws IOException
    {
        first.close();
        second.close();
    }

    public int read(char[] cbuf, int off, int len) throws IOException
    {
        if (!firstRead)
        {
            int i = first.read(cbuf, off, len);
            if (i < len)
            {
                firstRead = true;
                int x = second.read(cbuf, i, len - i);
                return x + i;
            }
            else
            {
                return i;
            }
        }
        else
        {
            return second.read(cbuf, off, len);
        }
    }

    public int read() throws IOException
    {
        if (!firstRead)
        {
            int i = first.read();
            if (i == -1)
            {
                firstRead = true;
                return second.read();
            }
            else
            {
                return i;
            }
        }
        else
        {
            return second.read();
        }
    }

    public int read(char[] cbuf) throws IOException
    {
        if (!firstRead)
        {
            int i = first.read(cbuf);
            if (i < cbuf.length)
            {
                firstRead = true;
                int x = second.read(cbuf, i, cbuf.length - i);
                return x + i;
            }
            else
            {
                return i;
            }
        }
        else
        {
            return second.read(cbuf);
        }
    }

}
