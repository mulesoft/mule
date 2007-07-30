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

import org.mule.impl.ThreadSafeAccess;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEvent;
import org.mule.umo.provider.OutputHandler;
import org.mule.umo.provider.UMOStreamMessageAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides a generic base class for stream based message flows in Mule. This adapter
 * represents the 3 flows of data that Mule identifies, namely inbound, outbound and
 * response flows. These are represented by three streams on the adapter.
 * 
 */
public class StreamMessageAdapter extends AbstractMessageAdapter implements UMOStreamMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6794965828515586752L;

    protected InputStream in;
    protected OutputStream out;
    protected OutputHandler handler;
    private static NullPayload NULL_PAYLOAD = NullPayload.getInstance();

    public StreamMessageAdapter(InputStream in)
    {
        this.in = in; 
    }

    public StreamMessageAdapter(InputStream in, OutputStream out)
    {
        this.in = in;
        this.out = out;
    }

    public StreamMessageAdapter(OutputHandler handler)
    {
        this.handler = handler;
    }

    public StreamMessageAdapter(OutputStream out, OutputHandler handler)
    {
        this.out = out;
        this.handler = handler;
    }

    public StreamMessageAdapter(InputStream in, OutputStream out, OutputHandler handler)
    {
        this.in = in;
        this.out = out;
        this.handler = handler;
    }

    protected StreamMessageAdapter(StreamMessageAdapter template)
    {
        super(template);
        in = template.in;
        out = template.out;
        handler = template.handler;
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
        throw new UnsupportedOperationException("getPayloadAsString");
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        throw new UnsupportedOperationException("getPayloadAsBytes");
    }

    /**
     * This is an InputStream if triggered from an inbound event or response. If the
     * Message has a response stream it is assumed that the message the response
     * stream should be used. If the Message has been triggered from an outbound
     * request and NullPayload will be used
     * 
     * @return the current message
     */
    public Object getPayload()
    {
        if (in != null)
        {
            return in;
        }
        return NULL_PAYLOAD;
    }

    public InputStream getInputStream()
    {
        return in;
    }

    public OutputStream getOutputStream()
    {
        return out;
    }

    public void write(UMOEvent event) throws IOException
    {
        handler.write(event, out);
    }

    public OutputHandler getOutputHandler()
    {
        return handler;
    }

    public void setOutputHandler(OutputHandler handler)
    {
        this.handler = handler;
    }

    /**
     * The release method is called by Mule to notify this adapter that it is no
     * longer needed. This method can be used to release any resources that a custom
     * StreamAdapter may have associated with it.
     */
    public void release()
    {
        // nothing to do?
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new StreamMessageAdapter(this);
    }

}
