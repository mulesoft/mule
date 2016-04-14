/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class StringDataSource implements DataSource
{
    protected String content;
    protected String contentType = "text/plain";
    protected String name = "StringDataSource";

    public StringDataSource(String payload)
    {
        super();
        content = payload;
    }

    public StringDataSource(String payload, String name)
    {
        super();
        content = payload;
        this.name = name;
    }

    public StringDataSource(String content, String name, String contentType)
    {
        this.content = content;
        this.contentType = contentType;
        this.name = name;
    }

    public InputStream getInputStream() throws IOException
    {
        return new ByteArrayInputStream(content.getBytes());
    }

    public OutputStream getOutputStream()
    {
        throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getName()
    {
        return name;
    }
}

