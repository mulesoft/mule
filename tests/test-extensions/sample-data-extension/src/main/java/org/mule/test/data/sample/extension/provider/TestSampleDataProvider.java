/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import org.mule.sdk.api.data.sample.SampleDataProvider;

public abstract class TestSampleDataProvider implements SampleDataProvider<String, String> {

  @Override
  public String getId() {
    return getClass().getSimpleName();
  }
}
