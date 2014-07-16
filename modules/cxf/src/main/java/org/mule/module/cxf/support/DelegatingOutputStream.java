/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import java.io.IOException;
import java.io.OutputStream;

public class DelegatingOutputStream extends OutputStream
{
    private OutputStream outputStream;


    public DelegatingOutputStream(OutputStream outputStream)
    {
        super();
        this.outputStream = outputStream;
    }
    
    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public void close() throws IOException
    {
        outputStream.close();
    }

    public boolean equals(Object obj)
    {
        return outputStream.equals(obj);
    }

    public void flush() throws IOException
    {
        outputStream.flush();
    }

    public int hashCode()
    {
        return outputStream.hashCode();
    }

    public String toString()
    {
        return outputStream.toString();
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        outputStream.write(b, off, len);
    }

    public void write(byte[] b) throws IOException
    {
        outputStream.write(b);
    }

    public void write(int b) throws IOException
    {
        outputStream.write(b);
    }
    
}


