/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static org.apache.commons.lang.StringUtils.join;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic implementation for an email connection of a connector which operates over the SMTP, IMAP, POP3 and it's secure versions
 * protocols.
 * <p>
 * Performs the creation of a persistent set of properties that are used to configure the {@link Session} instance.
 *
 * @since 4.0
 */
public abstract class AbstractEmailConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEmailConnection.class);

  public static final String PASSWORD_NO_USERNAME_ERROR = "Password provided but not username was specified.";
  public static final String USERNAME_NO_PASSWORD_ERROR = "Username provided but not password was specified.";

  /**
   * A separator used to separate some tokens from a property that accepts multiple values, e.g.: mail.{name}.ciphersuites
   */
  private static final String WHITESPACE_SEPARATOR = " ";

  protected final EmailProtocol protocol;
  protected final Session session;

  /**
   * Base constructor for {@link AbstractEmailConnection} implementations.
   *
   * @param protocol the protocol used to send mails.
   * @param username the username to establish connection with the mail server.
   * @param password the password corresponding to the {@code username}
   * @param host the host name of the mail server.
   * @param port the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout the socket read timeout
   * @param writeTimeout the socket write timeout
   * @param properties the custom properties added to configure the session.
   */
  public AbstractEmailConnection(EmailProtocol protocol, String username, String password, String host, String port,
                                 long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties)
      throws EmailConnectionException {
    this(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, null);
  }


  /**
   * Base constructor for {@link AbstractEmailConnection} implementations that aims to be secured by TLS.
   *
   * @param protocol the protocol used to send mails.
   * @param username the username to establish connection with the mail server.
   * @param password the password corresponding to the {@code username}
   * @param host the host name of the mail server.
   * @param port the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout the socket read timeout
   * @param writeTimeout the socket write timeout
   * @param properties the custom properties added to configure the session.
   * @param tlsContextFactory the tls context factory for creating the context to secure the connection
   */
  public AbstractEmailConnection(EmailProtocol protocol, String username, String password, String host, String port,
                                 long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties,
                                 TlsContextFactory tlsContextFactory)
      throws EmailConnectionException {
    this.protocol = protocol;
    Properties sessionProperties = buildBasicSessionProperties(host, port, connectionTimeout, readTimeout, writeTimeout);

    if (protocol.isSecure()) {
      sessionProperties.putAll(buildSecureProperties(tlsContextFactory));
    }

    if (properties != null) {
      sessionProperties.putAll(properties);
    }

    PasswordAuthenticator authenticator = null;
    if (shouldAuthenticate(username, password)) {
      sessionProperties.setProperty(protocol.getMailAuthProperty(), "true");
      authenticator = new PasswordAuthenticator(username, password);
    }

    session = Session.getInstance(sessionProperties, authenticator);
  }


  /**
   * Creates a new {@link Properties} instance and set all the basic properties required by the specified {@code protocol}.
   */
  private Properties buildBasicSessionProperties(String host, String port,
                                                 long connectionTimeout,
                                                 long readTimeout,
                                                 long writeTimeout)
      throws EmailConnectionException {
    Properties props = new Properties();
    props.setProperty(protocol.getPortProperty(), port);
    props.setProperty(protocol.getHostProperty(), host);
    props.setProperty(protocol.getReadTimeoutProperty(), Long.toString(readTimeout));
    props.setProperty(protocol.getConnectionTimeoutProperty(), Long.toString(connectionTimeout));
    props.setProperty(protocol.getWriteTimeoutProperty(), Long.toString(writeTimeout));
    props.setProperty(protocol.getTransportProtocolProperty(), protocol.getName());
    return props;
  }

  /**
   * Creates a new {@link Properties} instance and set all the secure properties required by the specified secure
   * {@code protocol}.
   */
  private Properties buildSecureProperties(TlsContextFactory tlsContextFactory) throws EmailConnectionException {
    Properties properties = new Properties();
    properties.setProperty(protocol.getStartTlsProperty(), "true");
    properties.setProperty(protocol.getSocketFactoryFallbackProperty(), "false");

    if (tlsContextFactory.getTrustStoreConfiguration().isInsecure()) {
      properties.setProperty(protocol.getSslTrustProperty(), "*");
    }

    String[] cipherSuites = tlsContextFactory.getEnabledCipherSuites();
    if (cipherSuites != null) {
      properties.setProperty(protocol.getSslCiphersuitesProperty(), join(cipherSuites, WHITESPACE_SEPARATOR));
    }

    String[] sslProtocols = tlsContextFactory.getEnabledProtocols();
    if (sslProtocols != null) {

      Stream.of(sslProtocols).filter(p -> p.startsWith("SSL")).findAny()
          .ifPresent(p -> {
            properties.setProperty(protocol.getSslEnableProperty(), "true");
            if (LOGGER.isWarnEnabled()) {
              LOGGER.warn(
                          "Property %s has been enabled. For disabling this property remove SSL from the enabled protocols in your TLS configuration",
                          protocol.getSslEnableProperty());
            }
          });
      properties.setProperty(protocol.getSslProtocolsProperty(), join(sslProtocols, WHITESPACE_SEPARATOR));
    }

    try {
      SSLContext sslContext = tlsContextFactory.createSslContext();
      properties.put(protocol.getSocketFactoryProperty(), sslContext.getSocketFactory());
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      throw new EmailConnectionException("Failed when creating SSL context.");
    }

    return properties;
  }

  /**
   * @return the email {@link Session} used by the connection.
   */
  public Session getSession() {
    return session;
  }

  /**
   * disconnects the internal client used by the {@link AbstractEmailConnection} instance.
   */
  public abstract void disconnect();

  /**
   * Checks if the current {@link AbstractEmailConnection} instance is valid or not.
   *
   * @return a {@link ConnectionValidationResult} indicating if the connection is valid or not.
   */
  public abstract ConnectionValidationResult validate();

  /**
   * Checks the consistency of the username and password parameters and returns whether we should authenticate with the server or
   * not.
   *
   * @param username the specified username.
   * @param password the specified password.
   */
  private boolean shouldAuthenticate(String username, String password) {
    if (username == null && password != null) {
      throw new EmailException(PASSWORD_NO_USERNAME_ERROR);
    }
    if (username != null && password == null) {
      throw new EmailException(USERNAME_NO_PASSWORD_ERROR);
    }
    return username != null;
  }

  /**
   * An {@link Authenticator} object that knows how to obtain authentication for a network connection using username and password.
   *
   * @since 4.0
   */
  private final class PasswordAuthenticator extends Authenticator {

    private String user;
    private String pass;

    /**
     * Creates a new instance.
     *
     * @param user the username to establish connection to.
     * @param pass the password for the specified {@code username}.
     */
    public PasswordAuthenticator(String user, String pass) {
      this.user = user;
      this.pass = pass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(user, pass);
    }
  }
}
