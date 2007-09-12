/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.JmxAgentDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildPropertiesDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.impl.internal.admin.EndpointNotificationLoggerAgent;
import org.mule.impl.internal.admin.Log4jNotificationLoggerAgent;
import org.mule.management.agents.DefaultJmxSupportAgent;
import org.mule.management.agents.JmxServerNotificationAgent;
import org.mule.management.agents.Log4jAgent;
import org.mule.management.agents.Mx4jAgent;
import org.mule.management.agents.RmiRegistryAgent;

/**
 * Handles all configuration elements in the Mule Management module.
 *
 */
public class ManagementNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jmx-server", new JmxAgentDefinitionParser());
        registerBeanDefinitionParser("mBeanServer", new ObjectFactoryDefinitionParser("MBeanServerObjectFactory"));
        registerBeanDefinitionParser("credentials", new ChildMapDefinitionParser("credentials"));
        registerBeanDefinitionParser("jmx-log4j", new MuleChildDefinitionParser(Log4jAgent.class, true));
        registerBeanDefinitionParser("jmx-mx4j-adaptor", new MuleChildDefinitionParser(Mx4jAgent.class, true));
        registerBeanDefinitionParser("jmx-notifications", new MuleChildDefinitionParser(JmxServerNotificationAgent.class, true));
        registerBeanDefinitionParser("jmx-default-config", new MuleChildDefinitionParser(DefaultJmxSupportAgent.class, true));
        registerBeanDefinitionParser("chainsaw-notifications", new MuleChildDefinitionParser(Log4jNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("level-mapping", new ChildMapEntryDefinitionParser("levelMappings", "severity", "eventId"));
        registerBeanDefinitionParser("log4j-notifications", new MuleChildDefinitionParser(Log4jNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("publish-notifications", new MuleChildDefinitionParser(EndpointNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("rmi-server", new MuleChildDefinitionParser(RmiRegistryAgent.class, true));
        registerBeanDefinitionParser("custom-agent", new MuleChildDefinitionParser(true));

        //This gets processed by the jmx-server parser
        registerIgnoredElement("connector-server");
    }
}
