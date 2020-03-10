/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.implicit.config.extension.extension.internal;

import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.test.implicit.config.extension.extension.api.Counter;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;

import java.util.Timer;
import java.util.TimerTask;

public class ImplicitOperations {

  public ImplicitConfigExtension getConfig(@Config ImplicitConfigExtension config) {
    return config;
  }

  public Counter getConnection(@Connection Counter connection) {
    return connection;
  }

  private static Timer timer = new Timer();

  /**
   * Test async operation
   */
  @MediaType(TEXT_PLAIN)
  public void simpleAsyncOperation(CompletionCallback<String, Void> completionCallback) {
    timer.schedule(
                   new TimerTask() {

                     @Override
                     public void run() {
                       completionCallback.success(Result.<String, Void>builder().output("async!").build());
                     }
                   }, 5000);
  }

  /**
   * Test async operation
   */
  @Execution(BLOCKING)
  @MediaType(TEXT_PLAIN)
  public void blockingAsyncOperation(CompletionCallback<String, Void> completionCallback) {
    timer.schedule(
                   new TimerTask() {

                     @Override
                     public void run() {
                       completionCallback.success(Result.<String, Void>builder().output("blocking-async").build());
                     }
                   }, 5000);
  }
}
