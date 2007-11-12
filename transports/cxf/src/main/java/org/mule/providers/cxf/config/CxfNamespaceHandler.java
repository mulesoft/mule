/*
 * $Id: XFireNamespaceHandler.java 7167 2007-06-19 19:57:12Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.providers.cxf.CxfConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class CxfNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        String[] endpointProps = new String[] { 
            "frontend", 
            "bindingUri",
            "bridge",
            "endpointName",
            "serviceName",
            "wsdlLocation" 
        };
        
        registerBeanDefinitionParser("connector", new OrphanDefinitionParser(CxfConnector.class, true));
        registerBeanDefinitionParser("endpoint", 
            new TransportGlobalEndpointDefinitionParser(
                    "cxf",
                    TransportGlobalEndpointDefinitionParser.META,
                    endpointProps, 
                    new String[]{}));
        registerBeanDefinitionParser("inbound-endpoint", 
            new TransportEndpointDefinitionParser(
                    "cxf",
                    TransportEndpointDefinitionParser.META,
                    InboundEndpointFactoryBean.class,
                    endpointProps,
                    new String[]{}));
    }
}
