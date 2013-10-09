/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * A Test message dispatcher factory that is used for testing configuration loading since "test://"
 * endpoints can be used instead of bringing in other dependencies into Mule core
 */
public class TestMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{

    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new TestMessageDispatcher(endpoint);
    }
}
