/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.config;

import org.mule.compatibility.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.tcp.DefaultTcpClientSocketProperties;
import org.mule.compatibility.transport.tcp.DefaultTcpServerSocketProperties;
import org.mule.compatibility.transport.tcp.PollingTcpConnector;
import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.compatibility.transport.tcp.TcpProtocol;
import org.mule.compatibility.transport.tcp.protocols.CustomClassLoadingLengthProtocol;
import org.mule.compatibility.transport.tcp.protocols.DirectProtocol;
import org.mule.compatibility.transport.tcp.protocols.EOFProtocol;
import org.mule.compatibility.transport.tcp.protocols.LengthProtocol;
import org.mule.compatibility.transport.tcp.protocols.MuleMessageDirectProtocol;
import org.mule.compatibility.transport.tcp.protocols.MuleMessageEOFProtocol;
import org.mule.compatibility.transport.tcp.protocols.MuleMessageLengthProtocol;
import org.mule.compatibility.transport.tcp.protocols.MuleMessageSafeProtocol;
import org.mule.compatibility.transport.tcp.protocols.SafeProtocol;
import org.mule.compatibility.transport.tcp.protocols.StreamingProtocol;
import org.mule.compatibility.transport.tcp.protocols.XmlMessageEOFProtocol;
import org.mule.compatibility.transport.tcp.protocols.XmlMessageProtocol;
import org.mule.runtime.config.spring.parsers.ClassOrRefDefinitionParser;
import org.mule.runtime.config.spring.parsers.delegate.RootOrNestedElementBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;

/**
 * Registers a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 */
public class TcpNamespaceHandler extends AbstractMuleTransportsNamespaceHandler {

  private static final String TCP_PROTOCOL_PROPERTY = "tcpProtocol";

  @Override
  public void init() {
    registerStandardTransportEndpoints(TcpConnector.TCP, URIBuilder.SOCKET_ATTRIBUTES);
    registerConnectorDefinitionParser(TcpConnector.class);

    registerBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(PollingTcpConnector.class, true));
    registerBeanDefinitionParser("custom-protocol", new ChildDefinitionParser("tcpProtocol", null, TcpProtocol.class, true));
    registerBeanDefinitionParser("xml-protocol", new ChildDefinitionParser("tcpProtocol", XmlMessageProtocol.class));
    registerBeanDefinitionParser("xml-eof-protocol", new ChildDefinitionParser("tcpProtocol", XmlMessageEOFProtocol.class));
    registerBeanDefinitionParser("safe-protocol",
                                 new ByteOrMessageProtocolDefinitionParser(SafeProtocol.class, MuleMessageSafeProtocol.class));
    registerBeanDefinitionParser("length-protocol", new ByteOrMessageProtocolDefinitionParser(LengthProtocol.class,
                                                                                              MuleMessageLengthProtocol.class));
    registerBeanDefinitionParser("eof-protocol",
                                 new ByteOrMessageProtocolDefinitionParser(EOFProtocol.class, MuleMessageEOFProtocol.class));
    registerBeanDefinitionParser("direct-protocol", new ByteOrMessageProtocolDefinitionParser(DirectProtocol.class,
                                                                                              MuleMessageDirectProtocol.class));
    registerBeanDefinitionParser("streaming-protocol",
                                 new ByteOrMessageProtocolDefinitionParser(StreamingProtocol.class,
                                                                           MuleMessageDirectProtocol.class));
    registerBeanDefinitionParser("custom-protocol", new ClassOrRefDefinitionParser(TCP_PROTOCOL_PROPERTY));
    registerBeanDefinitionParser("custom-class-loading-protocol",
                                 new ByteOrMessageProtocolDefinitionParser(CustomClassLoadingLengthProtocol.class,
                                                                           CustomClassLoadingLengthProtocol.class));
    registerBeanDefinitionParser("client-socket-properties",
                                 new RootOrNestedElementBeanDefinitionParser(DefaultTcpClientSocketProperties.class,
                                                                             "clientSocketProperties"));
    registerBeanDefinitionParser("server-socket-properties",
                                 new RootOrNestedElementBeanDefinitionParser(DefaultTcpServerSocketProperties.class,
                                                                             "serverSocketProperties"));
  }

}
