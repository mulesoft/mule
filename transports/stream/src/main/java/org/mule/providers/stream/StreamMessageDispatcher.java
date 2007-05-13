/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.stream;

import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.stream.i18n.StreamMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.util.StringUtils;

import java.io.OutputStream;

/**
 * <code>StreamMessageDispatcher</code> is a simple stream dispatcher that obtains
 * a stream from the Stream Connector to write to. This is only really useful for
 * testing purposes right now when writing to System.in and System.out. However, it
 * is feasible to set any OutputStream on the Stream connector and have that written
 * to.
 */

public class StreamMessageDispatcher extends AbstractMessageDispatcher
{
    private final StreamConnector connector;

    public StreamMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (StreamConnector)endpoint.getConnector();

        // apply connector-specific properties
        if (connector instanceof SystemStreamConnector)
        {
            SystemStreamConnector ssc = (SystemStreamConnector)connector;

            String outputMessage = (String)endpoint.getProperties().get("outputMessage");
            if (outputMessage != null)
            {
                ssc.setOutputMessage(outputMessage);
            }
        }
    }

    protected synchronized void doDispatch(UMOEvent event) throws Exception
    {
        OutputStream out = connector.getOutputStream();

        if (out == null)
        {
            throw new DispatchException(
                StreamMessages.couldNotFindStreamWithName(event.getEndpoint().getEndpointURI().getAddress()), 
                event.getMessage(), event.getEndpoint());
        }

        if (connector instanceof SystemStreamConnector)
        {
            SystemStreamConnector ssc = (SystemStreamConnector)connector;
            if (StringUtils.isNotBlank(ssc.getOutputMessage()))
            {
                out.write(ssc.getOutputMessage().getBytes());
            }
        }

        Object data = event.getTransformedMessage();
        if (data instanceof byte[])
        {
            out.write((byte[])data);
        }
        else
        {
            out.write(data.toString().getBytes());
        }

        out.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#send(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }



}
