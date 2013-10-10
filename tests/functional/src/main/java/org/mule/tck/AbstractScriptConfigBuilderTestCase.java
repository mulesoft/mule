/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.MuleException;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.model.Model;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.exception.AbstractExceptionListener;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestEntryPointResolverSet;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractScriptConfigBuilderTestCase extends org.mule.tck.junit4.FunctionalTestCase
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

    @Test
    public void testManagerConfig() throws Exception
    {
        assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
        assertNotNull(muleContext.getTransactionManager());
    }


    @Test
    public void testConnectorConfig() throws Exception
    {
        MessagingExceptionHandler es = muleContext.getRegistry().lookupModel("main").getExceptionListener();
        assertNotNull(es);
        assertTrue(es.getClass().getName(), es instanceof TestExceptionStrategy);
    }

    @Test
    public void testGlobalEndpointConfig() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            "fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");
        
        MessagePropertyFilter filter = (MessagePropertyFilter)endpoint.getFilter();
        assertNotNull(filter);
        assertEquals("foo=bar", filter.getPattern());

        ImmutableEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint("testEPWithCS");
        assertNotNull(ep);
    }

    @Test
    public void testEndpointConfig() throws MuleException
    {
        // test that targets have been resolved on targets
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            "waterMelonEndpoint");
        assertNotNull(endpoint);
        // aliases no longer possible
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        ImmutableEndpoint ep = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoint("Orange");
        assertNotNull(ep);
        final List responseTransformers = ep.getResponseTransformers();
        assertNotNull(responseTransformers);
        assertFalse(responseTransformers.isEmpty());
        final Object responseTransformer = responseTransformers.get(0);
        assertTrue(responseTransformer instanceof TestCompressionTransformer);
    }

    @Test
    public void testExceptionStrategy()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(muleContext.getRegistry().lookupModel("main").getExceptionListener());
        assertNotNull(service.getExceptionListener());

        assertTrue(((AbstractExceptionListener) service.getExceptionListener()).getMessageProcessors().size() > 0);
        OutboundEndpoint ep = (OutboundEndpoint) ((AbstractExceptionListener) service.getExceptionListener()).getMessageProcessors().get(0);

        assertEquals("test://orange.exceptions", ep.getEndpointURI().toString());
    }

    @Test
    public void testTransformerConfig()
    {
        Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnDataType(), DataTypeFactory.STRING);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    @Test
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

        assertTrue(((AbstractExceptionListener) model.getExceptionListener()).getMessageProcessors().size() > 0);
        OutboundEndpoint ep = (OutboundEndpoint) ((AbstractExceptionListener) model.getExceptionListener()).getMessageProcessors().get(0);

        assertEquals("test://component.exceptions", ep.getEndpointURI().toString());

        // assertTrue(model.isComponentRegistered("orangeComponent"));
    }

    /*
     * Since MULE-1933, Service no longer has properties and most properties are set on endpoint.
     * So lets continue to test properties, but on targets instead.
     */
    @Test
    public void testEndpointPropertiesConfig() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
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

    @Test
    public void testOutboundRouterConfig()
    {
        // test outbound message router
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getOutboundMessageProcessor());
        OutboundRouterCollection router = (OutboundRouterCollection) service.getOutboundMessageProcessor();
        assertNull(router.getCatchAllStrategy());
        assertEquals(1, router.getRoutes().size());
        // check first Router
        OutboundRouter route1 = (OutboundRouter) router.getRoutes().get(0);
        assertTrue(route1 instanceof OutboundPassThroughRouter);
        assertEquals(1, route1.getRoutes().size());
    }

    @Test
    public void testBindingConfig()
    {
        // test outbound message router
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getComponent());
        assertTrue(service.getComponent() instanceof JavaComponent);
        List<InterfaceBinding> bindings= ((JavaComponent) service.getComponent()).getInterfaceBindings();
        assertNotNull(bindings);

        assertEquals(2, bindings.size());
        // check first Router
        InterfaceBinding route1 = bindings.get(0);
        assertEquals(FruitCleaner.class, route1.getInterface());
        assertEquals("wash", route1.getMethod());
        assertNotNull(route1.getEndpoint());
        // check second Router
        InterfaceBinding route2 = bindings.get(1);
        assertEquals(FruitCleaner.class, route2.getInterface());
        assertEquals("polish", route2.getMethod());
        assertNotNull(route1.getEndpoint());
    }

    @Test
    public void testDescriptorEndpoints()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertEquals(1, ((OutboundRouterCollection) service.getOutboundMessageProcessor()).getRoutes().size());
        OutboundRouter router = (OutboundRouter) ((OutboundRouterCollection)service.getOutboundMessageProcessor()).getRoutes().get(0);
        assertEquals(1, router.getRoutes().size());
        MessageProcessor mp = router.getRoutes().get(0);
        assertNotNull(mp);
        assertTrue(mp instanceof ImmutableEndpoint);
        ImmutableEndpoint endpoint = (ImmutableEndpoint) mp;
        assertNotNull(endpoint);
        assertEquals("appleInEndpoint", endpoint.getName());
        assertNotNull(endpoint.getTransformers());
        assertTrue(TransformerUtils.firstOrNull(endpoint.getTransformers()) instanceof TestCompressionTransformer);

        assertEquals(2, ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().size());
        assertNotNull(((ServiceCompositeMessageSource) service.getMessageSource()).getCatchAllStrategy());
        assertTrue(((ServiceCompositeMessageSource) service.getMessageSource()).getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        ForwardingCatchAllStrategy fcas = (ForwardingCatchAllStrategy)((ServiceCompositeMessageSource) service.getMessageSource()).getCatchAllStrategy();
        assertNotNull(fcas.getEndpoint());
        assertEquals("test://catch.all", fcas.getEndpoint().getEndpointURI().toString());
        endpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoint("orangeEndpoint");
        assertNotNull(endpoint);
        assertEquals("orangeEndpoint", endpoint.getName());
        assertEquals("orangeQ", endpoint.getEndpointURI().getAddress());
        assertNotNull(endpoint.getTransformers());
        assertTrue(TransformerUtils.firstOrNull(endpoint.getTransformers()) instanceof TestCompressionTransformer);
    }

    @Test
    public void testInboundRouterConfig()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getMessageSource());
        ServiceCompositeMessageSource messageRouter = (ServiceCompositeMessageSource) service.getMessageSource();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(0, messageRouter.getMessageProcessors().size());
        assertTrue(messageRouter.getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertEquals(2, messageRouter.getEndpoints().size());
    }

    @Test
    public void testResponseRouterConfig()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(service.getAsyncReplyMessageSource());
        
        ServiceAsyncReplyCompositeMessageSource messageRouter = service.getAsyncReplyMessageSource();
        assertNull(messageRouter.getCatchAllStrategy());
        assertEquals(10001, messageRouter.getTimeout().longValue());
        assertEquals(1, messageRouter.getMessageProcessors().size());
        
        MessageProcessor router = messageRouter.getMessageProcessors().get(0);
        assertTrue(router instanceof TestResponseAggregator);
        assertNotNull(messageRouter.getEndpoints());
        assertEquals(2, messageRouter.getEndpoints().size());
        
        InboundEndpoint ep = messageRouter.getEndpoints().get(0);
        assertEquals("response1", ep.getEndpointURI().getAddress());
        
        ep = messageRouter.getEndpoints().get(1);
        assertEquals("AppleResponseQueue", ep.getEndpointURI().getAddress());
    }
}
