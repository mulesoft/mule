/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.http.config;

import org.mule.config.spring.parsers.CompoundElementDefinitionParser;
import org.mule.config.spring.parsers.SingleElementDefinitionParser;
import org.mule.providers.http.HttpsConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><ssl:connector></code> elements.
 */
public class HttpsNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new SingleElementDefinitionParser(HttpsConnector.class, true));
        registerBeanDefinitionParser("tls-key-store", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("tls-client", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("tls-server", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("tls-protocol-handler", new CompoundElementDefinitionParser());
    }

}