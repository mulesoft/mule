/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
