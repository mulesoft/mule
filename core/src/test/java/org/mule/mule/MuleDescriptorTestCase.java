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

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOOutboundRouter;

public class MuleDescriptorTestCase extends AbstractMuleTestCase
{
    public void testDescriptorDefaults() throws Exception
    {
        UMOComponent component = new SedaComponent();
        component.initialise();

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

        assertNull("Factory should be null but is " + component.getServiceFactory(), component.getServiceFactory());
        assertNull(component.getName());
        //assertEquals(0, component.getProperties().size());
    }

    public void testDescriptorNullValidation() throws Exception
    {
        UMOComponent component = new SedaComponent();
        try
        {
            component.setExceptionListener(null);
            fail("setting exeption strategy to null should fail");
        }
        catch (RuntimeException e)
        {
            // expected
        }

        try
        {
            component.setName(null);
            fail("setting name to null should fail");
        }
        catch (RuntimeException e)
        {
            // expected
        }

        try
        {
            component.setServiceFactory(null);
            fail("setting serviceFactory to null should fail");
        }
        catch (RuntimeException e)
        {
            // expected
        }

    }

    public void testImplementationValidation() throws Exception
    {
        UMOComponent component = new SedaComponent();
        try
        {
            component.setServiceFactory(null);
            fail("setting serviceFactory to null should fail");
        }
        catch (RuntimeException e)
        {
            // expected
        }

    }

    public void testEndpointValidation() throws Exception
    {
        UMOComponent component = getTestComponent("Terry", Orange.class);
        TestExceptionStrategy es = new TestExceptionStrategy();
        component.setExceptionListener(es);
        assertEquals(1, component.getOutboundRouter().getRouters().size());
        UMOEndpoint ep = (UMOEndpoint)((UMOOutboundRouter)component.getOutboundRouter().getRouters().get(0)).getEndpoints().get(0);
        assertNotNull(ep);
        assertNotNull(ep.getConnector().getExceptionListener());

        // create receive endpoint
        UMOEndpoint endpoint = getTestEndpoint("test2", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        component.getInboundRouter().addEndpoint(endpoint);
        // Add receive endpoint, this shoulbe set as default
        assertNotNull(endpoint.getConnector().getExceptionListener());
    }
}
