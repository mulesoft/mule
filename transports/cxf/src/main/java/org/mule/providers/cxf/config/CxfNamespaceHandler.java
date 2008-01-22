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
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.cxf.CxfConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class CxfNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(CxfConnector.CXF);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(CxfConnector.class, true));
    }

}
