/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.ssl.config;

import org.mule.config.spring.parsers.generic.CompoundElementDefinitionParser;
import org.mule.config.spring.parsers.generic.SingleElementDefinitionParser;
import org.mule.providers.ssl.SslConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><ssl:connector></code> elements.
 */
public class SslNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new SingleElementDefinitionParser(SslConnector.class, true));
        registerBeanDefinitionParser("ssl-key-store", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("ssl-client", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("ssl-server", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("ssl-protocol-handler", new CompoundElementDefinitionParser());
    }
    
}
