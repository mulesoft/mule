/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("valid")
public class PooledPetStoreConnectionProviderWithValidConnection extends PetStoreConnectionProvider<PetStoreClient>
    implements ConnectionProvider<PetStoreClient> {

}
