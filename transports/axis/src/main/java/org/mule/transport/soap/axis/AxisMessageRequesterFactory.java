/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.AbstractMessageRequesterFactory;

/**
 * <code>AxisMessageRequesterFactory</code> creates an AxisMessageRequester, used
 * for making SOAP calls using the Axis stack.
 */
public class AxisMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    public MessageRequester create(InboundEndpoint endpoint) throws MuleException
    {
        return new AxisMessageRequester(endpoint);
    }
}
