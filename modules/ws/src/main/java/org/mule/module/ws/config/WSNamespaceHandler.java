/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
