/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

