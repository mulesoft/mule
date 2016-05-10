/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever;

import org.mule.extension.email.api.AbstractEmailConfiguration;
import org.mule.extension.email.api.AbstractEmailConnection;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

/**
 * Generic contract for all email retriever {@link ConfigurationProvider}s.
 *
 * @since 4.0
 */
public abstract class AbstractRetrieverProvider<Config extends AbstractEmailConfiguration, Connection extends AbstractEmailConnection> implements ConnectionProvider<Config, Connection>
{

    /**
     * the username used to connect with the mail server.
     */
    @Parameter
    protected String user;

    /**
     * the corresponding password for the {@code username}.
     */
    @Parameter
    @Password
    protected String password;


    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(Connection connection)
    {
        connection.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate(Connection connection)
    {
        return connection.validate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, Connection> connectionHandlingStrategyFactory)
    {
        return connectionHandlingStrategyFactory.supportsPooling();
    }
}