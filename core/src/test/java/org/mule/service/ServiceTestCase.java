/*
 * $$Id$$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.config.QueueProfile;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transport.AbstractConnector;

public class ServiceTestCase extends AbstractMuleTestCase
{

    private Connector testConnector;
    private Service service;

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        testConnector = new TestConnector();
        testConnector.setName("customTestConnector");
        muleContext.getRegistry().registerConnector(testConnector);

        InboundEndpoint inboundEndpoint1 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://test1?connector=customTestConnector");
        InboundEndpoint inboundEndpoint2 = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "test://test2?connector=customTestConnector");

        service = new SedaService();
        service.setName("testService");
        service.getInboundRouter().addEndpoint(inboundEndpoint1);
        service.getInboundRouter().addEndpoint(inboundEndpoint2);
        service.setModel(new SedaModel());
        ((SedaService) service).setQueueProfile(new QueueProfile());
        muleContext.getRegistry().registerService(service);

    }

    public void testUnregisterListenersOnServiceDisposal() throws Exception
    {
        // Start muleContext, this starts connectors and services
        muleContext.start();

        // Assert that connector has two receivers registered, one for each endpoint
        assertEquals(2, ((AbstractConnector) testConnector).getReceivers().size());

        service.dispose();

        // Assert that connector has no receivers registered after service disposal
        assertEquals(0, ((AbstractConnector) testConnector).getReceivers().size());

    }

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
