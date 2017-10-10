/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.petstore.extension;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.ExecutorService;

@Alias("source")
@MediaType(TEXT_PLAIN)
public class FailingPetStoreSource extends Source<String, Object> {

  @Config
  PetStoreConnector config;

  @Connection
  private ConnectionProvider<PetStoreClient> connectionProvider;

  @Parameter
  @Optional(defaultValue = "false")
  boolean failOnStart;

  @Parameter
  @Optional(defaultValue = "false")
  boolean failOnException;

  public static boolean failedDueOnException = false;
  public static ConnectionException connectionException = new ConnectionException("ERROR");
  public static ExecutorService executor;

  @Override
  public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    PetStoreConnector.timesStarted++;

    if (failOnStart || failedDueOnException) {
      throw new RuntimeException(connectionException);
    }

    if (failOnException) {
      failedDueOnException = true;
      executor = newSingleThreadExecutor();
      executor.execute(() -> sourceCallback.onConnectionException(connectionException));
    }
  }

  @Override
  public void onStop() {}
}
