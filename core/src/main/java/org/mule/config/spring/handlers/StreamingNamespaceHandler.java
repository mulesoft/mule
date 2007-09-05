/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.StringAddressEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceDescriptorDefinitionParser;
import org.mule.impl.endpoint.InboundStreamingEndpoint;
import org.mule.impl.endpoint.OutboundStreamingEndpoint;
import org.mule.impl.endpoint.ResponseStreamingEndpoint;
import org.mule.impl.model.streaming.StreamingModel;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class StreamingNamespaceHandler  extends NamespaceHandlerSupport
{
    
    public void init()
    {
        registerBeanDefinitionParser("model", new OrphanDefinitionParser(StreamingModel.class, true));
        registerBeanDefinitionParser("service", new ServiceDescriptorDefinitionParser());
        registerBeanDefinitionParser("inbound-router", new ChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-router", new ChildDefinitionParser("outboundRouter", OutboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-pass-through-router", new RouterDefinitionParser("router", OutboundPassThroughRouter.class));
        registerBeanDefinitionParser("inbound-endpoint", new StringAddressEndpointDefinitionParser(InboundStreamingEndpoint.class));
        registerBeanDefinitionParser("outbound-endpoint", new StringAddressEndpointDefinitionParser(OutboundStreamingEndpoint.class));
        registerBeanDefinitionParser("response-endpoint", new StringAddressEndpointDefinitionParser(ResponseStreamingEndpoint.class));
    }

}
