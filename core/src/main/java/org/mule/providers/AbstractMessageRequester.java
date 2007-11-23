/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOMessageRequester;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractMessageRequester extends AbstractConnectable implements UMOMessageRequester
{
    
    public AbstractMessageRequester(UMOImmutableEndpoint endpoint)
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
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    public final UMOMessage request(long timeout) throws Exception
    {
        try
        {
            // Make sure we are connected
            connectionStrategy.connect(this);
            UMOMessage result = doRequest(timeout);
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
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected abstract UMOMessage doRequest(long timeout) throws Exception;

}