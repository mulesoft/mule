/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.impl.endpoint.URIBuilder;
import org.mule.providers.xmpp.XmppConnector;

/**
 * Registers a Bean Definition Parser for handling <code><xmpp:connector></code> elements.
 */
public class XmppNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(XmppConnector.XMPP, URIBuilder.USERHOST_ATTRIBUTES);
        this.registerBeanDefinitionParser("connector", 
            new MuleOrphanDefinitionParser(XmppConnector.class, true));
    }
}
