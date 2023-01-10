/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.source;

import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.source.SourceHandler;
import org.mule.runtime.extension.api.client.source.SourceResultCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExtensionClientConnectedPollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public SystemProperty configProperty = new SystemProperty("configName", "petstore");

  @Rule
  public ExpectedException expectedException = none();

  @Inject
  private ExtensionsClient extensionsClient;

  private SourceHandler handler;

  @Override
  protected String getConfigFile() {
    return "petstore.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    stopAndDispose(handler);
  }

  @Test
  public void test() {
    CompletableFuture<Void> f = new CompletableFuture<>();
    f = f.whenComplete((v, e) -> {
      if (e != null) {
        System.out.println("error");
      } else {
        System.out.println("yes");
      }
    });

    f.complete(null);

    CompletableFuture<Void> f2 = new CompletableFuture<>();
    f2 = f2.whenComplete((v, e) -> {
      if (e != null) {
        System.out.println("error");
      } else {
        System.out.println("yes");
      }
    });

    f2.completeExceptionally(new RuntimeException());

  }

  @Test
  public void initPollingSource() throws Exception {
    List<Result<String, Void>> results = new CopyOnWriteArrayList<>();
    final int petCount = ALL_PETS.size();
    CountDownLatch latch = new CountDownLatch(petCount);
    Consumer<SourceResultCallback<String, Void>> callbackConsumer = callback -> {
      results.add(callback.getResult());
      latch.countDown();
    };

    handler = extensionsClient.createSource("petstore",
                                            "ConnectedPetAdoptionSource",
                                            callbackConsumer,
                                            parameters -> parameters
                                              .withConfigRef(configProperty.getValue())
                                              .withParameter("watermark", true)
                                              .withParameter("idempotent", true)
                                              .withFixedSchedulingStrategy(1, SECONDS, 0));

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

  @Test
  public void pollingSourceWithoutSchedulingStrategy() throws Exception {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));

    handler = extensionsClient.createSource("petstore",
                                            "ConnectedPetAdoptionSource",
                                            callback -> {
                                            },
                                            parameters -> parameters
                                              .withConfigRef(configProperty.getValue())
                                              .withParameter("watermark", true)
                                              .withParameter("idempotent", true));
  }

  private void stopAndDispose(SourceHandler handler) throws Exception {
    if (handler != null) {
      try {
        handler.stop();
      } finally {
        handler.dispose();
      }
    }
  }
}
