/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import org.mule.extension.email.api.AbstractEmailConnectionProvider;
import org.mule.extension.email.api.EmailConnectionSettings;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.ParameterGroup;

public abstract class AbstractSenderProvider extends AbstractEmailConnectionProvider<SenderConnection>
{

    /**
     * A basic set of parameters for email connections.
     */
    @ParameterGroup
    protected EmailConnectionSettings settings;

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(SenderConnection connection)
    {
        connection.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate(SenderConnection connection)
    {
        return connection.validate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<SenderConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<SenderConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }
}
