/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleMessage;

/**
 * Defines generic methods for receiving events.
 * The exact behaviour of the action is defined by the implementing class.
 *
 * @see org.mule.api.endpoint.ImmutableEndpoint
 * @see MessageRequester
 */
public interface MessageRequesting
{
    
    long REQUEST_WAIT_INDEFINITELY = 0;
    long REQUEST_NO_WAIT = -1;

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal causes an exception
     */
    MuleMessage request(long timeout) throws Exception;

}
