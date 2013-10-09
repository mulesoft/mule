/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
