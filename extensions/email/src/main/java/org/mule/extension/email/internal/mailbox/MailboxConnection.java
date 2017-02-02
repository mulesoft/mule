/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import static java.lang.String.format;
import static org.mule.extension.email.api.exception.EmailError.CONNECTION_TIMEOUT;
import static org.mule.extension.email.api.exception.EmailError.DISCONNECTED;
import static org.mule.extension.email.api.exception.EmailError.INVALID_CREDENTIALS;
import static org.mule.extension.email.api.exception.EmailError.UNKNOWN_HOST;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.email.api.exception.EmailAccessingFolderException;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.internal.AbstractEmailConnection;
import org.mule.extension.email.internal.EmailProtocol;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * A connection with a mail server for retrieving and managing emails from an specific folder in a mailbox.
 *
 * @since 4.0
 */
public class MailboxConnection extends AbstractEmailConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailboxConnection.class);
  private static final String PORT_OUT_OF_RANGE = "port out of range";

  private Store store;
  private Folder folder;

  /**
   * Creates a new instance of the of the {@link MailboxConnection} secured by TLS.
   *
   * @param protocol          the desired protocol to use. One of imap or pop3 (and its secure versions)
   * @param username          the username to establish the connection.
   * @param password          the password corresponding to the {@code username}.
   * @param host              the host name of the mail server
   * @param port              the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout       the socket read timeout
   * @param writeTimeout      the socket write timeout
   * @param properties        additional custom properties.
   * @param tlsContextFactory the tls context factory for creating the context to secure the connection
   */
  public MailboxConnection(EmailProtocol protocol, String username, String password, String host, String port,
                           long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties,
                           TlsContextFactory tlsContextFactory)
      throws EmailConnectionException {
    super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, tlsContextFactory);
    String format = format("Error while acquiring connection with the %s store", protocol);

    try {
      this.store = session.getStore(protocol.getName());

      if (username != null && password != null) {
        this.store.connect(username, password);
      } else {
        this.store.connect();
      }
    } catch (AuthenticationFailedException e) {
      throw new EmailConnectionException(format, e, INVALID_CREDENTIALS);
    } catch (MessagingException e) {
      handleEmailMessagingException(format, e);
    } catch (IllegalArgumentException e) {
      handleIllegalArgumentException(format, e);
    } catch (Exception e) {
      throw new EmailConnectionException(format, e);
    }
  }

  /**
   * Creates a new instance of the of the {@link MailboxConnection}.
   *
   * @param protocol          the desired protocol to use. One of imap or pop3 (and its secure versions)
   * @param username          the username to establish the connection.
   * @param password          the password corresponding to the {@code username}.
   * @param host              the host name of the mail server
   * @param port              the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout       the socket read timeout
   * @param writeTimeout      the socket write timeout
   * @param properties        additional custom properties.
   */
  public MailboxConnection(EmailProtocol protocol, String username, String password, String host, String port,
                           long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties)
      throws EmailConnectionException {
    this(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, null);
  }


  /**
   * Opens and return the email {@link Folder} of name {@code mailBoxFolder}. The folder can contain Messages, other Folders or
   * both.
   * <p>
   * If there was an already opened folder and a different one is requested the opened folder will be closed and the new one will
   * be opened.
   *
   * @param mailBoxFolder the name of the folder to be opened.
   * @param openMode      open the folder in READ_ONLY or READ_WRITE mode
   * @return the opened {@link Folder}
   */
  public synchronized Folder getFolder(String mailBoxFolder, int openMode) {
    try {
      if (folder != null) {
        if (isCurrentFolder(mailBoxFolder) && folder.isOpen() && folder.getMode() == openMode) {
          return folder;
        }
        closeFolder(false);
      }

      folder = store.getFolder(mailBoxFolder);
      folder.open(openMode);
      return folder;
    } catch (MessagingException e) {
      throw new EmailAccessingFolderException(format("Error while opening folder %s", mailBoxFolder), e);
    }
  }

  /**
   * Closes the current connection folder.
   *
   * @param expunge whether to remove all the emails marked as DELETED.
   */
  public synchronized void closeFolder(boolean expunge) {
    try {
      if (folder != null && folder.isOpen()) {
        folder.close(expunge);
      }
    } catch (MessagingException e) {
      throw new EmailAccessingFolderException(format("Error while closing mailbox folder %s", folder.getName()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void disconnect() {
    try {
      closeFolder(false);
    } catch (Exception e) {
      LOGGER.error(format("Error closing mailbox folder [%s] when disconnecting: %s", folder.getName(), e.getMessage()));
    } finally {
      try {
        store.close();
      } catch (Exception e) {
        LOGGER.error(format("Error closing store when disconnecting: %s", e.getMessage()));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate() {
    String errorMessage = "Store is not connected";
    return store.isConnected() ? success() : failure(errorMessage, new EmailConnectionException(errorMessage, DISCONNECTED));
  }

  /**
   * Checks if a mailBoxFolder name is the same name as the current folder.
   *
   * @param mailBoxFolder the name of the folder
   * @return true if is the same folder, false otherwise.
   */
  private boolean isCurrentFolder(String mailBoxFolder) {
    return folder.getName().equalsIgnoreCase(mailBoxFolder);
  }

  private void handleIllegalArgumentException(String format, IllegalArgumentException e) throws EmailConnectionException {
    if (e.getMessage().contains(PORT_OUT_OF_RANGE)) {
      throw new EmailConnectionException(format, e, UNKNOWN_HOST);
    }
    throw new EmailConnectionException(format, e);
  }

  private void handleEmailMessagingException(String format, MessagingException e) throws EmailConnectionException {
    if (e.getCause() instanceof SocketTimeoutException) {
      throw new EmailConnectionException(format, e, CONNECTION_TIMEOUT);
    }
    if (e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException) {
      throw new EmailConnectionException(format, e, UNKNOWN_HOST);
    }
    throw new EmailConnectionException(format, e);
  }
}
