/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.config;

import org.mule.transport.jms.config.ConnectionFactoryDefinitionParser;
import org.mule.transport.jms.config.JmsConnectorDefinitionParser;
import org.mule.transport.jms.test.TestJmsConnector;

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
