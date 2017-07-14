/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client;

import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.EXTENSIONS_CLIENT;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.ExtensionsClientStory.NON_BLOCKING_CLIENT;

import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXTENSIONS_CLIENT)
@Story(NON_BLOCKING_CLIENT)
public class NonBlockingExtensionsClientTestCase extends ExtensionsClientTestCase {

  @Override
  <T, A> Result<T, A> doExecute(String extension, String operation, OperationParameters params)
      throws Throwable {
    CompletableFuture<Result<T, A>> future = client.executeAsync(extension, operation, params);
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Failure. The test throw an exception: " + e.getMessage(), e);
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
