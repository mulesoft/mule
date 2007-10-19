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

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.test.TestConnectionFactory;
import org.mule.providers.jms.activemq.ActiveMqJmsConnector;
import org.mule.providers.jms.weblogic.WeblogicJmsConnector;
import org.mule.providers.jms.websphere.WebsphereJmsConnector;
import org.mule.util.StringUtils;

import javax.jms.Session;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Registers Bean Definition Parsers for the "jms" namespace. */
public class JmsTestNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connection-factory", new ConnectionFactoryDefinitionParser(TestConnectionFactory.class));
    }
    
}