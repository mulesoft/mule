/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.source;

import static org.mule.test.marvel.xmen.MagnetoMutantSummon.MESSAGE;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.client.source.SourceResultCallback;

import java.io.InputStream;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionClientSourceWithResponseTestCase extends BaseExtensionClientSourceTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Test
  public void sendSuccessfulResponse() throws Exception {
    Latch latch = new Latch();
    InputStream responseStream = mock(InputStream.class);

    Consumer<SourceResultCallback<InputStream, Void>> callbackConsumer = callback -> {
      String message = IOUtils.toString(callback.getResult().getOutput());
      assertThat(message, equalTo(MESSAGE));

      callback.completeWithSuccess(params -> params.withParameter("body", responseStream))
          .whenComplete((v, e) -> latch.release());
    };

    handler = extensionsClient.createSource("Marvel",
                                            "MagnetoMutantSummon",
                                            callbackConsumer,
                                            parameters -> {
                                            });

    handler.start();
    assertThat(latch.await(5, SECONDS), is(true));
    verify(responseStream).read(any(byte[].class));
  }

  @Test
  public void sendErrorResponse() throws Exception {
    final Latch latch = new Latch();
    final Reference<Error> capturedError = new Reference<>();
    final String errorMessage = "Long Live Professor X";

    notificationListenerRegistry.registerListener((CustomNotificationListener<CustomNotification>) notification -> {
      if (notification instanceof CustomNotification) {
        capturedError.set((Error) notification.getSource());
        latch.release();
      }
    });

    Consumer<SourceResultCallback<InputStream, Void>> callbackConsumer = callback -> {
      String message = IOUtils.toString(callback.getResult().getOutput());
      assertThat(message, equalTo(MESSAGE));

      callback.completeWithError(new RuntimeException(errorMessage), params -> {
      });
    };

    handler = extensionsClient.createSource("Marvel",
                                            "MagnetoMutantSummon",
                                            callbackConsumer,
                                            parameters -> {
                                            });

    handler.start();
    assertThat(latch.await(5, SECONDS), is(true));
    Error error = capturedError.get();

    assertThat(error.getDescription(), equalTo(errorMessage));
    assertThat(error.getErrorType().getIdentifier(), equalTo("UNKNOWN"));
    assertThat(error.getErrorType().getNamespace(), equalTo("MULE"));
  }
}
