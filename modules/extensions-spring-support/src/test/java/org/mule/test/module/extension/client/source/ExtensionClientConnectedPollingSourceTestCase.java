/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.source;

import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.EXTENSIONS_CLIENT;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.ExtensionsClientStory.MESSAGE_SOURCE;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.client.source.SourceParameterizer;
import org.mule.runtime.extension.api.client.source.SourceResultHandler;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(EXTENSIONS_CLIENT)
@Story(MESSAGE_SOURCE)
public class ExtensionClientConnectedPollingSourceTestCase extends BaseExtensionClientSourceTestCase {

  @Rule
  public SystemProperty configProperty = new SystemProperty("configName", "petstore");

  @Rule
  public ExpectedException expectedException = none();


  @Override
  protected String getConfigFile() {
    return "petstore.xml";
  }

  @Test
  public void initPollingSource() throws Exception {
    Consumer<SourceParameterizer> parameterizer = parameters -> parameters
        .withConfigRef(configProperty.getValue())
        .withParameter("watermark", true)
        .withParameter("idempotent", true)
        .withFixedSchedulingStrategy(1, SECONDS, 0);


    assertPolling(parameterizer);
  }

  @Test
  public void pollingSourceWithoutSchedulingStrategy() throws Exception {
    Consumer<SourceParameterizer> parameterizer = parameters -> parameters
        .withConfigRef(configProperty.getValue())
        .withParameter("watermark", true)
        .withParameter("idempotent", true);

    assertPolling(parameterizer);
  }

  private void assertPolling(Consumer<SourceParameterizer> parameterizer) throws MuleException, InterruptedException {
    List<Result<String, Void>> results = new CopyOnWriteArrayList<>();
    final int petCount = ALL_PETS.size();
    CountDownLatch latch = new CountDownLatch(petCount);
    Consumer<SourceResultHandler<String, Void>> handlerConsumer = handler -> {
      results.add(handler.getResult());
      handler.completeWithSuccess(params -> {
      });
      latch.countDown();
    };
    handler = extensionsClient.createSource("petstore",
                                            "ConnectedPetAdoptionSource",
                                            handlerConsumer,
                                            parameterizer);

    handler.start();
    assertThat(latch.await(5, SECONDS), is(true));
    assertThat(results.size(), greaterThanOrEqualTo(petCount));

    for (int i = 0; i < petCount; i++) {
      Result<String, Void> result = results.get(i);
      assertThat(result.getOutput(), equalTo(ALL_PETS.get(i)));
      assertThat(result.getMediaType().get().matches(TEXT), is(true));
      assertThat(result.getAttributes().isPresent(), is(false));
      assertThat(result.getAttributesMediaType().isPresent(), is(false));
    }
  }
}
