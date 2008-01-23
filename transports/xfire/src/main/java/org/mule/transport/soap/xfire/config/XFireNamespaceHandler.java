/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedListDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedMapDefinitionParser;
import org.mule.transport.soap.xfire.XFireConnector;

public class XFireNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(XFireConnector.XFIRE);
        registerConnectorDefinitionParser(new XfireElementDefinitionParser());
        registerBeanDefinitionParser("client-in-handler", new ChildListEntryDefinitionParser("clientInHandlers"));
        registerBeanDefinitionParser("client-out-handler", new ChildListEntryDefinitionParser("clientOutHandlers"));
        registerBeanDefinitionParser("client-service", new ChildListEntryDefinitionParser("clientServices"));
        registerBeanDefinitionParser("server-in-handler", new ChildListEntryDefinitionParser("serverInHandlers"));
        registerBeanDefinitionParser("server-out-handler", new ChildListEntryDefinitionParser("serverOutHandlers"));
        registerMuleBeanDefinitionParser("soap-11-transport", new NestedListDefinitionParser("properties", "soap11Transports", MapEntryCombiner.VALUE));
        registerMuleBeanDefinitionParser("complex-type", new NestedMapDefinitionParser("properties", "complexTypes"));
    }
    
}


