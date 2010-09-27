/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * Creates a WorkflowMessageDispatcher instance.
 * 
 * @deprecated It is recommended to configure BPM as a component rather than a transport for 3.x
 */
public class ProcessMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new ProcessMessageDispatcher(endpoint);
    }
}
