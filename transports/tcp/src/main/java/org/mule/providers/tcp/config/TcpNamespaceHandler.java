/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.tcp.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.providers.tcp.TcpConnector;
import org.mule.providers.tcp.TcpProtocol;
import org.mule.providers.tcp.protocols.DirectProtocol;
import org.mule.providers.tcp.protocols.EOFProtocol;
import org.mule.providers.tcp.protocols.LengthProtocol;
import org.mule.providers.tcp.protocols.MuleMessageDirectProtocol;
import org.mule.providers.tcp.protocols.MuleMessageEOFProtocol;
import org.mule.providers.tcp.protocols.MuleMessageLengthProtocol;
import org.mule.providers.tcp.protocols.MuleMessageSafeProtocol;
import org.mule.providers.tcp.protocols.SafeProtocol;
import org.mule.providers.tcp.protocols.XmlMessageEOFProtocol;
import org.mule.providers.tcp.protocols.XmlMessageProtocol;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class TcpNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(TcpConnector.class, true));
        registerBeanDefinitionParser("custom-protocol", new ChildDefinitionParser("tcpProtocol", null, TcpProtocol.class, true));
        registerBeanDefinitionParser("xml-protocol", new ChildDefinitionParser("tcpProtocol", XmlMessageProtocol.class));
        registerBeanDefinitionParser("xml-eof-protocol", new ChildDefinitionParser("tcpProtocol", XmlMessageEOFProtocol.class));
        registerBeanDefinitionParser("safe-protocol", new ByteOrMessageProtocolDefinitionParser(SafeProtocol.class, MuleMessageSafeProtocol.class));
        registerBeanDefinitionParser("length-protocol", new ByteOrMessageProtocolDefinitionParser(LengthProtocol.class, MuleMessageLengthProtocol.class));
        registerBeanDefinitionParser("eof-protocol", new ByteOrMessageProtocolDefinitionParser(EOFProtocol.class, MuleMessageEOFProtocol.class));
        registerBeanDefinitionParser("direct-protocol", new ByteOrMessageProtocolDefinitionParser(DirectProtocol.class, MuleMessageDirectProtocol.class));
    }

}