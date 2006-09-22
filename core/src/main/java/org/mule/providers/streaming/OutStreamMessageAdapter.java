/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.streaming;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.StringMessageUtils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <code>StreamMessageAdapter</code> wraps an java.io.OutputStream and allows meta information
 * to be associated with the stream
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OutStreamMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -299373598028203772L;

    private OutputStream out;

    public OutStreamMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        try {
            if (message instanceof OutputStream) {
                out = (OutputStream)message;
            } else if (message instanceof String) {
                out = new ByteArrayOutputStream(message.toString().length());
                out.write(StringMessageUtils.getBytes(message.toString()));
            } else if (message instanceof byte[]) {
                out = new ByteArrayOutputStream(((byte[])message).length);
                out.write((byte[])message);
                   
            } else {
                throw new MessageTypeNotSupportedException(message, getClass());
            }
        } catch (IOException e) {
            throw new MessageTypeNotSupportedException(message, getClass(), e);
        }

    }

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
        if(out instanceof ByteArrayOutputStream) {
            return StringMessageUtils.getString(((ByteArrayOutputStream)out).toByteArray(), encoding);
        } else {
            logger.warn("Attempting to get the String contents of a non-ByteArray output stream");
            return out.toString();
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if(out instanceof ByteArrayOutputStream) {
            return ((ByteArrayOutputStream)out).toByteArray();
        } else {
            logger.warn("Attempting to get the bytes of a non-ByteArray output stream");
            return StringMessageUtils.getBytes(out.toString());
        }
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return out;
    }

    public void write(String string) throws IOException {
        out.write(StringMessageUtils.getBytes(string));
    }

    public void write(String string, int offset, int len) throws IOException {
        out.write(StringMessageUtils.getBytes(string), offset, len);
    }

    public void write(byte[] bytes) throws IOException {
        out.write(bytes);
    }

    public void write(byte[] bytes, int offset, int len) throws IOException {
        out.write(bytes, offset, len);
    }

    public OutputStream getStream()
    {
        return out;
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
