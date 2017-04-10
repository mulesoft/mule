/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.param.Connection;

public class PetStoreOperationsWithFailures extends PetStoreOperations {

  public static final String CONNECTION_FAIL = "Connection fail";

  public Integer failConnection(@Connection PetStoreClient client) throws ConnectionException {
    throw new ConnectionException(CONNECTION_FAIL);
  }

  public Integer failOperationWithException(@Connection PetStoreClient client) throws Exception {
    throw new Exception(CONNECTION_FAIL);
  }

  public Integer failOperationWithThrowable(@Connection PetStoreClient client) throws Throwable {
    throw new Throwable(CONNECTION_FAIL);
  }
}
