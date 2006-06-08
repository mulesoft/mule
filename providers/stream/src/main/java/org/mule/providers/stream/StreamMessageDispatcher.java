/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.stream;

import org.mule.config.i18n.Message;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;

import java.io.OutputStream;

/**
 * <code>StreamMessageDispatcher</code> A simple stream dispatcher that
 * obtains a stream from the Stream Connector to write to. This only really
 * useful for testing purposes right now when writing to System.in and
 * System.out. However, it is feesable to set any outputstream on the Stream
 * connector and have that written to.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class StreamMessageDispatcher extends AbstractMessageDispatcher
{

    private StreamConnector connector;

    public StreamMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (StreamConnector)endpoint.getConnector();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#dispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception
    {
        OutputStream out = null;
        String streamName = event.getEndpoint().getEndpointURI().getAddress();
        if (StreamConnector.STREAM_SYSTEM_OUT.equalsIgnoreCase(streamName)) {
            out = System.out;
        }
        else if (StreamConnector.STREAM_SYSTEM_ERR.equalsIgnoreCase(streamName)) {
            out = System.err;
        }
        else {
            out = connector.getOutputStream();
        }

        if (out == null) {
            throw new DispatchException(new Message("stream", 1, streamName), event.getMessage(), event
                    .getEndpoint());
        }
        Object data = event.getTransformedMessage();
        if (data instanceof byte[]) {
            out.write((byte[])data);
        }
        else {
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
     * @param endpoint
     *            the endpoint to use when connecting to the resource
     * @param timeout
     *            the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available.
     *            If no data becomes available before the timeout elapses, null
     *            will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null
     *         will be returned if no data was avaialable
     * @throws Exception
     *             if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }
}
