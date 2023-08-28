/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
