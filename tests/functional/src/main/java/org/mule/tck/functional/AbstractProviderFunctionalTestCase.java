/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.RegistryContext;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.model.seda.SedaModel;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;

import java.util.HashMap;
/**
 * @deprecated use Xml configuration instead
 */
public abstract class AbstractProviderFunctionalTestCase extends AbstractMuleTestCase
{
    protected static final int NUM_MESSAGES_TO_SEND = 100;

    protected UMOConnector connector;
    protected boolean callbackCalled = false;
    protected int callbackCount = 0;
    protected boolean transacted = false;

    private final Object callbackLock = new Object();

    protected MuleDescriptor descriptor;

    protected void doSetUp() throws Exception
    {
        // Make sure we are running synchronously
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);
//       TODO RM* RegistryContext.getConfiguration().getPoolingProfile().setInitialisationPolicy(
//            PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        UMOModel model = new SedaModel();
        model.setName("main");
       managementContext.getRegistry().registerModel(model);
        callbackCalled = false;
        callbackCount = 0;
        connector = createConnector();
        // Start the server
       managementContext.start();
    }

    protected void doTearDown() throws Exception
    {
        if (connector != null)
        {
            connector.dispose();
        }
    }

    public void testSend() throws Exception
    {
        descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        initialiseComponent(descriptor, this.createEventCallback());

        sendTestData(NUM_MESSAGES_TO_SEND);

        afterInitialise();

        receiveAndTestResults();

        assertTrue(callbackCalled);
    }

    public UMOComponent initialiseComponent(UMODescriptor descriptor, EventCallback callback)
        throws Exception
    {
        OutboundPassThroughRouter router = new OutboundPassThroughRouter();
        router.addEndpoint(createOutboundEndpoint());
        descriptor.getOutboundRouter().addRouter(router);

        descriptor.getInboundRouter().addEndpoint(createInboundEndpoint());
        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        descriptor.setProperties(props);
        managementContext.getRegistry().registerConnector(connector);
        descriptor.setModelName("main");
        managementContext.getRegistry().registerService(descriptor);
        UMOModel model = managementContext.getRegistry().lookupModel("main");

        return model.getComponent(descriptor.getName());
    }

    /**
     * Implementing tests can overide this to add further configuration to the
     * outbound endpoint
     * 
     * @return
     * @throws EndpointException 
     * @throws InitialisationException 
     */
    protected UMOImmutableEndpoint createOutboundEndpoint() throws InitialisationException, EndpointException
    {
        if (getOutDest() != null)
        {
            UMOEndpointBuilder builder=new EndpointURIEndpointBuilder(getOutDest(), managementContext);
            builder.setName("testOut");
            builder.setConnector(connector);
            builder.setSynchronous(true);
            return builder.buildOutboundEndpoint();
        }
        else
        {
            return null;
        }
    }

    /**
     * Implementing tests can overide this to add further configuration to the
     * inbound endpoint
     * 
     * @return
     * @throws EndpointException 
     * @throws InitialisationException 
     */
    protected UMOImmutableEndpoint createInboundEndpoint() throws InitialisationException, EndpointException
    {
        UMOEndpointBuilder builder=new EndpointURIEndpointBuilder(getInDest(), managementContext);
        builder.setName("testIn");
        builder.setConnector(connector);
        builder.setSynchronous(true);
        return builder.buildInboundEndpoint();
    }

    public void afterInitialise() throws Exception
    {
        // nothing to do
    }

    public EventCallback createEventCallback()
    {
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object component)
            {
                synchronized (callbackLock)
                {
                    callbackCalled = true;
                    callbackCount++;
                }
                if (!transacted)
                {
                    assertNull(context.getCurrentTransaction());
                }
                else
                {
                    assertNotNull(context.getCurrentTransaction());
                }
            }
        };
        return callback;
    }

    protected abstract void sendTestData(int iterations) throws Exception;

    protected abstract void receiveAndTestResults() throws Exception;

    protected abstract String getInDest();

    protected abstract String getOutDest();

    protected abstract UMOConnector createConnector() throws Exception;

}
