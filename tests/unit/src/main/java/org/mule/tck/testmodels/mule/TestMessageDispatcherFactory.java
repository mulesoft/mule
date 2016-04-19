/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.transport.AbstractMessageDispatcherFactory;

/**
 * A Test message dispatcher factory that is used for testing configuration loading since "test://"
 * endpoints can be used instead of bringing in other dependencies into Mule core
 */
public class TestMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{

    @Override
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new TestMessageDispatcher(endpoint);
    }
}
