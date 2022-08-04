/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.sdk.api.runtime.operation.Result.builder;

import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;

public class RefreshedOAuthSampleDataProvider implements SampleDataProvider<String, String> {

  public static final String SAMPLE_PAYLOAD_VALUE = "Sample payload!";
  public static final String SAMPLE_ATTRIBUTES_VALUE = "Sample Attributes!";

  @Connection
  private TestOAuthConnection testOAuthConnection;

  @Override
  public String getId() {
    return "OAuth sample data";
  }

  @Override
  public Result getSample() throws SampleDataException {
    if (!testOAuthConnection.getState().getState().getAccessToken().contains("refresh")) {
      throw new AccessTokenExpiredException();
    }
    return builder()
        .output(SAMPLE_PAYLOAD_VALUE)
        .attributes(SAMPLE_ATTRIBUTES_VALUE)
        .build();
  }
}
