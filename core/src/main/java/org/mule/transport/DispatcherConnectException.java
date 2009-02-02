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

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.Message;

/** 
 * When this exception is thrown it will trigger a retry (reconnection) policy to go into effect if one is configured.
 */
public class DispatcherConnectException extends ConnectException
{
    public DispatcherConnectException(Message message, OutboundEndpoint endpoint)
    {
        super(message, endpoint);
    }

    public DispatcherConnectException(Message message, Throwable cause, OutboundEndpoint endpoint)
    {
        super(message, cause, endpoint);
    }

    public DispatcherConnectException(Throwable cause, OutboundEndpoint endpoint)
    {
        super(cause, endpoint);
    }
}
