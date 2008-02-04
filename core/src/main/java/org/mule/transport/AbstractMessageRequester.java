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
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.ReceiveException;
import org.mule.context.notification.MessageNotification;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractMessageRequester extends AbstractConnectable implements MessageRequester
{
    
    public AbstractMessageRequester(ImmutableEndpoint endpoint)
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
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    public final MuleMessage request(long timeout) throws Exception
    {
        try
        {
            // Make sure we are connected
            connectionStrategy.connect(this);
            if (connector.isEnableMessageEvents())
            {
                connector.fireNotification(new MessageNotification(null, endpoint, null,
                    MessageNotification.MESSAGE_REQUESTED));
            }
            MuleMessage result = doRequest(timeout);
            if (result != null && connector.isEnableMessageEvents())
            {
                connector.fireNotification(new MessageNotification(result, endpoint, null,
                    MessageNotification.MESSAGE_RECEIVED));
            }
            return result;
        }
        catch (DispatchException e)
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