/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.config;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class XFireNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new XfireElementDefinitionParser());
        registerBeanDefinitionParser("client-in-handler", new ChildListEntryDefinitionParser("clientInHandlers"));
        registerBeanDefinitionParser("client-out-handler", new ChildListEntryDefinitionParser("clientOutHandlers"));
        registerBeanDefinitionParser("client-service", new ChildListEntryDefinitionParser("clientServices"));
        registerBeanDefinitionParser("server-in-handler", new ChildListEntryDefinitionParser("serverInHandlers"));
        registerBeanDefinitionParser("server-out-handler", new ChildListEntryDefinitionParser("serverOutHandlers"));
    }
}


