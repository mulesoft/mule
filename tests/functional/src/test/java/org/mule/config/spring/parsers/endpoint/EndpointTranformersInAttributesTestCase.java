/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.construct.Flow;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseTransformer;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EndpointTranformersInAttributesTestCase extends AbstractServiceAndFlowTestCase
{    
    public EndpointTranformersInAttributesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/config/spring/parsers/endpoint/endpoint-attribute-transformers-service.xml"},
            {ConfigVariant.FLOW, "org/mule/config/spring/parsers/endpoint/endpoint-attribute-transformers-flow.xml"}
        });
    }      
    
    @Test
    public void testGlobalEndpoint1() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep1");

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<Transformer> transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
    }

    @Test
    public void testGlobalEndpoint2() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep2");

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<Transformer> transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);
        assertTrue(transformers.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseTransformers();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
        assertTrue(transformers.get(1) instanceof TestResponseTransformer);
    }

    @Test
    public void testGlobalEndpoints() throws MuleException
    {
        ImmutableEndpoint endpoint;        
        Object service = muleContext.getRegistry().lookupObject("globalEndpoints");
        
        if (variant.equals(ConfigVariant.FLOW))
        {            
            endpoint = (ImmutableEndpoint) ((Flow) service).getMessageSource();
        }
        else
        {         
            endpoint = ((ServiceCompositeMessageSource) ((Service) service).getMessageSource()).getEndpoints()
                            .get(0);
        }            

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<Transformer> transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);

        if (variant.equals(ConfigVariant.FLOW))
        {
            endpoint = (ImmutableEndpoint) ((Flow) service).getMessageProcessors().get(0); 
        }
        else
        {            
            endpoint = (ImmutableEndpoint) ((OutboundPassThroughRouter) ((OutboundRouterCollection) ((Service) service).getOutboundMessageProcessor()).getRoutes()
                .get(0)).getRoutes().get(0);
        }          
 
        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);
        assertTrue(transformers.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseTransformers();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
        assertTrue(transformers.get(1) instanceof TestResponseTransformer);
    }

    @Test
    public void testLocalEndpoints() throws MuleException
    {
        ImmutableEndpoint endpoint;        
        Object service = muleContext.getRegistry().lookupObject("localEndpoints");
        
        if (variant.equals(ConfigVariant.FLOW))
        {            
            endpoint = (ImmutableEndpoint) ((Flow) service).getMessageSource();
        }
        else
        {         
            endpoint = ((ServiceCompositeMessageSource) ((Service) service).getMessageSource()).getEndpoints()
                            .get(0);
        }                            

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<Transformer> transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);

        if (variant.equals(ConfigVariant.FLOW))
        {
            endpoint = (ImmutableEndpoint) ((Flow) service).getMessageProcessors().get(0); 
        }
        else
        {            
            endpoint = (ImmutableEndpoint) ((OutboundPassThroughRouter) ((OutboundRouterCollection) ((Service) service).getOutboundMessageProcessor()).getRoutes()
                .get(0)).getRoutes().get(0);
        }           
        
        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
    }

    @Test
    public void testTransformerRefsWithChildProcessors() throws MuleException
    {
        ImmutableEndpoint endpoint;
        
        Object service = muleContext.getRegistry().lookupObject("transformerRefsWithChildProcessors");
        
        if (variant.equals(ConfigVariant.FLOW))
        {            
            endpoint = (ImmutableEndpoint) ((Flow) service).getMessageSource();
        }
        else
        {         
            endpoint = ((ServiceCompositeMessageSource) ((Service) service).getMessageSource()).getEndpoints()
                            .get(0);
        }                            

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof StringAppendTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        assertTrue(((MessageProcessorChain)processors.get(0)).getMessageProcessors().get(0) instanceof StringAppendTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);

        if (variant.equals(ConfigVariant.FLOW))
        {
            endpoint = (ImmutableEndpoint) ((Flow) service).getMessageProcessors().get(0); 
        }
        else
        {            
            endpoint = (ImmutableEndpoint) ((OutboundPassThroughRouter) ((OutboundRouterCollection) ((Service) service).getOutboundMessageProcessor()).getRoutes()
                .get(0)).getRoutes().get(0);
        }                          

        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof MessagePropertiesTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        assertTrue(((MessageProcessorChain)processors.get(0)).getMessageProcessors().get(0) instanceof MessagePropertiesTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);
    }

}
