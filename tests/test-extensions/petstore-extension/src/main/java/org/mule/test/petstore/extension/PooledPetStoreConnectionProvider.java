/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.test.petstore.extension.stereotype.CustomPetstoreConnectionStereotype;

@Alias("pooled")
@Stereotype(CustomPetstoreConnectionStereotype.class)
public class PooledPetStoreConnectionProvider extends PetStoreConnectionProvider<PetStoreClient>
    implements PoolingConnectionProvider<PetStoreClient> {

  public static int TIMES_CONNECTED = 0;

  @Optional
  @Parameter
  protected boolean forceConnectionError = false;

  @Override
  public PetStoreClient connect() throws ConnectionException {
    if (forceConnectionError) {
      throw new ConnectionException("Connection failure", new Exception());
    }
    TIMES_CONNECTED++;
    return super.connect();
  }
}
