/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.email.Pop3sConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class Pop3sNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(Pop3sConnector.POP3S, URIBuilder.USERHOST_ATTRIBUTES);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(Pop3sConnector.class, true));
        registerBeanDefinitionParser("tls-trust-store", new ParentDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ParentDefinitionParser());
    }
}