/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.vm.config;

import org.mule.config.QueueProfile;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.specific.EndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.EndpointAddressDefinitionParser;
import org.mule.providers.vm.VMConnector;
import org.mule.impl.endpoint.GlobalEndpoint;
import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.impl.endpoint.OutboundEndpoint;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><vm:connector></code> elements.
 *
 */
public class VmNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleChildDefinitionParser(VMConnector.class, true));
        registerBeanDefinitionParser("queueProfile", new ChildDefinitionParser("queueProfile", QueueProfile.class));
        registerBeanDefinitionParser("endpoint", new EndpointDefinitionParser(GlobalEndpoint.class));
        registerBeanDefinitionParser("inbound-endpoint", new EndpointDefinitionParser(InboundEndpoint.class));
        registerBeanDefinitionParser("outbound-endpoint", new EndpointDefinitionParser(OutboundEndpoint.class));
        registerBeanDefinitionParser("address", new EndpointAddressDefinitionParser("vm").addAlias("address", "hostname"));
    }

}