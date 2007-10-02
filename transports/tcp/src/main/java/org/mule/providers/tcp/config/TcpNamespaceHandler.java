/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.tcp.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.providers.tcp.TcpConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class TcpNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(TcpConnector.class, true));
    }
}