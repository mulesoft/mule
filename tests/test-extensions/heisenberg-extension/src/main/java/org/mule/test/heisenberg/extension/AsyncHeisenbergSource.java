/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.apache.log4j.Logger.getLogger;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.notification.Fires;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.notification.NotificationEmitter;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.test.heisenberg.extension.model.Methylamine;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.stereotypes.AsyncSourceStereotype;

import org.apache.log4j.Logger;

@Alias("AsyncListenPayments")
@EmitsResponse
@Fires(SourceNotificationProvider.class)
@Streaming
@Stereotype(AsyncSourceStereotype.class)
@MediaType(TEXT_PLAIN)
public class AsyncHeisenbergSource extends HeisenbergSource {

  private static Logger LOGGER = getLogger(AsyncHeisenbergSource.class);

  public static SourceCompletionCallback completionCallback;

  @OnSuccess
  public void onSuccess(@Optional(defaultValue = PAYLOAD) Long payment, @Optional String sameNameParameter,
                        @ParameterGroup(name = RICIN_GROUP_NAME) RicinGroup ricin,
                        @ParameterGroup(name = "Success Info", showInDsl = true) PersonalInfo successInfo,
                        @Optional boolean fail,
                        SourceCompletionCallback completionCallback,
                        NotificationEmitter notificationEmitter) {
    LOGGER.error("onSuccess() - Start");

    AsyncHeisenbergSource.completionCallback = completionCallback;

    try {
      super.onSuccess(payment, sameNameParameter, ricin, successInfo, fail, notificationEmitter);
      completionCallback.success();
      LOGGER.error("onSuccess() - Completed");
    } catch (Throwable t) {
      completionCallback.error(t);
      LOGGER.error("onSuccess() - Exception");
    }
  }

  @OnError
  public void onError(Error error, @Optional String sameNameParameter, @Optional Methylamine methylamine,
                      @ParameterGroup(name = RICIN_GROUP_NAME) RicinGroup ricin,
                      @ParameterGroup(name = "Error Info", showInDsl = true) PersonalInfo infoError,
                      @Optional boolean propagateError,
                      SourceCompletionCallback completionCallback,
                      NotificationEmitter notificationEmitter) {
    LOGGER.error("onError() - Start");
    LOGGER.error(error.getErrorMessage());
    if (error.getCause() != null) {
      LOGGER.error("onError() - Cause");
      LOGGER.error(error.getCause());
      LOGGER.error(error.getCause().getMessage());
    }

    AsyncHeisenbergSource.completionCallback = completionCallback;

    try {
      super.onError(error, sameNameParameter, methylamine, ricin, infoError, propagateError, notificationEmitter);
      completionCallback.success();
      LOGGER.error("onError() - Completed");
    } catch (Throwable t) {
      completionCallback.error(t);
      LOGGER.error("onError() - Exception");
    }
  }
}
