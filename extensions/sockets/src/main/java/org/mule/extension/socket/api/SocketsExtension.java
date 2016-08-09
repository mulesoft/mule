/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api;

import org.mule.extension.socket.api.config.ListenerConfig;
import org.mule.extension.socket.api.config.RequesterConfig;
import org.mule.extension.socket.api.connection.tcp.protocol.AbstractByteProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.CustomProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.DirectProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.EOFProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.LengthProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.SafeProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.StreamingProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.XmlMessageEOFProtocol;
import org.mule.extension.socket.api.connection.tcp.protocol.XmlMessageProtocol;
import org.mule.extension.socket.api.exceptions.LengthExceededException;
import org.mule.extension.socket.api.exceptions.ReadingTimeoutException;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.extension.socket.api.socket.udp.UdpSocketProperties;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;

/**
 * An extension for sending and receiving connections through both TCP and UDP protocols.
 *
 * @since 4.0
 */
@Extension(name = "Sockets")
@Configurations({ListenerConfig.class, RequesterConfig.class})
@SubTypeMapping(baseType = TcpProtocol.class, subTypes = {SafeProtocol.class, DirectProtocol.class, LengthProtocol.class,
    StreamingProtocol.class, XmlMessageProtocol.class, XmlMessageEOFProtocol.class, CustomProtocol.class, EOFProtocol.class})
@Export(classes = {TcpClientSocketProperties.class, TcpServerSocketProperties.class, UdpSocketProperties.class,
    ReadingTimeoutException.class, LengthExceededException.class, LengthProtocol.class, AbstractByteProtocol.class})
public class SocketsExtension {

  public static final String TLS_CONFIGURATION = "TLS Configuration";
  public static final String TLS = "TLS";
}
