/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.endpoint;

import org.mule.api.transport.MessageDispatching;

import java.util.List;

public interface OutboundEndpoint extends ImmutableEndpoint,  MessageDispatching
{    
    /**
     * @return a list of properties which should be carried over from the request message to the response message
     * in the case of a synchronous call.
     */
    List<String> getResponseProperties();
}


