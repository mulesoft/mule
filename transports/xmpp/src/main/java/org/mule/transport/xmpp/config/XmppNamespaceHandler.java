/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.xmpp.XmppConnector;
import org.mule.transport.xmpp.transformers.ObjectToXmppPacket;
import org.mule.transport.xmpp.transformers.XmppPacketToObject;

/**
 * Registers a Bean Definition Parser for handling <code><xmpp:connector></code> elements.
 */
public class XmppNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String RECIPIENT = "recipient";
    public static final String[] REQUIRED_ADDRESS_ATTRIBUTES =
            new String[]{RECIPIENT, URIBuilder.USER, URIBuilder.HOST};
      
    // required attributes for outbound endpoints
    private static final String[][] REQUIRED_OUTBOUND_ATTRIBUTE_GROUPS = new String[][] {
        new String[] { XmppConnector.XMPP_TYPE, XmppConnector.XMPP_RECIPIENT }
    };

    // required attributes for inbound endpoints
    private static final String[][] REQUIRED_INBOUND_ATTRIBUTE_GROUPS = new String[][] {
        new String[] { XmppConnector.XMPP_TYPE, XmppConnector.XMPP_FROM }
    };

    // required attributes for global endpoints
    private static final String[][] REQUIRED_ALL_ATTRIBUTE_GROUPS = new String[][] {
        new String[] { XmppConnector.XMPP_TYPE, XmppConnector.XMPP_FROM },
        new String[] { XmppConnector.XMPP_TYPE, XmppConnector.XMPP_RECIPIENT }
    };

    private static final String[][] REQUIRED_MESSAGE_PROPERTY_GROUPS = new String[][] {
        new String[] { }
    };

    public void init()
    {        
        registerGlobalEndpointParser();
        registerInboundEndpointParser();
        registerOutboundEndpintParser();
        
        registerConnectorDefinitionParser(XmppConnector.class);
        registerBeanDefinitionParser("xmpp-to-object-transformer", new MessageProcessorDefinitionParser(XmppPacketToObject.class));
        registerBeanDefinitionParser("object-to-xmpp-transformer", new MessageProcessorDefinitionParser(ObjectToXmppPacket.class));
    }

    private void registerGlobalEndpointParser()
    {
        TransportGlobalEndpointDefinitionParser endpointDefinitionParser = 
            new TransportGlobalEndpointDefinitionParser(XmppConnector.XMPP, 
                TransportGlobalEndpointDefinitionParser.PROTOCOL, 
                TransportGlobalEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, 
                REQUIRED_ALL_ATTRIBUTE_GROUPS, REQUIRED_MESSAGE_PROPERTY_GROUPS);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_TYPE, URIBuilder.HOST);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_RECIPIENT, URIBuilder.PATH);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_FROM, URIBuilder.PATH);
        registerBeanDefinitionParser("endpoint", endpointDefinitionParser);
    }

    private void registerInboundEndpointParser()
    {
        TransportEndpointDefinitionParser endpointDefinitionParser = 
            new TransportEndpointDefinitionParser(XmppConnector.XMPP, 
                TransportEndpointDefinitionParser.PROTOCOL, InboundEndpointFactoryBean.class, 
                TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, 
                REQUIRED_INBOUND_ATTRIBUTE_GROUPS, REQUIRED_MESSAGE_PROPERTY_GROUPS);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_TYPE, URIBuilder.HOST);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_FROM, URIBuilder.PATH);
        registerBeanDefinitionParser("inbound-endpoint", endpointDefinitionParser);
    }

    private void registerOutboundEndpintParser()
    {
        TransportEndpointDefinitionParser endpointDefinitionParser = 
            new TransportEndpointDefinitionParser(XmppConnector.XMPP, 
                TransportEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class, 
                TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, 
                REQUIRED_OUTBOUND_ATTRIBUTE_GROUPS, REQUIRED_MESSAGE_PROPERTY_GROUPS);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_TYPE, URIBuilder.HOST);
        endpointDefinitionParser.addAlias(XmppConnector.XMPP_RECIPIENT, URIBuilder.PATH);
        registerBeanDefinitionParser("outbound-endpoint", endpointDefinitionParser);
    }
}
