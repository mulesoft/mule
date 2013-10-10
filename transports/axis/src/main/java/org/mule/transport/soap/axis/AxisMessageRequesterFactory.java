/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
