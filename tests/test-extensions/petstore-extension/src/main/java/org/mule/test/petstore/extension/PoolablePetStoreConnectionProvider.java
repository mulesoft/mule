/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("poolable")
public class PoolablePetStoreConnectionProvider extends PetStoreConnectionProvider
{

    @Override
    public ConnectionHandlingStrategy<PetStoreClient> getHandlingStrategy(ConnectionHandlingStrategyFactory handlingStrategyFactory)
    {
        return handlingStrategyFactory.supportsPooling();
    }
}
