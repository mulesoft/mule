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
 * Handles all configuration elements in the Mule Management module.
 *
 */
public class ManagementNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("jmx-server", new JmxAgentDefinitionParser());
        registerBeanDefinitionParser("jmx-log4j", new SingleElementDefinitionParser(Log4jAgent.class, true));
        registerBeanDefinitionParser("jmx-mx4j-adaptor", new SingleElementDefinitionParser(Mx4jAgent.class, true));
        registerBeanDefinitionParser("jmx-notifications", new SingleElementDefinitionParser(JmxServerNotificationAgent.class, true));
        registerBeanDefinitionParser("jmx-default-configuration", new SingleElementDefinitionParser(DefaultJmxSupportAgent.class, true));
        registerBeanDefinitionParser("chainsaw-notifications", new SingleElementDefinitionParser(Log4jNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("level-mappings", new PropertiesDefinitionParser("levelMappings"));
        registerBeanDefinitionParser("log4j-notifications", new SingleElementDefinitionParser(Log4jNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("publish-notifications", new SingleElementDefinitionParser(EndpointNotificationLoggerAgent.class, true));
        registerBeanDefinitionParser("rmi-server", new SingleElementDefinitionParser(RmiRegistryAgent.class, true, "initialise", "dispose"));

        //This gets processed by the jmx-server parser
        registerIgnoredElement("connector-server");
    }
}
