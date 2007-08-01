/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.routing.outbound.RoundRobinXmlSplitter;

/**
 * Handles all outbound routers in XML config module (separate namespaces for separate component
 * typres helps keep schema restricted)
 */
public class XmlOutboundNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("message-splitter", new RouterDefinitionParser("router", FilteringXmlMessageSplitter.class));
        registerBeanDefinitionParser("round-robin-splitter", new RouterDefinitionParser("router", RoundRobinXmlSplitter.class).addAlias("endpointFiltering", "enableEndpointFiltering"));
        registerBeanDefinitionParser("namespace", new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));
    }

}
