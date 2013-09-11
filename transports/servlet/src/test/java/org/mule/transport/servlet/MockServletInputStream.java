/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

