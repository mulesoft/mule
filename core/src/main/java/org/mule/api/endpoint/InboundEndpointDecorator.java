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

import org.mule.api.MuleException;
import org.mule.api.service.Service;

/**
 * Used on custom endpoints that provide additional processing to service or message to handle the way certain
 * types of messages are handled.  For example, the ATOM endpoint adds an inbound router and filter for splitting
 * atom feeds.
 */
public interface InboundEndpointDecorator extends OutboundEndpointDecorator
{
    /**
     * A call back that is invoked when an inbound endpoint is registered with a service.  this allows the service to be
     * modified.  THis is only used when the endpoint needs an inbound router of filter will be added by the endpoint to change the flow of the message
     * processing
     *
     * @param service the service that the endpoint was registered with
     * @throws org.mule.api.MuleException if something fails while manipulating the service
     */
    void onListenerAdded(Service service) throws MuleException;

}