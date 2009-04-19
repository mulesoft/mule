/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.transport.cometd.container.CometdServletConnector;
import org.mule.transport.cometd.embedded.CometdConnector;
import org.mule.transport.servlet.ServletConnector;
import org.mule.endpoint.URIBuilder;

/**
 * Registers a Bean Definition Parser for handling <code><comet:connector></code> elements and
 * <code><comet:servlet-connector></code> elements.
 */
public class CometdNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(CometdConnector.PROTOCOL);
        registerConnectorDefinitionParser(CometdConnector.class);

        //registerStandardTransportEndpoints(CometdServletConnector.PROTOCOL, URIBuilder.SOCKET_ATTRIBUTES);
        //registerConnectorDefinitionParser(CometdServletConnector.class);

//        registerStandardTransportEndpoints(CometdServletConnector.PROTOCOL, URIBuilder.SOCKET_ATTRIBUTES);
//        registerBeanDefinitionParser("servlet-connector", new MuleOrphanDefinitionParser(CometdServletConnector.class, true));

       registerBeanDefinitionParser("servlet-connector", new MuleOrphanDefinitionParser(CometdServletConnector.class, true));

        registerBeanDefinitionParser("servlet-endpoint", new TransportGlobalEndpointDefinitionParser(CometdServletConnector.PROTOCOL, false, getGlobalEndpointBuilderBeanClass(), new String[]{"path"}, new String[]{}));
        registerBeanDefinitionParser("servlet-inbound-endpoint", new TransportEndpointDefinitionParser(CometdServletConnector.PROTOCOL, false, getInboundEndpointFactoryBeanClass(), new String[]{"path"}, new String[]{}));
        registerBeanDefinitionParser("servlet-outbound-endpoint", new TransportEndpointDefinitionParser(CometdServletConnector.PROTOCOL, false, getOutboundEndpointFactoryBeanClass(), new String[]{"path"}, new String[]{}));


    }

}
