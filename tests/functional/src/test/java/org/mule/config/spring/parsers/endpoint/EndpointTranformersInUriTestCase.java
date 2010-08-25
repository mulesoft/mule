/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseTransformer;

import java.util.List;

public class EndpointTranformersInUriTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/endpoint-uri-transformers.xml";
    }

    public void testGlobalEndpoint1() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep1");
        
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List <Transformer> transformers = endpoint.getTransformers();
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

    public void testGlobalEndpoint2() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep2");
        
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List <Transformer> transformers = endpoint.getTransformers();
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
    
    public void testGlobalEndpoints() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService("globalEndpoints");
        
        ImmutableEndpoint endpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List <Transformer> transformers = endpoint.getTransformers();
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

        endpoint = (ImmutableEndpoint) ((OutboundPassThroughRouter) ((OutboundRouterCollection) service.getOutboundMessageProcessor()).getRoutes()
            .get(0)).getRoutes().get(0);
        
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
    
   public void testLocalEndpoints() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService("localEndpoints");
       
        ImmutableEndpoint endpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List <Transformer> transformers = endpoint.getTransformers();
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

        endpoint = (ImmutableEndpoint) ((OutboundPassThroughRouter) ((OutboundRouterCollection) service.getOutboundMessageProcessor()).getRoutes()
            .get(0)).getRoutes().get(0);
        
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
}
