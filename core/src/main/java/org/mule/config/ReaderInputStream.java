/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ReaderInputStream extends InputStream
{
    private Reader reader = null;

    public ReaderInputStream(Reader reader)
    {
        this.reader = reader;
    }

    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException
    {
        return reader.read();
    }
}
