/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.assembly.configuration.PrefixValueMap;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionFactoryDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.impl.endpoint.URIBuilder;
import org.mule.providers.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsTransactionFactory;
import org.mule.providers.jms.activemq.ActiveMQJmsConnector;
import org.mule.providers.jms.activemq.ActiveMQXAJmsConnector;
import org.mule.providers.jms.filters.JmsPropertyFilter;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.providers.jms.transformers.JMSMessageToObject;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.providers.jms.weblogic.WeblogicJmsConnector;
import org.mule.providers.jms.websphere.WebsphereJmsConnector;
import org.mule.transaction.XaTransactionFactory;

/**
 * Registers Bean Definition Parsers for the "jms" namespace.
 */
public class JmsNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String QUEUE = "queue";
    public static final String TOPIC = "topic";
    public static final String[][] JMS_ATTRIBUTES = new String[][]{new String[]{QUEUE}, new String[]{TOPIC}};

    public void init()
    {
        registerJmsTransportEndpoints();

        registerBeanDefinitionParser("connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new JmsConnectorDefinitionParser());
        registerBeanDefinitionParser("activemq-connector", new JmsConnectorDefinitionParser(ActiveMQJmsConnector.class));
        registerBeanDefinitionParser("activemq-xa-connector", new JmsConnectorDefinitionParser(ActiveMQXAJmsConnector.class));
        
        registerBeanDefinitionParser("weblogic-connector", new JmsConnectorDefinitionParser(WeblogicJmsConnector.class));
        registerBeanDefinitionParser("websphere-connector", new JmsConnectorDefinitionParser(WebsphereJmsConnector.class));

        registerBeanDefinitionParser("transaction-factory", new TransactionFactoryDefinitionParser(JmsTransactionFactory.class));
        registerBeanDefinitionParser("client-ack-transaction-factory", new TransactionFactoryDefinitionParser(JmsClientAcknowledgeTransactionFactory.class));
        registerBeanDefinitionParser("xa-transaction-factory", new TransactionFactoryDefinitionParser(XaTransactionFactory.class));        

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

