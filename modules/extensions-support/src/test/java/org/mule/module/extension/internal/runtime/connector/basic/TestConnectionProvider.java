/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.basic;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.extension.annotation.api.Expression;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.introspection.ExpressionSupport;

public class TestConnectionProvider implements ConnectionProvider<TestConnector, Object>
{

    @Parameter
    private String connectionProviderRequiredFieldDefault;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    private String connectionProviderRequiredFieldExpressionSupported;


    @Parameter
    @Expression(ExpressionSupport.REQUIRED)
    private String connectionProviderRequiredFieldExpressionRequired;

    @Parameter
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    private String connectionProviderRequiredFieldExpressionNotSupported;

    @Parameter
    @Optional
    private String connectionProviderOptionalFieldDefault;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional
    private String connectionProviderOptionalFieldExpressionSupported;

    @Parameter
    @Expression(ExpressionSupport.REQUIRED)
    @Optional
    private String connectionProviderOptionalFieldExpressionRequired;

    @Parameter
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    @Optional
    private String connectionProviderOptionalFieldExpressionNotSupported;

    @Override
    public Object connect(TestConnector testConnector) throws ConnectionException
    {
        return new Object();
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
    public ConnectionHandlingStrategy<Object> getHandlingStrategy(ConnectionHandlingStrategyFactory<TestConnector, Object> connectionHandlingStrategyFactory)
    {
        return connectionHandlingStrategyFactory.none();
    }
}
