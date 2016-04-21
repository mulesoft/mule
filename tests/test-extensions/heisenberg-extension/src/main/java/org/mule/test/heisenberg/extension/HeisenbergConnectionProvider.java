/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;


public class HeisenbergConnectionProvider implements ConnectionProvider<HeisenbergExtension, HeisenbergConnection>
{

    public static final String SAUL_OFFICE_NUMBER = "505-503-4455";

    @Parameter
    @Optional(defaultValue = SAUL_OFFICE_NUMBER)
    private String saulPhoneNumber;

    @Parameter
    @Optional
    private TlsContextFactory tlsContextFactory;

    @Override
    public HeisenbergConnection connect(HeisenbergExtension heisenbergExtension) throws ConnectionException
    {
        return new HeisenbergConnection(saulPhoneNumber);
    }

    @Override
    public void disconnect(HeisenbergConnection heisenbergConnection)
    {

    }

    @Override
    public ConnectionValidationResult validate(HeisenbergConnection heisenbergConnection)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<HeisenbergConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<HeisenbergExtension, HeisenbergConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }
}
