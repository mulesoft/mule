/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.resolver;

import org.mule.sdk.api.data.sample.SampleDataProvider;

public abstract class TestSampleDataProvider implements SampleDataProvider<String, String> {

  @Override
  public String getId() {
    return getClass().getSimpleName();
  }
}
