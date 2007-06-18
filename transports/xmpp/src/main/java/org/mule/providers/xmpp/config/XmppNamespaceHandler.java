/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp.config;

import org.mule.config.spring.parsers.general.SingleElementDefinitionParser;
import org.mule.providers.xmpp.XmppConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling <code><xmpp:connector></code> elements.
 */
public class XmppNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        this.registerBeanDefinitionParser("connector", 
            new SingleElementDefinitionParser(XmppConnector.class, true));
    }
}
