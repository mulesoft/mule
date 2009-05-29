/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.transport.ajax.container.AjaxServletConnector;
import org.mule.transport.ajax.embedded.AjaxConnector;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;ajax:connector&gt;</code> elements and
 * <code>&lt;ajax:servlet-connector&gt;</code> elements.
 */
public class AjaxNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(AjaxConnector.PROTOCOL);
        registerConnectorDefinitionParser(AjaxConnector.class);

        //registerStandardTransportEndpoints(AjaxServletConnector.PROTOCOL, URIBuilder.SOCKET_ATTRIBUTES);
        //registerConnectorDefinitionParser(AjaxServletConnector.class);

//        registerStandardTransportEndpoints(AjaxServletConnector.PROTOCOL, URIBuilder.SOCKET_ATTRIBUTES);
//        registerBeanDefinitionParser("servlet-connector", new MuleOrphanDefinitionParser(AjaxServletConnector.class, true));

       registerBeanDefinitionParser("servlet-connector", new MuleOrphanDefinitionParser(AjaxServletConnector.class, true));

        registerBeanDefinitionParser("servlet-endpoint", new TransportGlobalEndpointDefinitionParser(AjaxServletConnector.PROTOCOL, false, getGlobalEndpointBuilderBeanClass(), new String[]{"path"}, new String[]{}));
        registerBeanDefinitionParser("servlet-inbound-endpoint", new TransportEndpointDefinitionParser(AjaxServletConnector.PROTOCOL, false, getInboundEndpointFactoryBeanClass(), new String[]{"path"}, new String[]{}));
        registerBeanDefinitionParser("servlet-outbound-endpoint", new TransportEndpointDefinitionParser(AjaxServletConnector.PROTOCOL, false, getOutboundEndpointFactoryBeanClass(), new String[]{"path"}, new String[]{}));
    }

}
