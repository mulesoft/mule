/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers;

import java.io.StringWriter;
import java.io.Writer;

import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * <code>WriterMessageAdapter</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class WriterMessageAdapter extends AbstractMessageAdapter
{
    private StringWriter writer;

    public WriterMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof String) {
            writer = new StringWriter();
            writer.write((String) message);
        } else if (message instanceof StringWriter) {
            this.writer = (StringWriter) message;
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }

    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception
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

}
