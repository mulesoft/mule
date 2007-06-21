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

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.providers.ssl.TlsConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><tls:connector></code> elements.
 */
public class TlsNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new OrphanDefinitionParser(TlsConnector.class, true));
        registerBeanDefinitionParser("tls-key-store", new ParentDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ParentDefinitionParser());
        registerBeanDefinitionParser("tls-server", new ParentDefinitionParser());
        registerBeanDefinitionParser("tls-protocol-handler", new ParentDefinitionParser());
    }

}