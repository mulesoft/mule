/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.Sources;
import org.mule.sdk.api.annotation.connectivity.ConnectionProviders;

import javax.inject.Inject;

@Configuration
@ConnectionProviders(JavaxInjectCompatibilityTestConnectionProvider.class)
@Sources(JavaxInjectCompatibilityTestSource.class)
@Operations(JavaxInjectCompatibilityTestOperations.class)
public class JavaxInjectCompatibilityTestConfiguration {

  @Inject
  private ArtifactEncoding encoding;

  public ArtifactEncoding getEncoding() {
    return encoding;
  }
}
