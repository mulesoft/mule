/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * TODO
 */
public class PartDataSource implements DataSource
{
    private Part part;

    public PartDataSource(Part part)
    {
        this.part = part;
    }

    public InputStream getInputStream() throws IOException
    {
        return part.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException
    {
        throw new UnsupportedOperationException("getOutputStream");
    }

    public String getContentType()
    {
        return part.getContentType();
    }

    public String getName()
    {
        return part.getName();
    }

    public Part getPart()
    {
        return part;
    }
}
