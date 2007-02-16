/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.JmxAgentDefinitionParser;
import org.mule.config.spring.parsers.PropertiesDefinitionParser;
import org.mule.config.spring.parsers.SingleElementDefinitionParser;
import org.mule.impl.internal.admin.EndpointNotificationLoggerAgent;
import org.mule.impl.internal.admin.Log4jNotificationLoggerAgent;
import org.mule.management.agents.DefaultJmxSupportAgent;
import org.mule.management.agents.JmxServerNotificationAgent;
import org.mule.management.agents.Log4jAgent;
import org.mule.management.agents.Mx4jAgent;
import org.mule.management.agents.RmiRegistryAgent;

/**
 * TODO document
 *
 */
public class ManagementNamespaceHandler extends AbstractHierarchicalNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jmx-server", new JmxAgentDefinitionParser());
        registerBeanDefinitionParser("jmx-log4j", new SingleElementDefinitionParser(Log4jAgent.class));
        registerBeanDefinitionParser("jmx-mx4j-adaptor", new SingleElementDefinitionParser(Mx4jAgent.class));
        registerBeanDefinitionParser("jmx-notifications", new SingleElementDefinitionParser(JmxServerNotificationAgent.class));
        registerBeanDefinitionParser("jmx-default-configuration", new SingleElementDefinitionParser(DefaultJmxSupportAgent.class));
        registerBeanDefinitionParser("chainsaw-notifications", new SingleElementDefinitionParser(Log4jNotificationLoggerAgent.class));
        registerBeanDefinitionParser("level-mappings", new PropertiesDefinitionParser("levelMappings"));
        registerBeanDefinitionParser("log4j-notifications", new SingleElementDefinitionParser(Log4jNotificationLoggerAgent.class));
        registerBeanDefinitionParser("publish-notifications", new SingleElementDefinitionParser(EndpointNotificationLoggerAgent.class));
        registerBeanDefinitionParser("rmi-server", new SingleElementDefinitionParser(RmiRegistryAgent.class));

        //This gets processed by the jmx-server parser
        registerIgnoredElement("connector-server");
    }
}
