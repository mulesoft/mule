/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.config;

import org.mule.runtime.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.runtime.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ObjectFactoryWrapper;
import org.mule.runtime.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.runtime.core.agent.EndpointNotificationLoggerAgent;
import org.mule.runtime.core.agent.Log4jNotificationLoggerAgent;
import org.mule.runtime.module.management.agent.DefaultJmxSupportAgent;
import org.mule.runtime.module.management.agent.JmxServerNotificationAgent;
import org.mule.runtime.module.management.agent.Log4jAgent;
import org.mule.runtime.module.management.agent.Mx4jAgent;
import org.mule.runtime.module.management.agent.RmiRegistryAgent;
import org.mule.runtime.module.management.agent.YourKitProfilerAgent;

/**
 * Handles all configuration elements in the Mule Management module.
 */
public class ManagementNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerBeanDefinitionParser("jmx-server", new JmxAgentDefinitionParser());
        registerBeanDefinitionParser("mBeanServer", new ObjectFactoryWrapper("MBeanServerObjectFactory"));
        registerBeanDefinitionParser("credentials", new ChildMapDefinitionParser("credentials"));
        registerBeanDefinitionParser("jmx-log4j", new DefaultNameMuleOrphanDefinitionParser(Log4jAgent.class));
        registerBeanDefinitionParser("jmx-mx4j-adaptor", new DefaultNameMuleOrphanDefinitionParser(Mx4jAgent.class));
        registerBeanDefinitionParser("jmx-notifications", new DefaultNameMuleOrphanDefinitionParser(JmxServerNotificationAgent.class));

        MuleDefinitionParserConfiguration defaultJmxParser = registerMuleBeanDefinitionParser("jmx-default-config", new DefaultNameMuleOrphanDefinitionParser(DefaultJmxSupportAgent.class));
        defaultJmxParser.addAlias("registerMx4jAdapter", "loadMx4jAgent");
        defaultJmxParser.addAlias("registerLog4j", "loadLog4jAgent");
        
        registerBeanDefinitionParser("level-mapping", new ChildMapEntryDefinitionParser("levelMappings", "severity", "eventId"));

        // these two are identical?
        registerBeanDefinitionParser("log4j-notifications", new DefaultNameMuleOrphanDefinitionParser(Log4jNotificationLoggerAgent.class));
        registerBeanDefinitionParser("chainsaw-notifications", new DefaultNameMuleOrphanDefinitionParser(Log4jNotificationLoggerAgent.class));

        registerBeanDefinitionParser("publish-notifications", new DefaultNameMuleOrphanDefinitionParser(EndpointNotificationLoggerAgent.class));
        registerBeanDefinitionParser("rmi-server", new DefaultNameMuleOrphanDefinitionParser(RmiRegistryAgent.class));
        registerBeanDefinitionParser("yourkit-profiler", new DefaultNameMuleOrphanDefinitionParser(YourKitProfilerAgent.class));

        registerBeanDefinitionParser("outbound-endpoint", new ChildEndpointDefinitionParser(OutboundEndpointFactoryBean.class));

        // This gets processed by the jmx-server parser
        registerIgnoredElement("connector-server");
    }
    
}
