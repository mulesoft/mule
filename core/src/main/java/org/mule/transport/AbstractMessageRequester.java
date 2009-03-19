/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.ReceiveException;
import org.mule.context.notification.EndpointMessageNotification;

/**
 * The Message Requester is used to explicitly request messages from a message channel or
 * resource rather than subscribing to inbound events or polling for messages.
 * This is often used programatically but will not be used for inbound endpoints
 * configured on services.
 */
public abstract class AbstractMessageRequester extends AbstractConnectable implements MessageRequester
{
    public AbstractMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    public final void initialise() throws InitialisationException
    {
        super.initialise();
        doInitialise();
    }

    @Override
    public final synchronized void dispose()
    {
        super.dispose();
        try
        {
            doDispose();
        }
        finally
        {
            disposed.set(true);
        }
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    public final MuleMessage request(long timeout) throws Exception
    {
        try
        {
            // Make sure we are connected
            connect();
            MuleMessage result = null;
            result = doRequest(timeout);

            if (result != null && connector.isEnableMessageEvents())
            {
                connector.fireNotification(new EndpointMessageNotification(result, endpoint, null,
                    EndpointMessageNotification.MESSAGE_REQUESTED));
            }
            return result;
        }
        catch (ReceiveException e)
        {
            disposeAndLogException();
            throw e;
        }
        catch (Exception e)
        {
            disposeAndLogException();
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    @Override
    protected WorkManager getWorkManager()
    {
        try
        {
            return connector.getRequesterWorkManager();
        }
        catch (MuleException e)
        {
            handleException(e);
            return null;
        }
    }
    
    public InboundEndpoint getEndpoint()
    {
        return (InboundEndpoint) super.getEndpoint();
    }
    
    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected abstract MuleMessage doRequest(long timeout) throws Exception;

}