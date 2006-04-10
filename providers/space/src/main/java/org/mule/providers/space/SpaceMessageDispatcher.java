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

package org.mule.providers.space;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.space.UMOSpace;

/**
 * <code>SpaceMessageDispatcher</code> Provides generic connectivity to
 * 'Spaces' that implement the Mule Space Api, i.e. Gigaspaces, JCache imples,
 * Rio can be accessed as well as a mule file, Journal or VM space.
 * 
 * The dispatcher allows Mule to dispatch events synchronously and asynchronusly
 * to a space as well as make receive calls to the space.
 * 
 * @version $Revision$
 */

public class SpaceMessageDispatcher extends AbstractMessageDispatcher
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private UMOSpace space;
    private SpaceConnector connector;

    public SpaceMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (SpaceConnector)endpoint.getConnector();
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        if (space == null) {
            space = connector.getSpace(endpoint);
        }
    }

    protected void doDisconnect() throws Exception
    {
        try {
            space.dispose();
        }
        finally {
            space = null;
        }
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        space.put(event.getTransformedMessage(), event.getTimeout());
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return null;
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
        String destination = endpoint.getEndpointURI().toString();

        if (logger.isInfoEnabled()) {
            logger.info("Connecting to space '" + destination + "'");
        }
        UMOSpace space = connector.getSpace(destination);

        Object result = space.take(timeout);
        if (result == null) {
            return null;
        }
        UMOMessage message = new MuleMessage(connector.getMessageAdapter(result));
        return message;
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    protected void doDispose()
    {
        // template method
    }

}
