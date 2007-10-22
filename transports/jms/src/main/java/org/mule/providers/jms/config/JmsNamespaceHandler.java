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

import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.providers.jms.activemq.ActiveMQJmsConnector;
import org.mule.providers.jms.weblogic.WeblogicJmsConnector;
import org.mule.providers.jms.websphere.WebsphereJmsConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** Registers Bean Definition Parsers for the "jms" namespace. */
public class JmsNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("activemq-connector", new JmsConnectorDefinitionParser(ActiveMQJmsConnector.class));
        // TODO XA
        //registerBeanDefinitionParser("activemq-xa-connector", new JmsConnectorDefinitionParser(ActiveMQJmsConnector.class));
        registerBeanDefinitionParser("weblogic-connector", new JmsConnectorDefinitionParser(WeblogicJmsConnector.class));
        registerBeanDefinitionParser("websphere-connector", new JmsConnectorDefinitionParser(WebsphereJmsConnector.class));
        
        registerBeanDefinitionParser("connection-factory", new ConnectionFactoryDefinitionParser());
        registerBeanDefinitionParser("redelivery-handler", new ObjectFactoryDefinitionParser("redeliveryHandler"));
    }
    
}

