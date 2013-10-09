/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ws.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.module.ws.config.spring.parsers.specific.WSProxyDefinitionParser;

/**
 * Registers a Bean Definition Parser for handling <code><ws:*></code> elements.
 */
public class WSNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        // Flow Constructs
        registerBeanDefinitionParser("proxy", new WSProxyDefinitionParser());
    }
}
