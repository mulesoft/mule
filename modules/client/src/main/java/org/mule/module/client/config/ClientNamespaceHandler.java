/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
