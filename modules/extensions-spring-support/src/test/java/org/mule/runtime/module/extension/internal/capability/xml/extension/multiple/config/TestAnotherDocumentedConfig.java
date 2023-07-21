/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * This is some Another Config documentation.
 */
@Configuration(name = "another")
@ConnectionProviders(TestAnotherDocumentedProvider.class)
@Operations({TestDocumentedExtensionOperations.class})
@Sources({TestDocumentedExtensionSource.class})
public class TestAnotherDocumentedConfig {

  /**
   * Connection parameter
   */
  @Parameter
  private String magicParam;
}
