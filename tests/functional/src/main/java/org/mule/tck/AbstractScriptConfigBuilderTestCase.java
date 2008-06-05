/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.AbstractExceptionListener;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.model.Model;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.NestedRouter;
import org.mule.api.routing.NestedRouterCollection;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.ResponseRouter;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestEntryPointResolverSet;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.transformer.TransformerUtils;

import java.util.List;
import java.util.Map;

public abstract class AbstractScriptConfigBuilderTestCase extends FunctionalTestCase
{

    // use legacy entry point resolver?
    private boolean legacy;

    protected AbstractScriptConfigBuilderTestCase()
    {
        this(false);
    }

    protected AbstractScriptConfigBuilderTestCase(boolean legacy)
    {
        this.legacy = legacy;
    }

    public void testManagerConfig() throws Exception
    {
        assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
        assertNotNull(muleContext.getTransactionManager());
    }


    public void testConnectorConfig() throws Exception
    {
        TestConnector c = (TestConnector) muleContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(c);
        assertNotNull(c.getExceptionListener());
        assertTrue(c.getExceptionListener() instanceof TestExceptionStrategy);
    }

    public void testGlobalEndpointConfig() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");
        
        MessagePropertyFilter filter = (MessagePropertyFilter)endpoint.getFilter();
        assertNotNull(filter);
        assertEquals("foo=bar", filter.getExpression());

        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("testEPWithCS");
        assertNotNull(ep);
    }

    public void testEndpointConfig() throws MuleException
    {
        // test that endpoints have been resolved on endpoints
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "waterMelonEndpoint");
        assertNotNull(endpoint);
        // aliases no longer possible
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        ImmutableEndpoint ep = service.getInboundRouter().getEndpoint("Orange");
        assertNotNull(ep);
        final List responseTransformers = ep.getResponseTransformers();
        assertNotNull(responseTransformers);
        assertFalse(responseTransformers.isEmpty());
        final Object responseTransformer = responseTransformers.get(0);
        assertTrue(responseTransformer instanceof TestCompressionTransformer);
    }

    public void testExceptionStrategy()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(muleContext.getRegistry().lookupModel("main").getExceptionListener());
        assertNotNull(service.getExceptionListener());

        assertTrue(((AbstractExceptionListener) service.getExceptionListener()).getEndpoints().size() > 0);
        ImmutableEndpoint ep = (ImmutableEndpoint) ((AbstractExceptionListener) service.getExceptionListener()).getEndpoints()
                .get(0);

        assertEquals("test://orange.exceptions", ep.getEndpointURI().toString());
    }

    public void testTransformerConfig()
    {
        Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), String.class);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    public void testModelConfig() throws Exception
    {
        Model model = muleContext.getRegistry().lookupModel("main");
        assertNotNull(model);
        assertEquals("main", model.getName());
        if (legacy)
        {
            assertTrue(model.getEntryPointResolverSet() instanceof LegacyEntryPointResolverSet);
        }
        else
        {
            assertTrue(model.getEntryPointResolverSet() instanceof TestEntryPointResolverSet);
        }
        assertTrue(model.getExceptionListener() instanceof TestExceptionStrategy);

        assertTrue(((AbstractExceptionListener) model.getExceptionListener()).getEndpoints().size() > 0);
        ImmutableEndpoint ep = (ImmutableEndpoint) ((AbstractExceptionListener) model.getExceptionListener()).getEndpoints()
                .get(0);

        assertEquals("test://component.exceptions", ep.getEndpointURI().toString());

        // assertTrue(model.isComponentRegistered("orangeComponent"));
    }

    /*
     * Since MULE-1933, Service no longer has properties and most properties are set on endpoint.
     * So lets continue to test properties, but on endpoints instead.
     */
    public void testEndpointPropertiesConfig() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "endpointWithProps");

        Map props = endpoint.getProperties();
        assertNotNull(props);
        assertEquals("9", props.get("segments"));
        assertEquals("4.21", props.get("radius"));
        assertEquals("Juicy Baby!", props.get("brand"));

        assertNotNull(props.get("listProperties"));
        List list = (List) props.get("listProperties");
        assertEquals(3, list.size());
        assertEquals("prop1", list.get(0));
        assertEquals("prop2", list.get(1));
        assertEquals("prop3", list.get(2));

        assertNotNull(props.get("arrayProperties"));
        list = (List) props.get("arrayProperties");
        assertEquals(3, list.size());
        assertEquals("prop4", list.get(0));
        assertEquals("prop5", list.get(1));
        assertEquals("prop6", list.get(2));

        assertNotNull(props.get("mapProperties"));
        props = (Map) props.get("mapProperties");
        assertEquals("prop1", props.get("prop1"));
        assertEquals("prop2", props.get("prop2"));

        assertEquals(6, endpoint.getProperties().size());
    }

    public void testOutboundRouterConfig()
    {
        // test outbound message router
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getOutboundRouter());
        OutboundRouterCollection router = service.getOutboundRouter();
        assertNull(router.getCatchAllStrategy());
        assertEquals(1, router.getRouters().size());
        // check first Router
        OutboundRouter route1 = (OutboundRouter) router.getRouters().get(0);
        assertTrue(route1 instanceof OutboundPassThroughRouter);
        assertEquals(1, route1.getEndpoints().size());
    }

    public void testNestedRouterConfig()
    {
        // test outbound message router
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getComponent());
        assertTrue(service.getComponent() instanceof JavaComponent);
        NestedRouterCollection router = ((JavaComponent) service.getComponent()).getNestedRouter();
        assertNotNull(router);

        assertEquals(2, router.getRouters().size());
        // check first Router
        NestedRouter route1 = (NestedRouter) router.getRouters().get(0);
        assertEquals(FruitCleaner.class, route1.getInterface());
        assertEquals("wash", route1.getMethod());
        assertNotNull(route1.getEndpoint());
        // check second Router
        NestedRouter route2 = (NestedRouter) router.getRouters().get(1);
        assertEquals(FruitCleaner.class, route2.getInterface());
        assertEquals("polish", route2.getMethod());
        assertNotNull(route1.getEndpoint());
    }

    public void testDescriptorEndpoints()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertEquals(1, service.getOutboundRouter().getRouters().size());
        OutboundRouter router = (OutboundRouter)service.getOutboundRouter().getRouters().get(0);
        assertEquals(1, router.getEndpoints().size());
        ImmutableEndpoint endpoint = (ImmutableEndpoint) router.getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals("appleInEndpoint", endpoint.getName());
        assertNotNull(endpoint.getTransformers());
        assertTrue(TransformerUtils.firstOrNull(endpoint.getTransformers()) instanceof TestCompressionTransformer);

        // check the global endpoint
        try
        {
            endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("appleInEndpoint");
        }
        catch (MuleException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertNotNull(endpoint);
        assertEquals(1, endpoint.getTransformers().size());
        assertTrue(endpoint.getTransformers().get(0) instanceof TestInboundTransformer);

        assertEquals(2, service.getInboundRouter().getEndpoints().size());
        assertNotNull(service.getInboundRouter().getCatchAllStrategy());
        assertTrue(service.getInboundRouter().getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertNotNull(service.getInboundRouter().getCatchAllStrategy().getEndpoint());
        assertEquals("test://catch.all", service.getInboundRouter()
            .getCatchAllStrategy()
            .getEndpoint()
            .getEndpointURI()
            .toString());
        endpoint = service.getInboundRouter().getEndpoint("orangeEndpoint");
        assertNotNull(endpoint);
        assertEquals("orangeEndpoint", endpoint.getName());
        assertEquals("orangeQ", endpoint.getEndpointURI().getAddress());
        assertNotNull(endpoint.getTransformers());
        assertTrue(TransformerUtils.firstOrNull(endpoint.getTransformers()) instanceof TestCompressionTransformer);

        // check the global endpoint
        try
        {
            endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("orangeEndpoint");
        }
        catch (MuleException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertNotNull(endpoint);
        assertFalse(endpoint.getTransformers().isEmpty());
        assertTrue(endpoint.getTransformers().get(0) instanceof TestInboundTransformer);
    }

    public void testInboundRouterConfig()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getInboundRouter());
        InboundRouterCollection messageRouter = service.getInboundRouter();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(0, messageRouter.getRouters().size());
        assertTrue(messageRouter.getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertEquals(2, messageRouter.getEndpoints().size());
    }

    public void testResponseRouterConfig()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getResponseRouter());
        ResponseRouterCollection messageRouter = service.getResponseRouter();
        assertNull(messageRouter.getCatchAllStrategy());
        assertEquals(10001, messageRouter.getTimeout());
        assertEquals(1, messageRouter.getRouters().size());
        ResponseRouter router = (ResponseRouter) messageRouter.getRouters().get(0);
        assertTrue(router instanceof TestResponseAggregator);
        assertNotNull(messageRouter.getEndpoints());
        assertEquals(2, messageRouter.getEndpoints().size());
        ImmutableEndpoint ep = (ImmutableEndpoint) messageRouter.getEndpoints().get(0);
        assertEquals("response1", ep.getEndpointURI().getAddress());
        assertTrue(ep instanceof InboundEndpoint);
        ep = (ImmutableEndpoint) messageRouter.getEndpoints().get(1);
        assertEquals("AppleResponseQueue", ep.getEndpointURI().getAddress());
        assertTrue(ep instanceof InboundEndpoint);
    }
}
