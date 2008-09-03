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

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.ReceiveException;
import org.mule.context.notification.EndpointMessageNotification;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractMessageRequester extends AbstractConnectable implements MessageRequester
{
    
    public AbstractMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
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
        // Variable needs to be final in order to use it inside callback
        final long finalTimeout = timeout;
        try
        {
            RetryContext context = retryTemplate.execute(new RetryCallback()
            {
                public void doWork(RetryContext context) throws Exception
                {
                    // Make sure we are connected
                    connect();
                    MuleMessage result = doRequest(finalTimeout);
                    if (result != null && connector.isEnableMessageEvents())
                    {
                        connector.fireNotification(new EndpointMessageNotification(result, endpoint, null,
                            EndpointMessageNotification.MESSAGE_REQUESTED));
                    }
                    context.addReturnMessage(result);
                    // Is there any difference ?
                    //context.setReturnMessages(new MuleMessage[]{result});
                }

                public String getWorkDescription()
                {
                    return getConnectionDescription();
                }
            });
            return context.getFirstReturnMessage();
        }
        catch (Exception e)
        {
            disposeAndLogException();
            throw new ReceiveException(endpoint, timeout, e);
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
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected abstract MuleMessage doRequest(long timeout) throws Exception;

}