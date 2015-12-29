/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transformer.Transformer;
import org.mule.construct.Flow;
import org.mule.exception.AbstractExceptionListener;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.util.List;
import java.util.Map;

import org.junit.Test;

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

    @Test
    public void testManagerConfig() throws Exception
    {
        assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
        assertNotNull(muleContext.getTransactionManager());
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

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
        ImmutableEndpoint ep = (ImmutableEndpoint) ((CompositeMessageSource) flow.getMessageSource()).getSources().get(0);
        assertNotNull(ep);
        final List responseTransformers = ep.getResponseMessageProcessors();
        assertNotNull(responseTransformers);
        assertFalse(responseTransformers.isEmpty());
        final Object responseTransformer = responseTransformers.get(0);
        assertTrue(responseTransformer instanceof InterceptingChainLifecycleWrapper);
        assertTrue(((InterceptingChainLifecycleWrapper) responseTransformer).getMessageProcessors().get(0) instanceof TestCompressionTransformer);
    }

    @Test
    public void testExceptionStrategy()
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
        assertNotNull(flow.getExceptionListener());

        assertTrue(((AbstractExceptionListener) flow.getExceptionListener()).getMessageProcessors().size() > 0);
        OutboundEndpoint ep = (OutboundEndpoint) ((AbstractExceptionListener) flow.getExceptionListener()).getMessageProcessors().get(0);

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
    public void testBindingConfig()
    {
        // test outbound message router
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
        assertNotNull(flow.getMessageProcessors().get(0));
        assertTrue((flow.getMessageProcessors().get(0) instanceof JavaComponent));
        List<InterfaceBinding> bindings= ((JavaComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings();
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

}
