/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message.ds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * {@link DataSource} wapper for an {@link InputStream}.
 */
public class InputStreamDataSource implements DataSource
{

    private final InputStream data;
    private final String contentType;
    private final String name;

    public InputStreamDataSource(InputStream data, String contentType, String name)
    {
        this.data = data;
        this.contentType = contentType;
        this.name = name;
    }

    @Override
    public String getContentType()
    {
        return contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return data;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        throw new IOException("Cannot write into an InputStreamDataSource");
    }

}
