/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.module.xml.transformer.wire.XStreamWireFormat;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * TODO
 */
public class ClientNamespaceHandler extends NamespaceHandlerSupport
{
    /**
     * Invoked by the {@link org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader} after
     * construction but before any custom elements are parsed.
     *
     * @see org.springframework.beans.factory.xml.NamespaceHandlerSupport#registerBeanDefinitionParser(String, org.springframework.beans.factory.xml.BeanDefinitionParser)
     */
    public void init()
    {
        registerBeanDefinitionParser("remote-dispatcher-agent", new RemoteDispatcherAgentDefinitionParser());
        registerBeanDefinitionParser("remote-endpoint", new ChildEndpointDefinitionParser(InboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("xml-wire-format", new ChildDefinitionParser("wireFormat", XStreamWireFormat.class));
        registerBeanDefinitionParser("serialization-wire-format", new ChildDefinitionParser("wireFormat", SerializedMuleMessageWireFormat.class));
        registerBeanDefinitionParser("custom-wire-format", new ChildDefinitionParser("wireFormat"));
    }
}
