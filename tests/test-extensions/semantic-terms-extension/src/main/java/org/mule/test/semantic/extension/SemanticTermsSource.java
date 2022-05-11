/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension;

import static org.mule.sdk.api.annotation.param.MediaType.APPLICATION_JSON;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.sdk.api.annotation.execution.OnSuccess;
import org.mule.sdk.api.annotation.semantics.connectivity.Endpoint;
import org.mule.sdk.api.annotation.semantics.security.AccountId;
import org.mule.sdk.api.annotation.semantics.security.Secret;
import org.mule.sdk.api.annotation.semantics.security.TenantIdentifier;
import org.mule.sdk.api.annotation.semantics.security.Username;

@MediaType(APPLICATION_JSON)
public class SemanticTermsSource extends Source<String, Void> {

  @Parameter
  @Endpoint
  private String endpoint;

  @OnSuccess
  private String onSuccess(@AccountId String accountId, @TenantIdentifier String tenantId) {
    return "";
  }

  @OnError
  private String onError(@Secret String secret, @Username String username) {
    return "";
  }

  @Override
  public void onStart(SourceCallback<String, Void> sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}
