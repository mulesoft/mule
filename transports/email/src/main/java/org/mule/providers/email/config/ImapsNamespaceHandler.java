/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.providers.email.ImapsConnector;
import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.impl.endpoint.OutboundEndpoint;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class ImapsNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleChildDefinitionParser(ImapsConnector.class, true));
        registerBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser("imaps"));
        registerBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser("imaps", InboundEndpoint.class));
        registerBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser("imaps", OutboundEndpoint.class));
        registerBeanDefinitionParser("tls-trust-store", new ParentDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ParentDefinitionParser());
    }
    
}