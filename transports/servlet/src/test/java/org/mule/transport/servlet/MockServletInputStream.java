/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class MockServletInputStream extends ServletInputStream
{
    private InputStream input;

    public MockServletInputStream(InputStream dataStream)
    {
        super();
        input = dataStream;
    }

    public MockServletInputStream()
    {
        this(new ByteArrayInputStream(new byte[0]));
    }

    @Override
    public int read() throws IOException
    {
        return input.read();
    }
}

