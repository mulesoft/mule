/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


