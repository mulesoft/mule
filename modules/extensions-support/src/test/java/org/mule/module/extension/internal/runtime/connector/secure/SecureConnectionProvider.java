/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.secure;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.display.Password;
import org.mule.extension.annotation.api.param.display.Text;

public class SecureConnectionProvider implements ConnectionProvider<SecureConnector, Object>
{

    @Parameter
    @Password
    private String providerPassword;

    @Parameter
    @Text
    private String anotherLongText;

    @Override
    public Object connect(SecureConnector secureConnector) throws ConnectionException
    {
        return null;
    }

    @Override
    public void disconnect(Object o)
    {

    }

    @Override
    public ConnectionValidationResult validate(Object o)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<Object> getHandlingStrategy(ConnectionHandlingStrategyFactory<SecureConnector, Object> connectionHandlingStrategyFactory)
    {
        return null;
    }
}
