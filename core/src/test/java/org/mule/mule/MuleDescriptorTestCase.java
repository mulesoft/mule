/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.component.simple.PassThroughComponent;
import org.mule.model.seda.SedaService;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.util.object.SingletonObjectFactory;

public class MuleDescriptorTestCase extends AbstractMuleTestCase
{
    public void testDescriptorDefaults() throws Exception
    {
        Service service = new SedaService();

        //TODO RM*
//        MuleConfiguration config = new MuleConfiguration();
//        assertEquals(config.getQueueProfile().getMaxOutstandingMessages(), 
//                     descriptor.getQueueProfile().getMaxOutstandingMessages());
//        assertEquals(config.getThreadingProfile().getMaxBufferSize(), 
//                     descriptor.getThreadingProfile().getMaxBufferSize());
//        assertEquals(config.getPoolingProfile().getMaxIdle(), descriptor.getPoolingProfile().getMaxIdle());
//        assertEquals(config.getPoolingProfile().getMaxWait(), descriptor.getPoolingProfile().getMaxWait());
//        assertEquals(config.getPoolingProfile().getMaxActive(), descriptor.getPoolingProfile().getMaxActive());
//        assertEquals("1.0", descriptor.getVersion());
        // assertEquals(2, descriptor.getInitialisationPolicy());

        //assertNull("Factory should be null but is " + service.getServiceFactory(), service.getServiceFactory());
        assertNotNull(service.getComponentFactory());
        assertEquals(SingletonObjectFactory.class, service.getComponentFactory().getClass());
        service.getComponentFactory().initialise();
        assertTrue(service.getComponentFactory().getInstance() instanceof PassThroughComponent);
        assertNull(service.getName());
        //assertEquals(0, service.getProperties().size());
    }

    // These validations seems a bit silly, IMHO.
//    public void testDescriptorNullValidation() throws Exception
//    {
//        Service service = new SedaService();
//        try
//        {
//            service.setExceptionListener(null);
//            fail("setting exeption strategy to null should fail");
//        }
//        catch (RuntimeException e)
//        {
//            // expected
//        }
//
//        try
//        {
//            service.setName(null);
//            fail("setting name to null should fail");
//        }
//        catch (RuntimeException e)
//        {
//            // expected
//        }
//
//        try
//        {
//            service.setServiceFactory(null);
//            fail("setting serviceFactory to null should fail");
//        }
//        catch (RuntimeException e)
//        {
//            // expected
//        }
//
//    }

    public void testEndpointValidation() throws Exception
    {
        Service service = getTestService("Terry", Orange.class);
        TestExceptionStrategy es = new TestExceptionStrategy();
        service.setExceptionListener(es);
        assertEquals(1, service.getOutboundRouter().getRouters().size());
        
        // TODO Why should there be an outbound endpoint configured?
        //Endpoint ep = (Endpoint)((OutboundRouter)service.getOutboundRouter().getRouters().get(0)).getEndpoints().get(0);
        //assertNotNull(ep);
        //assertNotNull(ep.getConnector().getExceptionListener());

        // create receive endpoint
        InboundEndpoint endpoint = getTestInboundEndpoint("test2");
        service.getInboundRouter().addEndpoint(endpoint);
        // Add receive endpoint, this shoulbe set as default
        assertNotNull(endpoint.getConnector().getExceptionListener());
    }
}
