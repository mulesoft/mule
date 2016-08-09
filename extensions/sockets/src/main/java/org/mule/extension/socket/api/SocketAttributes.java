/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.socket.api;

import org.mule.runtime.api.message.Attributes;

import java.security.cert.Certificate;

/**
 * Canonical representation a connection's metadata attributes.
 * <p>
 * It contains information such as a the port from which the sending endpoint is writing information, its host name and address.
 *
 * @since 4.0
 */
public interface SocketAttributes extends Attributes {

  /**
   * @return the port number from which the sender is bounded.
   */
  int getPort();

  /**
   * @return the host address of the sender
   */
  String getHostAddress();

  /**
   * @return the host name of the sender
   */
  String getHostName();


  /**
   * @return the certificate(s) that were sent to the peer during the SSL handshaking.
   */
  Certificate[] getLocalCertificates();

  /**
   * @return the identity of the peer which was established as part of defining the session. Note: This method can be used only
   *         when using certificate-based cipher suites; using it with non-certificate-based cipher suites, such as Kerberos, will
   *         be return {@code null}
   */
  Certificate[] getPeerCertificates();
}
