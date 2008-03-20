/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.transport.cxf.CxfConnector;
import org.mule.transport.cxf.CxfConstants;
import org.mule.endpoint.URIBuilder;

public class CxfNamespaceHandler extends AbstractMuleNamespaceHandler
{

    /// this is a subset of {@link URIBuilder#ALL_ATTRIBUTES}
    String[] addressAttributes =
            new String[]{URIBuilder.META, URIBuilder.PROTOCOL, URIBuilder.USER, URIBuilder.PASSWORD,
                    URIBuilder.HOST, URIBuilder.ADDRESS, URIBuilder.PATH};

    public void init()
    {
        registerBeanDefinitionParser(AbstractMuleNamespaceHandler.GLOBAL_ENDPOINT,
                new TransportGlobalEndpointDefinitionParser(CxfConnector.CXF, true,
                        addressAttributes, new String[][]{}, new String[][]{}));
        registerBeanDefinitionParser(AbstractMuleNamespaceHandler.OUTBOUND_ENDPOINT,
                new TransportEndpointDefinitionParser(CxfConnector.CXF, true, OutboundEndpointFactoryBean.class,
                        addressAttributes, new String[][]{}, new String[][]{}));
        registerBeanDefinitionParser(AbstractMuleNamespaceHandler.INBOUND_ENDPOINT,
                new TransportEndpointDefinitionParser(CxfConnector.CXF, true, InboundEndpointFactoryBean.class,
                        addressAttributes, new String[][]{}, new String[][]{}));

        registerConnectorDefinitionParser(CxfConnector.class);

        registerBeanDefinitionParser("features",
            new EndpointChildDefinitionParser("features"));

        registerBeanDefinitionParser(CxfConstants.IN_INTERCEPTORS,
            new EndpointChildDefinitionParser(CxfConstants.IN_INTERCEPTORS));

        registerBeanDefinitionParser(CxfConstants.IN_FAULT_INTERCEPTORS,
            new EndpointChildDefinitionParser(CxfConstants.IN_FAULT_INTERCEPTORS));

        registerBeanDefinitionParser(CxfConstants.OUT_INTERCEPTORS,
            new EndpointChildDefinitionParser(CxfConstants.OUT_INTERCEPTORS));

        registerBeanDefinitionParser(CxfConstants.OUT_FAULT_INTERCEPTORS,
            new EndpointChildDefinitionParser(CxfConstants.OUT_FAULT_INTERCEPTORS));
    }

}
