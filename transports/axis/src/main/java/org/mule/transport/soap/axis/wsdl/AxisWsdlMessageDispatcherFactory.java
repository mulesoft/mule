/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.wsdl;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * Creates an WSDL Message dispatcher using the Axis client
 */
public class AxisWsdlMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new AxisWsdlMessageDispatcher(endpoint);
    }
}
