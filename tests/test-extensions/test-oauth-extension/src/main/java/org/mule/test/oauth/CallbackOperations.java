/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;

public class CallbackOperations {

  public void captureCallbackPayloads(@Config AuthCodeConfig config,
                                      @Optional AuthCodeRequest request,
                                      @Optional AuthorizationCodeState state) {
    config.getCapturedAuthCodeRequests().add(request);
    config.getCapturedAuthCodeStates().add(state);
  }

  public void captureSdkCallbackPayloads(@Config AuthCodeConfig config,
                                         @Optional org.mule.sdk.api.connectivity.oauth.AuthCodeRequest request,
                                         @Optional AuthorizationCodeState state) {
    config.getCapturedAuthCodeRequests().add(request);
    config.getCapturedAuthCodeStates().add(state);
  }
}
