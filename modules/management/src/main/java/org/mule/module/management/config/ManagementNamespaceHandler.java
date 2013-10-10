/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.config;

import org.mule.agent.EndpointNotificationLoggerAgent;
import org.mule.agent.Log4jNotificationLoggerAgent;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryWrapper;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.module.management.agent.DefaultJmxSupportAgent;
import org.mule.module.management.agent.JmxServerNotificationAgent;
import org.mule.module.management.agent.Log4jAgent;
import org.mule.module.management.agent.Mx4jAgent;
import org.mule.module.management.agent.RmiRegistryAgent;
import org.mule.module.management.agent.YourKitProfilerAgent;

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
