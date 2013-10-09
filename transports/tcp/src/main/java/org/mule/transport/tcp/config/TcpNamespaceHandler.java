/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.ClassOrRefDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.tcp.PollingTcpConnector;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.tcp.TcpProtocol;
import org.mule.transport.tcp.protocols.CustomClassLoadingLengthProtocol;
import org.mule.transport.tcp.protocols.DirectProtocol;
import org.mule.transport.tcp.protocols.EOFProtocol;
import org.mule.transport.tcp.protocols.LengthProtocol;
import org.mule.transport.tcp.protocols.MuleMessageDirectProtocol;
import org.mule.transport.tcp.protocols.MuleMessageEOFProtocol;
import org.mule.transport.tcp.protocols.MuleMessageLengthProtocol;
import org.mule.transport.tcp.protocols.MuleMessageSafeProtocol;
import org.mule.transport.tcp.protocols.SafeProtocol;
import org.mule.transport.tcp.protocols.StreamingProtocol;
import org.mule.transport.tcp.protocols.XmlMessageEOFProtocol;
import org.mule.transport.tcp.protocols.XmlMessageProtocol;

/**
 * Registers a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 */
public class TcpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    private static final String TCP_PROTOCOL_PROPERTY = "tcpProtocol";

    public void init()
    {
        registerStandardTransportEndpoints(TcpConnector.TCP, URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(TcpConnector.class);

        registerBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(PollingTcpConnector.class, true));
        registerBeanDefinitionParser("custom-protocol", new ChildDefinitionParser("tcpProtocol", null, TcpProtocol.class, true));
        registerBeanDefinitionParser("xml-protocol", new ChildDefinitionParser("tcpProtocol", XmlMessageProtocol.class));
        registerBeanDefinitionParser("xml-eof-protocol", new ChildDefinitionParser("tcpProtocol", XmlMessageEOFProtocol.class));
        registerBeanDefinitionParser("safe-protocol", new ByteOrMessageProtocolDefinitionParser(SafeProtocol.class, MuleMessageSafeProtocol.class));
        registerBeanDefinitionParser("length-protocol", new ByteOrMessageProtocolDefinitionParser(LengthProtocol.class, MuleMessageLengthProtocol.class));
        registerBeanDefinitionParser("eof-protocol", new ByteOrMessageProtocolDefinitionParser(EOFProtocol.class, MuleMessageEOFProtocol.class));
        registerBeanDefinitionParser("direct-protocol", new ByteOrMessageProtocolDefinitionParser(DirectProtocol.class, MuleMessageDirectProtocol.class));
        registerBeanDefinitionParser("streaming-protocol", new ByteOrMessageProtocolDefinitionParser(StreamingProtocol.class, MuleMessageDirectProtocol.class));
        registerBeanDefinitionParser("custom-protocol", new ClassOrRefDefinitionParser(TCP_PROTOCOL_PROPERTY));
        registerBeanDefinitionParser("custom-class-loading-protocol", new ByteOrMessageProtocolDefinitionParser(CustomClassLoadingLengthProtocol.class, CustomClassLoadingLengthProtocol.class));
    }

}
