/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
