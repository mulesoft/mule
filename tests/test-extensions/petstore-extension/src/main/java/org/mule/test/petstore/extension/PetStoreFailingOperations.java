/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionException;

import java.util.Map;

public class PetStoreFailingOperations extends PetStoreOperations {

  public String fail(Map<String, String> petNames) throws Exception {
    throw new ConnectionException("Failed operation");
  }

}
