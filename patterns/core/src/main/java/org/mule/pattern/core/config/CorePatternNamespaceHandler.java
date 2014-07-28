/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.pattern.core.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.handlers.MuleNamespaceHandler;
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
        logger.warn(MuleNamespaceHandler.PATTERNS_DEPRECATION_MESSAGE);

        // Flow Constructs
        registerBeanDefinitionParser("web-service-proxy", new WSProxyDefinitionParser());
        registerBeanDefinitionParser("simple-service", new SimpleServiceDefinitionParser());
        registerBeanDefinitionParser("bridge", new BridgeDefinitionParser());
        registerBeanDefinitionParser("validator", new ValidatorDefinitionParser());
        registerBeanDefinitionParser("http-proxy", new HttpProxyDefinitionParser());
    }
}
