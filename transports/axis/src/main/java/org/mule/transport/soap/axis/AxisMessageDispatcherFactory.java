/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * <code>AxisMessageDispatcherFactory</code> creates an AxisMessageDispatcher, used
 * for making SOAP calls using the Axis stack.
 */
public class AxisMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new AxisMessageDispatcher(endpoint);
    }
}
