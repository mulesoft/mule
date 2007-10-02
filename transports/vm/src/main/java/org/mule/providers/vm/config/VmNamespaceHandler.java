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
import org.mule.config.spring.handlers.AbstractIgnorableNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.impl.endpoint.OutboundEndpoint;
import org.mule.providers.vm.VMConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><vm:connector></code> elements.
 *
 */
public class VmNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(VMConnector.class, true));
        registerBeanDefinitionParser("queueProfile", new ChildDefinitionParser("queueProfile", QueueProfile.class));
        registerBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser("vm"));
        registerBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser("vm", InboundEndpoint.class));
        registerBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser("vm", OutboundEndpoint.class));
    }

}
