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

import org.mule.config.spring.parsers.ListEntryDefinitionParser;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class XFireNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        this.registerBeanDefinitionParser("connector", new XfireElementDefinitionParser());
        this.registerBeanDefinitionParser("client-in-handler", 
            new ListEntryDefinitionParser("clientInHandlers"));
        this.registerBeanDefinitionParser("client-out-handler", 
            new ListEntryDefinitionParser("clientOutHandlers"));
        this.registerBeanDefinitionParser("client-service", 
            new ListEntryDefinitionParser("clientServices"));
        this.registerBeanDefinitionParser("server-in-handler", 
            new ListEntryDefinitionParser("serverInHandlers"));
        this.registerBeanDefinitionParser("server-out-handler", 
            new ListEntryDefinitionParser("serverOutHandlers"));
    }
}


