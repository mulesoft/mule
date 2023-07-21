/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import org.mule.runtime.api.exception.MuleException;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

public class AbstractSdkSource extends Source<String, String> {

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}
