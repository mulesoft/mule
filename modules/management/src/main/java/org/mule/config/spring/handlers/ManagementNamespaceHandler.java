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

import org.mule.agent.EndpointNotificationLoggerAgent;
import org.mule.agent.Log4jNotificationLoggerAgent;
import org.mule.config.spring.parsers.JmxAgentDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryWrapper;
import org.mule.config.spring.parsers.specific.AgentDefinitionParser;
import org.mule.management.agents.DefaultJmxSupportAgent;
import org.mule.management.agents.JmxServerNotificationAgent;
import org.mule.management.agents.Log4jAgent;
import org.mule.management.agents.Mx4jAgent;
import org.mule.management.agents.RmiRegistryAgent;
import org.mule.management.agents.YourKitProfilerAgent;

/**
 * Handles all configuration elements in the Mule Management module.
 */
public class ManagementNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jmx-server", new JmxAgentDefinitionParser());
        registerBeanDefinitionParser("mBeanServer", new ObjectFactoryWrapper("MBeanServerObjectFactory"));
        registerBeanDefinitionParser("credentials", new ChildMapDefinitionParser("credentials"));
        registerBeanDefinitionParser("jmx-log4j", new AgentDefinitionParser(Log4jAgent.class));
        registerBeanDefinitionParser("jmx-mx4j-adaptor", new AgentDefinitionParser(Mx4jAgent.class));
        registerBeanDefinitionParser("jmx-notifications", new AgentDefinitionParser(JmxServerNotificationAgent.class));
        registerBeanDefinitionParser("jmx-default-config", new AgentDefinitionParser(DefaultJmxSupportAgent.class));
        registerBeanDefinitionParser("level-mapping", new ChildMapEntryDefinitionParser("levelMappings", "severity", "eventId"));

        // these two are identical?
        registerBeanDefinitionParser("log4j-notifications", new AgentDefinitionParser(Log4jNotificationLoggerAgent.class));
        registerBeanDefinitionParser("chainsaw-notifications", new AgentDefinitionParser(Log4jNotificationLoggerAgent.class));

        registerBeanDefinitionParser("publish-notifications", new AgentDefinitionParser(EndpointNotificationLoggerAgent.class));
        registerBeanDefinitionParser("rmi-server", new AgentDefinitionParser(RmiRegistryAgent.class));
        registerBeanDefinitionParser("yourkit-profiler", new AgentDefinitionParser(YourKitProfilerAgent.class));
        registerBeanDefinitionParser("custom-agent", new AgentDefinitionParser());

        //This gets processed by the jmx-server parser
        registerIgnoredElement("connector-server");
    }
}
