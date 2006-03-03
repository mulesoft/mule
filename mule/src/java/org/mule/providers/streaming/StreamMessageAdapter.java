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
package org.mule.providers.streaming;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StreamMessageAdapter extends AbstractMessageAdapter {

    protected InputStream in;
    protected InputStream response;
    protected OutputStream out;
    protected OutputHandler handler;
    private NullPayload nullPayload = new NullPayload();

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
        throw new UnsupportedOperationException("getPayloadAsString");
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception {
        throw new UnsupportedOperationException("getPayloadAsBytes");
    }

    /**
     * This is an InputStream if triggered from an inbound event or response.
     * If the Message has a response stream it is assumed
     * that the message the response stream should be used.
     * If the Message has been triggered from an outbound request and NullPayload will be used
     * @return the current message
     */
    public Object getPayload() {
        if(response!=null) {
            return response;
        }
        if(in!=null) {
            return in;
        }
        return nullPayload;
    }

    public StreamMessageAdapter(InputStream in) {
        this.in = in;
    }

    public StreamMessageAdapter(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public StreamMessageAdapter(OutputHandler handler) {
        this.handler = handler;
    }
    public StreamMessageAdapter(OutputStream out, OutputHandler handler) {
        this.out = out;
        this.handler = handler;
    }
    public StreamMessageAdapter(InputStream in, OutputStream out, OutputHandler handler) {
        this.in = in;
        this.out = out;
        this.handler = handler;
    }

    public InputStream getInput() {
        return in;
    }

    public OutputStream getOutput() {
        return out;
    }

    public void write(UMOEvent event, OutputStream out) throws IOException
    {
        handler.write(event, out);
    }


    public OutputHandler getOutputHandler() {
        return handler;
    }

    public void setOutputHandler(OutputHandler handler) {
        this.handler = handler;
    }

    public InputStream getResponse() {
        return response;
    }

    public void setResponse(InputStream response) {
        this.response = response;
    }

    public boolean hasResponse() {
        return response!=null;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }
}
