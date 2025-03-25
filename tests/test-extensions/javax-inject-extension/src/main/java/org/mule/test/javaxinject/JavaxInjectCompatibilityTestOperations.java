/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.annotation.values.OfValues;

import javax.inject.Inject;

public class JavaxInjectCompatibilityTestOperations {

  @Inject
  private ArtifactEncoding encoding;

  @SampleData(JavaxInjectCompatibilityTestSampleDataProvider.class)
  public String execute() {
    return encoding.getDefaultEncoding().name();
  }

  public String executeConnection(@Connection JavaxInjectCompatibilityTestConnection conn) {
    return conn.getEncoding().getDefaultEncoding().name();
  }

  public String executeConfig(@Config JavaxInjectCompatibilityTestConfiguration config) {
    return config.getEncoding().getDefaultEncoding().name();
  }

  public String executePojo(JavaxInjectCompatibilityTestPojo pojoParam) {
    return pojoParam.getEncoding().getDefaultEncoding().name();
  }

  public String valueProvider(@OfValues(JavaxInjectCompatibilityTestValueProvider.class) String param) {
    return param;
  }

}
