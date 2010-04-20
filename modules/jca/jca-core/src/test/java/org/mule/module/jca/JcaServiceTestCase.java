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
import org.mule.model.AbstractServiceTestCase;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;

import java.lang.reflect.Method;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

public class JcaServiceTestCase extends AbstractServiceTestCase
{
    private Service service;

    protected void doSetUp() throws Exception
    {
        // Create and initialise JcaModel
        workManager = new TestJCAWorkManager();
        JcaModel jcaModel = new JcaModel();
        jcaModel.setMuleContext(muleContext);
        jcaModel.initialise();

        String name = "JcaService#";
        service = new JcaService(muleContext);
        service.setName(name);
        service.setModel(jcaModel);
        service.setComponent(new JcaComponent(new TestMessageEndpointFactory(), new DefaultEntryPointResolverSet(),
                service, workManager));
    }

    @Override
    protected Service getService()
    {
        return service;
    }

    private TestJCAWorkManager workManager;

    protected void doTearDown() throws Exception
    {
        workManager = null;
        service = null;
    }

    public void testSendEvent() throws Exception
    {
        getService().initialise();
        getService().start();
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
        getService().initialise();
        getService().start();
        ImmutableEndpoint endpoint = getTestInboundEndpoint("jcaInFlowEndpoint");
        MuleEvent event = getTestEvent("Message", endpoint);

        getService().dispatchEvent(event);
        assertEquals(1, workManager.getScheduledWorkList().size());
        assertEquals(0, workManager.getStartWorkList().size());
        assertEquals(0, workManager.getDoWorkList().size());
    }

    public void testPause() throws MuleException
    {
        try
        {
            getService().pause();
            fail("Exception expected, JcaService does not support pause()");
        }
        catch (IllegalStateException e)
        {
            // expected
        }

    }

    public void testResume() throws MuleException
    {
        try
        {
            service.resume();
            fail("Exception expected, JcaService does not support resume()");
        }
        catch (IllegalStateException e)
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
}
