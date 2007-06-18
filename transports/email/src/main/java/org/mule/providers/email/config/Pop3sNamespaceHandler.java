/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.config.spring.parsers.general.CompoundElementDefinitionParser;
import org.mule.config.spring.parsers.general.SingleElementDefinitionParser;
import org.mule.providers.email.Pop3sConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class Pop3sNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new SingleElementDefinitionParser(Pop3sConnector.class, true));
        registerBeanDefinitionParser("tls-trust-store", new CompoundElementDefinitionParser());
        registerBeanDefinitionParser("tls-client", new CompoundElementDefinitionParser());
    }
}