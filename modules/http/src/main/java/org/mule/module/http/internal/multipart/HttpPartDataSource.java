/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.multipart;

import org.mule.api.MuleRuntimeException;
import org.mule.message.ds.ByteArrayDataSource;
import org.mule.message.ds.StringDataSource;
import org.mule.module.http.internal.HttpParam;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.servlet.http.Part;

public class HttpPartDataSource implements DataSource
{

    private final HttpPart part;
    private byte[] content;

    private HttpPartDataSource(HttpPart part)
    {
        try
        {
            this.part = part;
            this.content = IOUtils.toByteArray(part.getInputStream());
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public byte[] getContent() throws IOException
    {
        return this.content;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return new ByteArrayInputStream(getContent());
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType()
    {
        return part.getContentType();
    }

    public String getHeader(String headerName)
    {
        return part.getHeader(headerName);
    }

    @Override
    public String getName()
    {
        return part.getName();
    }

    public static Collection<HttpPartDataSource> createFrom(Collection<HttpPart> parts)
    {
        final ArrayList<HttpPartDataSource> httpParts = new ArrayList<>(parts.size());
        for (HttpPart part : parts)
        {
            httpParts.add(new HttpPartDataSource(part));
        }
        return httpParts;
    }

    public static Map<String, DataHandler> createDataHandlerFrom(Collection<HttpPart> parts)
    {
        final Map<String, DataHandler> httpParts = new HashMap<>(parts.size());
        for (HttpPart part : parts)
        {
            httpParts.put(part.getName(), new DataHandler(new HttpPartDataSource(part)));
        }
        return httpParts;
    }

    public static Collection<HttpPart> createFrom(Map<String, DataHandler> parts) throws IOException
    {
        final ArrayList<HttpPart> httpParts = new ArrayList<>(parts.size());
        for (String partName : parts.keySet())
        {
            final DataHandler dataHandlerPart = parts.get(partName);
            if (dataHandlerPart.getDataSource() instanceof HttpPartDataSource)
            {
                httpParts.add(((HttpPartDataSource) dataHandlerPart.getDataSource()).getPart());
            }
            else
            {
                byte[] data = IOUtils.toByteArray(dataHandlerPart.getInputStream());
                String fileName = null;

                if (dataHandlerPart.getDataSource() instanceof FileDataSource || dataHandlerPart.getDataSource() instanceof ByteArrayDataSource)
                {
                    fileName = dataHandlerPart.getDataSource().getName();
                }
                httpParts.add(new HttpPart(partName, fileName, data, dataHandlerPart.getContentType(), data.length));
            }
        }
        return httpParts;
    }

    public HttpPart getPart()
    {
        return part;
    }
}
