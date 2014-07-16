/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class PlainTextDataSource implements DataSource
{
    public static final String CONTENT_TYPE = "text/plain";

    private final String name;
    private byte[] data;
    private ByteArrayOutputStream os;

    // FIXME: Doesn't work with non ascii string, but now it's ok. 
    public PlainTextDataSource(String name, String data)
    {
        this.name = name;
        this.data = data == null ? null : data.getBytes();
        os = new ByteArrayOutputStream();
    } // ctor

    public String getName()
    {
        return name;
    } // getName

    public String getContentType()
    {
        return CONTENT_TYPE;
    } // getContentType

    public InputStream getInputStream() throws IOException
    {
        if (os.size() != 0)
        {
            data = os.toByteArray();
        }
        return new ByteArrayInputStream(data == null ? new byte[0] : data);
    } // getInputStream

    public OutputStream getOutputStream() throws IOException
    {
        if (os.size() != 0)
        {
            data = os.toByteArray();
        }
        return new ByteArrayOutputStream();
    } // getOutputStream

} // class PlainTextDataSource
