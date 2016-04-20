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
import org.mule.tck.testmodels.fruit.Banana;

@Alias("banana")
public class VeganBananaConnectionProvider implements ConnectionProvider<BananaConfig, Banana>
{

    @Override
    public Banana connect(BananaConfig config) throws ConnectionException
    {
        return new Banana();
    }

    @Override
    public void disconnect(Banana banana)
    {

    }

    @Override
    public ConnectionValidationResult validate(Banana banana)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<Banana> getHandlingStrategy(ConnectionHandlingStrategyFactory<BananaConfig, Banana> handlingStrategyFactory)
    {
        return handlingStrategyFactory.none();
    }
}
