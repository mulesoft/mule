/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.io.IOException;
import java.io.InputStream;

public class NonSignallingInputStream extends InputStream
{

    private InputStream inputStream;

    public NonSignallingInputStream(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public int read() throws IOException
    {
        return inputStream.read();
    }

    // @Override
    public int read(byte b[]) throws IOException
    {
        return inputStream.read(b);
    }

    // @Override
    public int read(byte b[], int off, int len) throws IOException
    {
        return inputStream.read(b, off, len);
    }

    // @Override
    public void close() throws IOException
    {
        super.close();
    }
    
}
