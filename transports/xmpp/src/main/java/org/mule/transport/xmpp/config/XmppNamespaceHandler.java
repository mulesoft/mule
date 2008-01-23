/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.processors.RequireAttribute;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.xmpp.XmppConnector;
import org.mule.util.StringUtils;

import org.w3c.dom.Element;

/**
 * Registers a Bean Definition Parser for handling <code><xmpp:connector></code> elements.
 */
public class XmppNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String RECIPIENT = "recipient";
    public static final String[] REQUIRED_ADDRESS_ATTRIBUTES =
            new String[]{RECIPIENT, URIBuilder.USER, URIBuilder.HOST};

    public void init()
    {
        registerStandardTransportEndpoints(XmppConnector.XMPP, REQUIRED_ADDRESS_ATTRIBUTES).addAlias(RECIPIENT, URIBuilder.PATH).registerPreProcessor(new RequireNickname());
        this.registerConnector(XmppConnector.class);
    }

    public static class RequireNickname implements PreProcessor
    {

        public void preProcess(PropertyConfiguration config, Element element)
        {
            String groupChat = element.getAttribute(XmppConnector.XMPP_GROUP_CHAT);
            if (Boolean.valueOf(groupChat).booleanValue())
            {
                if (StringUtils.isBlank(element.getAttribute(XmppConnector.XMPP_NICKNAME)))
                {
                    throw new RequireAttribute.RequireAttributeException("Attribute " +
                                    XmppConnector.XMPP_NICKNAME + " must be given if " +
                                    XmppConnector.XMPP_GROUP_CHAT + " is true.");
                }
            }
        }

    }

}
