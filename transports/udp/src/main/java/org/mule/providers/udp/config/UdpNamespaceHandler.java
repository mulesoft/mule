/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.udp.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.udp.UdpConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling UDP specific elements.
 *
 */
public class UdpNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerStandardTransportEndpoints(UdpConnector.UDP, URIBuilder.SOCKET_ATTRIBUTES);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(UdpConnector.class, true));
    }

}