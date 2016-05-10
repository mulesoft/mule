/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever;

import static java.lang.String.format;
import static org.mule.runtime.api.connection.ConnectionExceptionCode.CREDENTIALS_EXPIRED;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.email.api.AbstractEmailConnection;
import org.mule.extension.email.internal.exception.EmailConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection with a mail server for retrieving emails from an specific folder.
 *
 * @since 4.0
 */
public class RetrieverConnection extends AbstractEmailConnection
{

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieverConnection.class);

    private final Store store;

    /**
     * Creates a new instance of the of the {@link RetrieverConnection} secured by TLS.
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
    public RetrieverConnection(String protocol,
                               String username,
                               String password,
                               String host,
                               String port,
                               long connectionTimeout,
                               long readTimeout,
                               long writeTimeout,
                               Map<String, String> properties,
                               TlsContextFactory tlsContextFactory) throws EmailConnectionException
    {
        super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, tlsContextFactory);
        try
        {
            this.store = session.getStore(protocol);
            this.store.connect(username, password);
        }
        catch (MessagingException e)
        {
            throw new EmailConnectionException(format("Error while acquiring connection with the %s store", protocol), e);
        }
    }

    /**
     * Creates a new instance of the of the {@link RetrieverConnection}.
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
    public RetrieverConnection(String protocol,
                               String username,
                               String password,
                               String host,
                               String port,
                               long connectionTimeout,
                               long readTimeout,
                               long writeTimeout,
                               Map<String, String> properties) throws EmailConnectionException
    {
        this(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void disconnect()
    {
        try
        {
            store.close();
        }
        catch (Exception e)
        {
            LOGGER.error(format("Error while disconnecting: %s", e.getMessage()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate()
    {
        String errorMessage = "Store is not connected";
        return store.isConnected() ? success()
                                   : failure(errorMessage, CREDENTIALS_EXPIRED, new EmailConnectionException(errorMessage));
    }

    /**
     * @return The configured connection {@link Store}
     */
    public Store getStore()
    {
        return store;
    }
}
