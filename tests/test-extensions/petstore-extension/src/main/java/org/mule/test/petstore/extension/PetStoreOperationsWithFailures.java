/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.petstore.extension;

import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PetStoreOperationsWithFailures extends PetStoreOperations {

  public static final String CONNECTION_FAIL = "Connection fail";

  /**
   * The threads on which a connection was executed.
   */
  private static List<Thread> connectionThreads = new ArrayList<>();

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

  public PagingProvider<PetStoreClient, Integer> failPagedOperation(int failOnPage, @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false") boolean stickyConnections) {
    return new PagingProvider<PetStoreClient, Integer>() {

      private int index = 0;
      private List<Integer> returnValue = Collections.singletonList(1);

      @Override
      public List<Integer> getPage(PetStoreClient petStoreClient) {
        if (index == failOnPage) {
          connectionThreads.add(currentThread());
          throw new ModuleException(CONNECTIVITY, new ConnectionException(CONNECTION_FAIL));
        }
        index++;
        return returnValue;
      }

      @Override
      public Optional<Integer> getTotalResults(PetStoreClient petStoreClient) {
        return Optional.empty();
      }

      @Override
      public void close(PetStoreClient petStoreClient) throws MuleException {

      }

      @Override
      public boolean useStickyConnections() {
        return stickyConnections;
      }
    };
  }
}
