/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static org.mule.test.petstore.extension.PetStoreOperationsWithFailures.CONNECTION_FAIL;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("invalid")
public class PooledPetStoreConnectionProviderWithFailureInvalidConnection
    extends PetStoreConnectionProvider<PetStoreClient> implements ConnectionProvider<PetStoreClient> {

  @Override
  public ConnectionValidationResult validate(PetStoreClient connection) {
    return ConnectionValidationResult.failure(CONNECTION_FAIL, new Exception("Invalid credentials"));
  }
}
