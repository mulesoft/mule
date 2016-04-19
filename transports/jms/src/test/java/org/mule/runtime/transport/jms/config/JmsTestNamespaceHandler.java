/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.config;

import org.mule.runtime.transport.jms.config.ConnectionFactoryDefinitionParser;
import org.mule.runtime.transport.jms.config.JmsConnectorDefinitionParser;
import org.mule.runtime.transport.jms.test.TestJmsConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** Registers Bean Definition Parsers for the "jms" namespace. */
public class JmsTestNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new JmsConnectorDefinitionParser(TestJmsConnector.class));
        registerBeanDefinitionParser("connection-factory", new ConnectionFactoryDefinitionParser());
    }
    
}
