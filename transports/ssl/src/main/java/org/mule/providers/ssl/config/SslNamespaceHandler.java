/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.ssl.config;

import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.providers.ssl.SslConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><ssl:connector></code> elements.
 */
public class SslNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleChildDefinitionParser(SslConnector.class, true));
        registerBeanDefinitionParser("ssl-key-store", new ParentDefinitionParser());
        registerBeanDefinitionParser("ssl-client", new ParentDefinitionParser());
        registerBeanDefinitionParser("ssl-server", new ParentDefinitionParser());
        registerBeanDefinitionParser("ssl-protocol-handler", new ParentDefinitionParser());
    }
    
}
