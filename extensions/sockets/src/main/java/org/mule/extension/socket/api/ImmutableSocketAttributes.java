/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api;

import static org.apache.commons.lang.StringUtils.EMPTY;

import org.mule.runtime.core.message.BaseAttributes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable implementation of {@link SocketAttributes}.
 *
 * @since 4.0
 */
public class ImmutableSocketAttributes extends BaseAttributes implements SocketAttributes {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImmutableSocketAttributes.class);
  private int port;
  private String hostAddress;
  private String hostName;
  private Certificate[] localCertificates;
  private Certificate[] peerCertificates;

  /**
   * Creates a new instance
   *
   * @param socket TCP {@link Socket} connection with the remote host
   */
  public ImmutableSocketAttributes(Socket socket) {
    fromInetAddress(socket.getPort(), socket.getInetAddress());

    if (socket instanceof SSLSocket) {
      try {
        SSLSocket sslSocket = (SSLSocket) socket;
        // getSession tries to set up a session if there is no currently valid session, and an implicit handshake is done
        SSLSession sslSession = sslSocket.getSession();
        localCertificates = sslSession.getLocalCertificates();
        peerCertificates = sslSession.getPeerCertificates();
      } catch (SSLPeerUnverifiedException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Error obtaining SSLSocket attributes", e);
        }
      }
    }

  }


  /**
   * Creates a new instance
   *
   * @param socket UDP {@link DatagramSocket} connection with the remote host
   */
  public ImmutableSocketAttributes(DatagramSocket socket) {
    fromInetAddress(socket.getPort(), socket.getInetAddress());
  }

  /**
   * Creates a new instance
   *
   * @param packet UDP {@link DatagramPacket} received from remote host
   */
  public ImmutableSocketAttributes(DatagramPacket packet) {
    this(packet.getPort(), packet.getAddress().getHostAddress(), packet.getAddress().getHostName());
  }

  private void fromInetAddress(int port, InetAddress address) {
    this.port = port;

    if (address == null) {
      this.hostName = EMPTY;
      this.hostAddress = EMPTY;
    } else {
      this.hostName = address.getHostName();
      this.hostAddress = address.getHostAddress();
    }
  }

  public ImmutableSocketAttributes(int remotePort, String remoteHostAddress, String remoteHostName) {
    this.port = remotePort;
    this.hostAddress = remoteHostAddress;
    this.hostName = remoteHostName;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int getPort() {
    return port;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHostAddress() {
    return hostAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHostName() {
    return hostName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Certificate[] getLocalCertificates() {
    return localCertificates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Certificate[] getPeerCertificates() {
    return peerCertificates;
  }
}
