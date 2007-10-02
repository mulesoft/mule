/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.tcp.TcpProtocol;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;

/**
 * This Abstract class has been introduced so as to have the byte protocols (i.e. the
 * protocols that had only a single write method taking just an array of bytes as a
 * parameter) to inherit from since they will all behave the same, i.e. if the object
 * is serializable, serialize it into an array of bytes and send it.
 *
 * <p>Note that the raw write method has changed name from <code>write</code> to
 * <code>writeByteArray</code>.  This is to remove ambiguity from the code.  In almost
 * all cases it is possible to call {@link #write(java.io.OutputStream, Object)} which
 * will, via {@link #write(java.io.OutputStream, Object)}, dispatch to
 * {@link #writeByteArray(java.io.OutputStream, byte[])}.</p>.
 */
public abstract class ByteProtocol implements TcpProtocol
{
    private static final Log logger = LogFactory.getLog(DefaultProtocol.class);
    private static final long PAUSE_PERIOD = 100;
    public static final int EOF = -1;

    // make this really clear in subclasses, because otherwise people will forget
    public static final boolean STREAM_OK = true;
    public static final boolean NO_STREAM = false;
    private boolean streamOk;

    public ByteProtocol(boolean streamOk)
    {
        this.streamOk = streamOk;
    }

    public void write(OutputStream os, Object data) throws IOException
    {
        if (data instanceof InputStream)
        {
            if (streamOk)
            {
                copyStream((InputStream)data, os);
            }
            else
            {
                throw new IOException("TCP protocol " + ClassUtils.getSimpleName(getClass())
                        + " cannot handle streaming");
            }
        }
        else if (data instanceof UMOMessageAdapter)
        {
            write(os, ((UMOMessageAdapter) data).getPayload());
        }
        else if (data instanceof byte[])
        {
            writeByteArray(os, (byte[]) data);
        }
        else if (data instanceof String)
        {
            // TODO SF: encoding is lost/ignored; it is probably a good idea to have
            // a separate "stringEncoding" property on the protocol
            String s = (String) data;
            writeByteArray(os, s.getBytes(getStringEncoding(s)));
        }
        else if (data instanceof Serializable)
        {
            writeByteArray(os, SerializationUtils.serialize((Serializable) data));
        }
        else
        {
            throw new IllegalArgumentException("Cannot serialize data: " + data);
        }
    }

    protected void copyStream(InputStream is, OutputStream os) throws IOException
    {
        IOUtils.copy((InputStream) is, os);
    }

    /**
     * Override this if you'd like to change the String encoding for a message.
     * @return
     */
    protected String getStringEncoding(String s)
    {
        return "UTF-8";
    }

    protected void writeByteArray(OutputStream os, byte[] data) throws IOException
    {
        os.write(data);
    }

    /**
     * Manage non-blocking reads and handle errors
     *
     * @param is The input stream to read from
     * @param buffer The buffer to read into
     * @return The amount of data read (always non-zero, -1 on EOF or socket exception)
     * @throws IOException other than socket exceptions
     */
    protected int safeRead(InputStream is, byte[] buffer) throws IOException
    {
        return safeRead(is, buffer, buffer.length);
    }

    /**
     * Manage non-blocking reads and handle errors
     *
     * @param is The input stream to read from
     * @param buffer The buffer to read into
     * @param size The amount of data (upper bound) to read
     * @return The amount of data read (always non-zero, -1 on EOF or socket exception)
     * @throws IOException other than socket exceptions
     */
    protected int safeRead(InputStream is, byte[] buffer, int size) throws IOException
    {
        int len;
        try
        {
            do
            {
                len = is.read(buffer, 0, size);
                if (0 == len)
                {
                    // wait for non-blocking input stream
                    // use new lock since not expecting notification
                    try
                    {
                        Thread.sleep(PAUSE_PERIOD);
                    }
                    catch (InterruptedException e)
                    {
                        // no-op
                    }
                }
            }
            while (0 == len);
            return len;
        }
        catch (SocketException e)
        {
            // do not pollute the log with a stacktrace, log only the message
            logger.info("Socket exception occured: " + e.getMessage());
            return EOF;
        }
        catch (SocketTimeoutException e)
        {
            logger.debug("Socket timeout.");
            return EOF;
        }
    }

    /**
     * Make a single transfer from source to dest via a byte array buffer
     *
     * @param source Source of data
     * @param buffer Buffer array for transfer
     * @param dest Destination of data
     * @return Amount of data transferred, or -1 on eof or socket error
     * @throws IOException On non-socket error
     */
    protected int copy(InputStream source, byte[] buffer, OutputStream dest) throws IOException
    {
        return copy(source, buffer, dest, buffer.length);
    }

    /**
     * Make a single transfer from source to dest via a byte array buffer
     *
     * @param source Source of data
     * @param buffer Buffer array for transfer
     * @param dest Destination of data
     * @param size The amount of data (upper bound) to read
     * @return Amount of data transferred, or -1 on eof or socket error
     * @throws IOException On non-socket error
     */
    protected int copy(InputStream source, byte[] buffer, OutputStream dest, int size) throws IOException
    {
        int len = safeRead(source, buffer, size);
        if (len > 0)
        {
            dest.write(buffer, 0, len);
        }
        return len;
    }

    protected byte[] nullEmptyArray(byte[] data)
    {
        if (0 == data.length)
        {
            return null;
        }
        else
        {
            return data;
        }
    }

}
