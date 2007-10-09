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

import org.mule.config.spring.factories.InboundStreamingEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundStreamingEndpointFactoryBean;
import org.mule.config.spring.factories.ResponseStreamingEndpointFactoryBean;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.GenericEndpointDefinitionParser;
import org.mule.impl.model.streaming.StreamingComponent;
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

        registerBeanDefinitionParser("service", new ComponentDefinitionParser(StreamingComponent.class));

        registerBeanDefinitionParser("inbound-router", new ChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-router", new ChildDefinitionParser("outboundRouter", OutboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-pass-through-router", new RouterDefinitionParser("router", OutboundPassThroughRouter.class));
        registerBeanDefinitionParser("inbound-endpoint", new GenericEndpointDefinitionParser(InboundStreamingEndpointFactoryBean.class));
        registerBeanDefinitionParser("outbound-endpoint", new GenericEndpointDefinitionParser(OutboundStreamingEndpointFactoryBean.class));
        registerBeanDefinitionParser("response-endpoint", new GenericEndpointDefinitionParser(ResponseStreamingEndpointFactoryBean.class));
    }

}
