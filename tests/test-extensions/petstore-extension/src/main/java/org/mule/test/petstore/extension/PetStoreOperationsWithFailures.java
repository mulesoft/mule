/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.annotation.param.Connection;

import java.util.ArrayList;
import java.util.List;

public class PetStoreOperationsWithFailures extends PetStoreOperations {

  public static final String CONNECTION_FAIL = "Connection fail";

  /**
   * The threads on which a connection was executed.
   */
  private static List<Thread> connectionThreads = new ArrayList<>();

  @Ignore
  public static List<Thread> getConnectionThreads() {
    return unmodifiableList(connectionThreads);
  }

  public static void resetConnectionThreads() {
    connectionThreads.clear();
  }

  public Integer failConnection(@Connection PetStoreClient client) throws ConnectionException {
    connectionThreads.add(currentThread());
    throw new ConnectionException(CONNECTION_FAIL);
  }

  public Integer failOperationWithException(@Connection PetStoreClient client) throws Exception {
    connectionThreads.add(currentThread());
    throw new Exception(CONNECTION_FAIL);
  }

  public Integer failOperationWithThrowable(@Connection PetStoreClient client) throws Throwable {
    connectionThreads.add(currentThread());
    throw new Throwable(CONNECTION_FAIL);
  }
}
