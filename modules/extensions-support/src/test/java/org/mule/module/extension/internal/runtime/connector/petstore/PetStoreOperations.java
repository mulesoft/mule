/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.UseConfig;

import java.util.List;

public class PetStoreOperations
{

    @Operation
    public List<String> getPets(@Connection PetStoreClient client, @UseConfig PetStoreConnector config, String ownerName)
    {
        return client.getPets(ownerName, config);
    }

    @Operation
    public PetStoreClient getClient(@Connection PetStoreClient client)
    {
        return client;
    }
}
