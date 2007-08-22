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
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildPropertiesDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
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
        registerBeanDefinitionParser("credentials", new ChildPropertiesDefinitionParser("credentials"));
        registerBeanDefinitionParser("jmx-log4j", new OrphanDefinitionParser(Log4jAgent.class, true));
        registerBeanDefinitionParser("jmx-mx4j-adaptor", new OrphanDefinitionParser(Mx4jAgent.class, true));
        registerBeanDefinitionParser("jmx-notifications", new OrphanDefinitionParser(JmxServerNotificationAgent.class, true));
        registerBeanDefinitionParser("jmx-default-config", new OrphanDefinitionParser(DefaultJmxSupportAgent.class, true));
        registerBeanDefinitionParser("chainsaw-notifications", new OrphanDefinitionParser(Log4jNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("level-mapping", new ChildMapEntryDefinitionParser("levelMappings", "severity", "eventId"));
        registerBeanDefinitionParser("log4j-notifications", new OrphanDefinitionParser(Log4jNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("publish-notifications", new OrphanDefinitionParser(EndpointNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("rmi-server", new OrphanDefinitionParser(RmiRegistryAgent.class, true));


        
        
        //This gets processed by the jmx-server parser
        registerIgnoredElement("connector-server");
    }
}
