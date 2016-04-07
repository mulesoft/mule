/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.subtypes;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.extension.api.annotation.Parameter;

public class SubtypesConnectionProvider implements ConnectionProvider<SubTypesMappingConnector, SubtypesConnectorConnection>
{

    @Parameter
    private Shape abstractShape;

    @Parameter
    private Door doorInterface;


    @Override
    public SubtypesConnectorConnection connect(SubTypesMappingConnector subtypesPojoConnector) throws ConnectionException
    {
        return new SubtypesConnectorConnection(abstractShape, doorInterface);
    }

    @Override
    public void disconnect(SubtypesConnectorConnection subtypesConnectorConnection)
    {
    }

    @Override
    public ConnectionValidationResult validate(SubtypesConnectorConnection subtypesConnectorConnection)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<SubtypesConnectorConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<SubTypesMappingConnector, SubtypesConnectorConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }
}
