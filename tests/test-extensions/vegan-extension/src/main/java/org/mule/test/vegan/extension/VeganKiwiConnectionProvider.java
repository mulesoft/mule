/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.tck.testmodels.fruit.Kiwi;

@Alias("kiwi")
public class VeganKiwiConnectionProvider implements ConnectionProvider<KiwiConfig, Kiwi>
{

    @Override
    public Kiwi connect(KiwiConfig config) throws ConnectionException
    {
        return new Kiwi();
    }

    @Override
    public void disconnect(Kiwi kiwi)
    {

    }

    @Override
    public ConnectionValidationResult validate(Kiwi kiwi)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<Kiwi> getHandlingStrategy(ConnectionHandlingStrategyFactory<KiwiConfig, Kiwi> handlingStrategyFactory)
    {
        return handlingStrategyFactory.none();
    }
}
