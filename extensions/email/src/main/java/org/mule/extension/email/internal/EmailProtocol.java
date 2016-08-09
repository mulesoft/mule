/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static java.lang.String.format;

import java.net.Socket;

/**
 * Email Protocols supported by the {@link EmailConnector}.
 * <p>
 * Each protocol have a set of properties that need to be configured to establish a connection with a mail server.
 * <p>
 * The full list of properties available for protocols can be found at:
 * <ul>
 * <li>for POP3 <a>https://javamail.java.net/nonav/docs/api/com/sun/mail/pop3/package-summary.html#properties</a></li>
 * <li>for SMTP <a>https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html#properties</a></li>
 * <li>for IMAP <a>https://javamail.java.net/nonav/docs/api/com/sun/mail/imap/package-summary.html#properties</a></li>
 * <ul/>
 *
 * @since 4.0
 */
public enum EmailProtocol {

  /**
   * represents the Simple Mail Transfer Protocol.
   */
  SMTP("smtp", false),

  /**
   * represents the secured Simple Mail Transfer Protocol.
   */
  SMTPS("smtp", true),

  /**
   * represents the Internet Message Access Protocol.
   */
  IMAP("imap", false),

  /**
   * represents the secured Internet Message Access Protocol.
   */
  IMAPS("imap", true),

  /**
   * represents the Post Office Protocol.
   */
  POP3("pop3", false),

  /**
   * represents the secured Post Office Protocol.
   */
  POP3S("pop3", true);

  private final String name;
  private final boolean secure;

  /**
   * Creates an instance.
   *
   * @param name the name of the protocol.
   */
  EmailProtocol(String name, boolean secure) {
    this.name = name;
    this.secure = secure;
  }

  /**
   * The name of the protocol.
   *
   * @return the name of the protocol.
   */
  public String getName() {
    return name;
  }

  /**
   * If the protocol is secure or not.
   *
   * @return whether a protocol is secured by SSL/TLS or not.
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * The host name of the mail server.
   *
   * @return the protocol host name property.
   */
  public String getHostProperty() {
    return unmaskProperty("mail.%s.host");
  }

  /**
   * The port number of the mail server.
   *
   * @return the protocol port property.
   */
  public String getPortProperty() {
    return unmaskProperty("mail.%s.port");
  }

  /**
   * Indicates if should attempt to authorize or not. Defaults to false.
   *
   * @return the protocol mail auth property.
   */
  public String getMailAuthProperty() {
    return unmaskProperty("mail.%s.auth");
  }

  /**
   * Defines the default mime charset to use when none has been specified for the message.
   *
   * @return the mime charset property.
   */
  public String getMailMimeCharsetProperty() {
    return unmaskProperty("mail.mime.charset");
  }

  /**
   * Whether to use {@link Socket} as a fallback if the initial connection fails or not.
   *
   * @return the protocol socket factory fallback property.
   */
  public String getSocketFactoryFallbackProperty() {
    return unmaskProperty("mail.%s.socketFactory.fallback");
  }

  /**
   * Specifies the port to connect to when using a socket factory.
   *
   * @return the protocol socket factory port property.
   */
  public String getSocketFactoryPortProperty() {
    return unmaskProperty("mail.%s.socketFactory.port");
  }

  /**
   * @return the protocol socket factory property.
   */
  public String getSocketFactoryProperty() {
    return unmaskProperty("mail.%s.ssl.socketFactory");
  }

  /**
   * Specifies the SSL ciphersuites that will be enabled for SSL connections.
   *
   * @return the protocol ssl ciphersuites property.
   */
  public String getSslCiphersuitesProperty() {
    return unmaskProperty("mail.%s.ssl.ciphersuites");
  }

  /**
   * Specifies the SSL protocols that will be enabled for SSL connections.
   *
   * @return the protocol ssl enabled protocols property.
   */
  public String getSslProtocolsProperty() {
    return unmaskProperty("mail.%s.ssl.protocols");
  }

  /**
   * Specifies if ssl is enabled or not.
   *
   * @return the ssl enable property.
   */
  public String getSslEnableProperty() {
    return unmaskProperty("mail.%s.ssl.enable");
  }

  /**
   * Specifies the trusted hosts.
   *
   * @return the protocol ssl trust property.
   */
  public String getSslTrustProperty() {
    return unmaskProperty("mail.%s.ssl.trust");
  }

  /**
   * Indicates if the STARTTLS command shall be used to initiate a TLS-secured connection.
   *
   * @return the protocol start tls property.
   */
  public String getStartTlsProperty() {
    return unmaskProperty("mail.%s.starttls.enable");
  }

  /**
   * Specifies the default transport name.
   *
   * @return the protocol name property.
   */
  public String getTransportProtocolProperty() {
    return "mail.transport.name";
  }

  /**
   * Socket read timeout value in milliseconds. Default is infinite timeout.
   *
   * @return the protocol read timeout property.
   */
  public String getReadTimeoutProperty() {
    return unmaskProperty("mail.%s.timeout");
  }

  /**
   * Socket connection timeout value in milliseconds. Default is infinite timeout.
   *
   * @return the protocol connection timeout property.
   */
  public String getConnectionTimeoutProperty() {
    return unmaskProperty("mail.%s.connectiontimeout");
  }

  /**
   * Socket write timeout value in milliseconds. Default is infinite timeout.
   *
   * @return the protocol write timeout property.
   */
  public String getWriteTimeoutProperty() {
    return unmaskProperty("mail.%s.writetimeout");
  }

  private String unmaskProperty(String property) {
    return format(property, name);
  }
}
