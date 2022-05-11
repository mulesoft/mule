/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
