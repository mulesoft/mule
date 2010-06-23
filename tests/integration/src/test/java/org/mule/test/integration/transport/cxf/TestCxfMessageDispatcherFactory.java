/*
 * $$Id$$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Defines a factory for {@link CxfMessageDispatcherWrapper} instances for
 * testing purposes. Maintains a list of the created dispatchers so they
 * are accessible from the tests.
 */
public class TestCxfMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{

    private List<CxfMessageDispatcherWrapper> createdDispatchers = new LinkedList<CxfMessageDispatcherWrapper>();

    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        CxfMessageDispatcherWrapper dispatcher = new CxfMessageDispatcherWrapper(endpoint);
        createdDispatchers.add(dispatcher);

        return dispatcher;
    }

    public List<CxfMessageDispatcherWrapper> getCreatedDispatchers()
    {
        return createdDispatchers;
    }
}
