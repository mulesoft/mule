/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.transport.AbstractMessageDispatcherFactory;

/**
 * <code>JmsMessageDispatcherFactory</code> creates a message adapter that will
 * send JMS messages
 */
public class JmsMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{

    @Override
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new JmsMessageDispatcher(endpoint);
    }

}
