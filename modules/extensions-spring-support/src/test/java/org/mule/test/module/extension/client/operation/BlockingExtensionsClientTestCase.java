/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.operation;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.EXTENSIONS_CLIENT;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.ExtensionsClientStory.BLOCKING_CLIENT;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(EXTENSIONS_CLIENT)
@Story(BLOCKING_CLIENT)
public class BlockingExtensionsClientTestCase extends ExtensionsClientTestCase {

  @Override
  protected <T, A> Result<T, A> doExecute(String extension, String operation, Optional<String> configName,
                                          Map<String, Object> params, boolean isPagedOperation, boolean supportsStreaming)
      throws Throwable {
    try {
      return (Result<T, A>) client.execute(extension, operation, parameterizer -> {
        configName.ifPresent(parameterizer::withConfigRef);
        params.forEach(parameterizer::withParameter);
        if (isPagedOperation) {
          parameterizer.withDefaultRepeatableIterables();
        } else if (supportsStreaming) {
          parameterizer.withDefaultRepeatableStreaming();
        }
      }).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Failure. The test threw an exception: " + e.getMessage(), e);
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void executeNonRepeatablePagedOperation() throws Throwable {
    Result<Iterator<Message>, Object> result = client
        .<Iterator<Message>, Object>execute(HEISENBERG_EXT_NAME, "getPagedBlocklist",
                                            params -> params.withConfigRef(HEISENBERG_CONFIG))
        .get();

    AtomicInteger count = new AtomicInteger(0);
    result.getOutput().forEachRemaining(m -> count.addAndGet(1));
    assertThat(count.get(), is(6));
  }

  @Test
  public void executeNonRepeatableInputStreamOperation() throws Throwable {
    Result<InputStream, Object> result =
        client
            .<InputStream, Object>execute(HEISENBERG_EXT_NAME, "nameAsStream",
                                          params -> params.withConfigRef(HEISENBERG_CONFIG))
            .get();


    String value = IOUtils.toString(result.getOutput());
    try {
      assertThat(value, equalTo("Heisenberg"));
    } finally {
      closeQuietly(result.getOutput());
    }
  }

  @Test
  public void executeOperationWithInternalParameterGroup() throws Throwable {
    final String message = "Skyler cheated on you";
    Result<String, Void> result = client.<String, Void>execute(HEISENBERG_EXT_NAME,
                                                               "whisperSecret",
                                                               params -> params
                                                                   .withConfigRef(HEISENBERG_CONFIG)
                                                                   .withParameter("internalGroup", "secret", message))
        .get();

    assertThat(result.getOutput(), equalTo(message));
  }

}
