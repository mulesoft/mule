/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.util.Map;

public class PetStoreFailingOperations extends PetStoreOperations {

  @MediaType(TEXT_PLAIN)
  public String fail(Map<String, String> petNames) throws Exception {
    throw new ConnectionException("Failed operation");
  }

}
