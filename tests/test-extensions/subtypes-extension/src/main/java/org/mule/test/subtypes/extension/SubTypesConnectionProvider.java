/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;


import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Parameter;

public class SubTypesConnectionProvider implements ConnectionProvider<SubTypesMappingConnector, SubTypesConnectorConnection>
{

    @Parameter
    private Shape abstractShape;

    @Parameter
    private Door doorInterface;


    @Override
    public SubTypesConnectorConnection connect(SubTypesMappingConnector subtypesPojoConnector) throws ConnectionException
    {
        return new SubTypesConnectorConnection(abstractShape, doorInterface);
    }

    @Override
    public void disconnect(SubTypesConnectorConnection subtypesConnectorConnection)
    {
    }

    @Override
    public ConnectionValidationResult validate(SubTypesConnectorConnection subtypesConnectorConnection)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<SubTypesConnectorConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<SubTypesMappingConnector, SubTypesConnectorConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }
}
