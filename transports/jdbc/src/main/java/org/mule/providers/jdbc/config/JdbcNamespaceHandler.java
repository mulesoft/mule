/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jdbc.config;

import org.mule.config.spring.parsers.generic.SingleElementDefinitionParser;
import org.mule.providers.jdbc.JdbcConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/** Registers Bean Definition Parsers for the "jdbc" namespace. */
public class JdbcNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new SingleElementDefinitionParser(JdbcConnector.class, true));
//        registerBeanDefinitionParser("jndi-resource", new SimpleChildDefinitionParser("jndiResource", JndiResource.class));
//        registerBeanDefinitionParser("provider-properties", new ChildMapDefinitionParser("providerProperties"));
    }

//    class JndiResourceDefinitionParser extends SimpleChildDefinitionParser
//    {
//        public JndiResourceDefinitionParser(String setterMethod)
//        {
//            super(setterMethod, JndiResource.class);
//            registerAttributeMapping("scheduler", "quartzScheduler");
//        }
//    }
}
