/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.config;

import org.mule.providers.jms.test.TestConnectionFactory;
import org.mule.providers.jms.test.TestJmsConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** Registers Bean Definition Parsers for the "jms" namespace. */
public class JmsTestNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new JmsConnectorDefinitionParser(TestJmsConnector.class));
        registerBeanDefinitionParser("connection-factory", new ConnectionFactoryDefinitionParser(TestConnectionFactory.class));
    }
    
}