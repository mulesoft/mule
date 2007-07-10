/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.config;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.activemq.ActiveMqJmsConnector;
import org.mule.providers.jms.weblogic.WeblogicJmsConnector;
import org.mule.providers.jms.websphere.WebsphereJmsConnector;
import org.mule.util.StringUtils;

import javax.jms.Session;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Registers Bean Definition Parsers for the "jms" namespace. */
public class JmsNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("activemq-connector", new JmsConnectorDefinitionParser(ActiveMqJmsConnector.class));
        // TODO XA
        //registerBeanDefinitionParser("activemq-xa-connector", new JmsConnectorDefinitionParser(ActiveMqJmsConnector.class));
        registerBeanDefinitionParser("weblogic-connector", new JmsConnectorDefinitionParser(WeblogicJmsConnector.class));
        registerBeanDefinitionParser("websphere-connector", new JmsConnectorDefinitionParser(WebsphereJmsConnector.class));
        
        registerBeanDefinitionParser("connection-factory", new ConnectionFactoryDefinitionParser());
        registerBeanDefinitionParser("redelivery-handler", new ObjectFactoryDefinitionParser("redeliveryHandler"));
    }
}

class JmsConnectorDefinitionParser extends OrphanDefinitionParser
{
    public JmsConnectorDefinitionParser()
    {
        this(JmsConnector.class);
    }

    public JmsConnectorDefinitionParser(Class clazz)
    {
        super(clazz, true);
        addMapping("acknowledgementMode", 
            "AUTO_ACKNOWLEDGE=" + Session.AUTO_ACKNOWLEDGE + "," +
            "CLIENT_ACKNOWLEDGE=" + Session.CLIENT_ACKNOWLEDGE + "," +
            "DUPS_OK_ACKNOWLEDGE=" + Session.DUPS_OK_ACKNOWLEDGE);
    }
}

class ConnectionFactoryDefinitionParser extends ObjectFactoryDefinitionParser
{
    public ConnectionFactoryDefinitionParser()
    {
        super("connectionFactory");
    }

    // TODO AC: How can we simplify this code using BeanAssemblers?
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        BeanDefinition parent = getParentBeanDefinition(element);

        String username = element.getAttribute("username");
        if (StringUtils.isNotBlank(username))
        {
            parent.getPropertyValues().addPropertyValue("username", username);
            element.removeAttribute("username");
        }

        String password = element.getAttribute("password");
        if (StringUtils.isNotBlank(password))
        {
            parent.getPropertyValues().addPropertyValue("password", password);
            element.removeAttribute("password");
        }
            
        super.parseChild(element, parserContext, builder);
    }
}

