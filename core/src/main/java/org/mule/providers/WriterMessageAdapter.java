/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * <code>WriterMessageAdapter</code> wraps a java.io.StringWriter and allows meta
 * information to be associated with the Writer.
 */
public class WriterMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1065602752454818625L;

    private final StringWriter writer;

    public WriterMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof String)
        {
            writer = new StringWriter();
            writer.write((String)message);
        }
        else if (message instanceof StringWriter)
        {
            this.writer = (StringWriter)message;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }

    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        return writer.toString();
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return writer.toString().getBytes();
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return writer.toString();
    }

    public void write(String string)
    {
        writer.write(string);
    }

    public void write(String string, int offset, int len)
    {
        writer.write(string, offset, len);
    }

    public Writer getWriter()
    {
        return writer;
    }

    public void flush()
    {
        writer.flush();
    }

    public void close() throws IOException
    {
        writer.close();
    }
}
