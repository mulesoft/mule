/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.pattern.core.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.BridgeDefinitionParser;
import org.mule.config.spring.parsers.specific.SimpleServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.ValidatorDefinitionParser;
import org.mule.module.ws.config.spring.parsers.specific.WSProxyDefinitionParser;
import org.mule.transport.http.config.spring.parsers.specific.HttpProxyDefinitionParser;

/**
 * Registers a Bean Definition Parser for handling <code><pattern:*></code> elements.
 */
public class CorePatternNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        // Flow Constructs
        registerBeanDefinitionParser("web-service-proxy", new WSProxyDefinitionParser());
        registerBeanDefinitionParser("simple-service", new SimpleServiceDefinitionParser());
        registerBeanDefinitionParser("bridge", new BridgeDefinitionParser());
        registerBeanDefinitionParser("validator", new ValidatorDefinitionParser());
        registerBeanDefinitionParser("http-proxy", new HttpProxyDefinitionParser());
    }
}
