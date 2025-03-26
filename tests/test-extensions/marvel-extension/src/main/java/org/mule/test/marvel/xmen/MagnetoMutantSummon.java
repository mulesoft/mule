/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.xmen;

import static org.mule.runtime.api.notification.AbstractServerNotification.CUSTOM_EVENT_ACTION_START_RANGE;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.inject.Inject;

@MediaType(TEXT_PLAIN)
public class MagnetoMutantSummon extends Source<InputStream, Void> {

  public static final String MESSAGE = "We are the future. ... You have lived in the shadows of shame and fear for too long!";
  public static final int ERROR_NOTIFICATION_ACTION = CUSTOM_EVENT_ACTION_START_RANGE + 99;
  public static final int CLASSLOADER_NOTIFICATION_ACTION = CUSTOM_EVENT_ACTION_START_RANGE + 101;

  @Inject
  private ServerNotificationManager notificationManager;

  @Override
  public void onStart(SourceCallback<InputStream, Void> sourceCallback) throws MuleException {
    sourceCallback.handle(makeResult());
  }

  private Result<InputStream, Void> makeResult() {
    return Result.<InputStream, Void>builder()
        .output(new ByteArrayInputStream(MESSAGE.getBytes()))
        .build();
  }

  @OnSuccess
  public void onSuccess(@ParameterGroup(name = "Response", showInDsl = true) MutantUnitedResponse mutantResponse,
                        CorrelationInfo correlationInfo,
                        SourceCallbackContext callbackContext)
      throws IOException {
    notifyContextClassLoader();

    if (mutantResponse.getBody().getValue() instanceof InputStream) {
      ((InputStream) mutantResponse.getBody().getValue()).read(new byte[1024]);
    }

    if (mutantResponse.getBody().getValue() instanceof CursorStreamProvider) {
      ((CursorStreamProvider) mutantResponse.getBody().getValue()).openCursor().read(new byte[1024]);
    }
  }

  @OnError
  public void onError(Error error) {
    notifyContextClassLoader();
    notificationManager.fireNotification(new MagnetoMutantNotification(error, ERROR_NOTIFICATION_ACTION));
  }

  @Override
  public void onStop() {
    // Nothing to do
  }

  private void notifyContextClassLoader() {
    notificationManager
        .fireNotification(new MagnetoMutantNotification(currentThread().getContextClassLoader(),
                                                        CLASSLOADER_NOTIFICATION_ACTION));
  }

}
