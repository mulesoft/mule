/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jca;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.tck.AbstractMuleTestCase;

import java.lang.reflect.Method;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

public class JcaServiceTestCase extends AbstractMuleTestCase // AbstractServiceTestCase
{

    // Cannot extend AbstractServiceTestCase because of inconsistent behaviour. See
    // MULE-2843

    private Service service;

    private TestJCAWorkManager workManager;

    protected void doSetUp() throws Exception
    {
        // Create and register JcaModel
        workManager = new TestJCAWorkManager();
        JcaModel jcaModel = new JcaModel();
        muleContext.getRegistry().registerModel(jcaModel);

        // Create, register, initialise and start JcaService
        String name = "JcaService#";
        service = new JcaService();
        service.setName(name);
        service.setModel(jcaModel);
        service.setComponent(new JcaComponent(new TestMessageEndpointFactory(), new DefaultEntryPointResolverSet(),
            service, workManager));
        muleContext.getRegistry().registerService(service);

        assertNotNull(service);
    }

    protected void doTearDown() throws Exception
    {
        workManager = null;
        service = null;
    }

    public void testSendEvent() throws Exception
    {
        service.start();
        ImmutableEndpoint endpoint = getTestInboundEndpoint("jcaInFlowEndpoint");
        MuleEvent event = getTestEvent("Message", endpoint);

        try
        {
            service.sendEvent(event);
            fail("Exception expected, JcaService does not support sendEvent()");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testDispatchEvent() throws Exception
    {
        service.start();
        ImmutableEndpoint endpoint = getTestInboundEndpoint("jcaInFlowEndpoint");
        MuleEvent event = getTestEvent("Message", endpoint);

        service.dispatchEvent(event);
        assertEquals(1, workManager.getScheduledWorkList().size());
        assertEquals(0, workManager.getStartWorkList().size());
        assertEquals(0, workManager.getDoWorkList().size());
    }

    public void testPause()
    {
        try
        {
            service.pause();
            fail("Exception expected, JcaService does not support pause()");
        }
        catch (MuleException e)
        {
            // expected
        }
    }

    public void testResume()
    {
        try
        {
            service.resume();
            fail("Exception expected, JcaService does not support resume()");
        }
        catch (MuleException e)
        {
            // expected
        }
    }
    
    class TestMessageEndpointFactory implements MessageEndpointFactory
    {

        public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException
        {
            return null;
        }

        public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException
        {
            return false;
        }

    }

    class TestMessageEndoint implements MessageEndpoint
    {

        public void afterDelivery() throws ResourceException
        {
            // TODO Auto-generated method stub

        }

        public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException
        {
            // TODO Auto-generated method stub

        }

        public void release()
        {
            // TODO Auto-generated method stub

        }

    }

}
