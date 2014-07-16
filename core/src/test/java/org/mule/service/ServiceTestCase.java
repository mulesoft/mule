/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.model.Model;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transport.Connector;
import org.mule.config.QueueProfile;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServiceTestCase extends AbstractMuleContextTestCase
{
    private Connector testConnector;
    private Service service;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        testConnector = new TestConnector(muleContext);
        testConnector.setName("customTestConnector");
        muleContext.getRegistry().registerConnector(testConnector);

        InboundEndpoint inboundEndpoint1 = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://test1?connector=customTestConnector");
        InboundEndpoint inboundEndpoint2 = muleContext.getEndpointFactory().getInboundEndpoint(
            "test://test2?connector=customTestConnector");

        service = new SedaService(muleContext);
        service.setName("testService");
        ((CompositeMessageSource) service.getMessageSource()).addSource(inboundEndpoint1);
        ((CompositeMessageSource) service.getMessageSource()).addSource(inboundEndpoint2);
        Model model = new SedaModel();
        model.setMuleContext(muleContext);
        model.initialise();
        service.setModel(model);
        
        QueueProfile queueProfile = QueueProfile.newInstancePersistingToDefaultMemoryQueueStore(muleContext);
        ((SedaService) service).setQueueProfile(queueProfile);

        muleContext.getRegistry().registerService(service);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        muleContext.getRegistry().unregisterObject(service.getName(), MuleRegistry.LIFECYCLE_BYPASS_FLAG);
    }

    @Test
    public void testUnregisterListenersOnServiceDisposal() throws Exception
    {
        // Start muleContext, this starts connectors and services
        muleContext.start();

        // Assert that connector has two receivers registered, one for each endpoint
        assertEquals(2, ((AbstractConnector) testConnector).getReceivers().size());

        service.stop();
        service.dispose();

        // Assert that connector has no receivers registered after service disposal
        assertEquals(0, ((AbstractConnector) testConnector).getReceivers().size());
    }

    @Test
    public void testUnregisterListenersOnServiceStop() throws Exception
    {

        // Start muleContext, this starts connectors and services
        muleContext.start();

        // Assert that connector has two receivers registered, one for each endpoint
        assertEquals(2, ((AbstractConnector) testConnector).getReceivers().size());

        service.stop();

        // Assert that connector has no receivers registered after service disposal
        assertEquals(0, ((AbstractConnector) testConnector).getReceivers().size());
    }
}
