/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.factories.ResponseEndpointFactoryBean;
import org.mule.providers.email.ImapConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class ImapNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(ImapConnector.class, true));
        registerBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(ImapConnector.IMAP, LazyEndpointURI.UPH_ATTRIBUTES));
        registerBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(ImapConnector.IMAP, InboundEndpointFactoryBean.class, LazyEndpointURI.UPH_ATTRIBUTES));
        registerBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(ImapConnector.IMAP, OutboundEndpointFactoryBean.class, LazyEndpointURI.UPH_ATTRIBUTES));
        registerBeanDefinitionParser("response-endpoint", new TransportEndpointDefinitionParser(ImapConnector.IMAP, ResponseEndpointFactoryBean.class, LazyEndpointURI.UPH_ATTRIBUTES));
    }

}
