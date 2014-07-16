/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.DataObjectDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.EndpointPropertyElementDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.EndpointRefParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.jobs.CustomJobConfig;
import org.mule.transport.quartz.jobs.CustomJobFromMessageConfig;
import org.mule.transport.quartz.jobs.EndpointPollingJobConfig;
import org.mule.transport.quartz.jobs.EventGeneratorJobConfig;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

/**
 * Registers Bean Definition Parsers for the "quartz" namespace
 */
public class QuartzNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String JOB_NAME_ATTRIBUTE = "jobName";
    public static final String[][] QUARTZ_ATTRIBUTES = new String[][]{new String[]{JOB_NAME_ATTRIBUTE}};

    public void init()
    {
        registerQuartzTransportEndpoints();
        registerMuleBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(QuartzConnector.class, true)).addAlias("scheduler", "quartzScheduler");
        // note that we use the singular (factoryProperty) for the setter so that we auto-detect a collection
        registerBeanDefinitionParser("factory-property", new ChildSingletonMapDefinitionParser("factoryProperty"));
        registerBeanDefinitionParser("factory-properties", new ChildMapDefinitionParser("factoryProperty"));

        registerBeanDefinitionParser("event-generator-job", new EndpointPropertyElementDefinitionParser(QuartzConnector.PROPERTY_JOB_CONFIG, EventGeneratorJobConfig.class));
        registerBeanDefinitionParser("endpoint-polling-job", new EndpointPropertyElementDefinitionParser(QuartzConnector.PROPERTY_JOB_CONFIG, EndpointPollingJobConfig.class));
        registerBeanDefinitionParser("scheduled-dispatch-job", new EndpointPropertyElementDefinitionParser(QuartzConnector.PROPERTY_JOB_CONFIG, ScheduledDispatchJobConfig.class));
        registerBeanDefinitionParser("custom-job", new EndpointPropertyElementDefinitionParser(QuartzConnector.PROPERTY_JOB_CONFIG, CustomJobConfig.class));
        registerBeanDefinitionParser("custom-job-from-message", new EndpointPropertyElementDefinitionParser(QuartzConnector.PROPERTY_JOB_CONFIG, CustomJobFromMessageConfig.class));

        ParentDefinitionParser parser = new ParentDefinitionParser();
        parser.addAlias("address", "endpointRef");
        parser.addAlias("ref", "endpointRef");
        registerBeanDefinitionParser("job-endpoint", new EndpointRefParser("endpointRef"));

        registerBeanDefinitionParser("payload", new DataObjectDefinitionParser("payload"));
    }

    /**
     * Need to use the most complex constructors as have mutually exclusive address
     * aattributes
     */
    protected void registerQuartzTransportEndpoints()
    {
        registerQuartzEndpointDefinitionParser("endpoint",
            new TransportGlobalEndpointDefinitionParser(QuartzConnector.QUARTZ,
                TransportGlobalEndpointDefinitionParser.PROTOCOL,
                TransportGlobalEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, QUARTZ_ATTRIBUTES,
                new String[][]{}));
        registerQuartzEndpointDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(
            QuartzConnector.QUARTZ, TransportEndpointDefinitionParser.PROTOCOL, InboundEndpointFactoryBean.class,
            TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, QUARTZ_ATTRIBUTES, new String[][]{}));
        registerQuartzEndpointDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(
            QuartzConnector.QUARTZ, TransportEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class,
            TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, QUARTZ_ATTRIBUTES, new String[][]{}));
    }

    protected void registerQuartzEndpointDefinitionParser(String element, MuleDefinitionParser parser)
    {
        parser.addAlias(JOB_NAME_ATTRIBUTE, URIBuilder.PATH);
        registerBeanDefinitionParser(element, parser);
    }
}
