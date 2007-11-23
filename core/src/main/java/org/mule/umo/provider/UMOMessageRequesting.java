/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

/**
 * Defines generic methods for receiving events.
 * The exact behaviour of the action is defined by the implementing class.
 *
 * @see org.mule.umo.endpoint.UMOImmutableEndpoint
 * @see UMOMessageRequester
 */
public interface UMOMessageRequesting
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
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal causes an exception
     */
    UMOMessage request(long timeout) throws Exception;

}