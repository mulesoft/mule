/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("valid")
public class PooledPetStoreConnectionProviderWithValidConnection extends PetStoreConnectionProvider<PetStoreClient>
    implements ConnectionProvider<PetStoreClient> {

}
