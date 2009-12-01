/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.assembly.configuration.PrefixValueMap;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsTransactionFactory;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.transport.jms.activemq.ActiveMQXAJmsConnector;
import org.mule.transport.jms.filters.JmsPropertyFilter;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.transport.jms.mulemq.MuleMQJmsConnector;
import org.mule.transport.jms.mulemq.MuleMQXAJmsConnector;
import org.mule.transport.jms.transformers.JMSMessageToObject;
import org.mule.transport.jms.transformers.ObjectToJMSMessage;
import org.mule.transport.jms.weblogic.WeblogicJmsConnector;
import org.mule.transport.jms.websphere.WebsphereJmsConnector;

/**
 * Registers Bean Definition Parsers for the "jms" namespace.
 */
public class JmsNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String QUEUE = "queue";
    public static final String TOPIC = "topic";
    public static final String NUMBER_OF_CONSUMERS_ATTRIBUTE = "numberOfConsumers";
    public static final String NUMBER_OF_CONCURRENT_TRANSACTED_RECEIVERS_ATTRIBUTE = "numberOfConcurrentTransactedReceivers";
    public static final String NUMBER_OF_CONSUMERS_PROPERTY = "numberOfConcurrentTransactedReceivers";
    public static final String[][] JMS_ATTRIBUTES = new String[][]{new String[]{QUEUE}, new String[]{TOPIC}};

    public void init()
    {
        registerJmsTransportEndpoints();

        registerMuleBeanDefinitionParser("connector", new JmsConnectorDefinitionParser()).addAlias(
            NUMBER_OF_CONSUMERS_ATTRIBUTE, NUMBER_OF_CONSUMERS_PROPERTY).registerPreProcessor(
            new CheckExclusiveAttributes(new String[][]{
                new String[]{NUMBER_OF_CONCURRENT_TRANSACTED_RECEIVERS_ATTRIBUTE},
                new String[]{NUMBER_OF_CONSUMERS_ATTRIBUTE}}));
        registerBeanDefinitionParser("custom-connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("activemq-connector", new JmsConnectorDefinitionParser(ActiveMQJmsConnector.class));
        registerBeanDefinitionParser("activemq-xa-connector", new JmsConnectorDefinitionParser(ActiveMQXAJmsConnector.class));
        
        registerBeanDefinitionParser("mulemq-connector", new JmsConnectorDefinitionParser(MuleMQJmsConnector.class));
        registerBeanDefinitionParser("mulemq-xa-connector", new JmsConnectorDefinitionParser(MuleMQXAJmsConnector.class));
        
        registerBeanDefinitionParser("weblogic-connector", new JmsConnectorDefinitionParser(WeblogicJmsConnector.class));
        registerBeanDefinitionParser("websphere-connector", new JmsConnectorDefinitionParser(WebsphereJmsConnector.class));

        registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(JmsTransactionFactory.class));
        registerBeanDefinitionParser("client-ack-transaction", new TransactionDefinitionParser(JmsClientAcknowledgeTransactionFactory.class));
        
        registerBeanDefinitionParser("jmsmessage-to-object-transformer", new TransformerDefinitionParser(JMSMessageToObject.class));

        registerBeanDefinitionParser("object-to-jmsmessage-transformer", new TransformerDefinitionParser(ObjectToJMSMessage.class));
        registerBeanDefinitionParser("property-filter", new FilterDefinitionParser(JmsPropertyFilter.class));
        registerBeanDefinitionParser("selector", new ChildDefinitionParser(FilterDefinitionParser.FILTER, JmsSelectorFilter.class));
    }

    /**
     * Need to use the most complex constructors as have mutually exclusive address aattributes
     */
    protected void registerJmsTransportEndpoints()
    {
        registerJmsEndpointDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(JmsConnector.JMS, TransportGlobalEndpointDefinitionParser.PROTOCOL, TransportGlobalEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, JMS_ATTRIBUTES, new String[][]{}));
        registerJmsEndpointDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(JmsConnector.JMS, TransportEndpointDefinitionParser.PROTOCOL, InboundEndpointFactoryBean.class, TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, JMS_ATTRIBUTES, new String[][]{}));
        registerJmsEndpointDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(JmsConnector.JMS, TransportEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class, TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, JMS_ATTRIBUTES, new String[][]{}));
    }

    protected void registerJmsEndpointDefinitionParser(String element, MuleDefinitionParser parser)
    {
        parser.addAlias(QUEUE, URIBuilder.PATH);
        parser.addAlias(TOPIC, URIBuilder.PATH);
        parser.addMapping(TOPIC, new PrefixValueMap(TOPIC + ":"));
        registerBeanDefinitionParser(element, parser);
    }

}

