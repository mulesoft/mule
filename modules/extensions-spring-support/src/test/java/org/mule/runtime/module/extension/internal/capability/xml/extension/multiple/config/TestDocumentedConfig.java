/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

/**
 * This is some Config documentation.
 */
@Configuration(name = "config")
@Operations({TestDocumentedExtensionOperations.class})
@ConnectionProviders({TestDocumentedProvider.class, TestAnotherDocumentedProvider.class})
public class TestDocumentedConfig {

  /**
   * Config parameter
   */
  @Parameter
  private String configParameter;

  /**
   * Config {@link Parameter} with an {@link Optional} value
   */
  @Parameter
  @Optional
  private String configParameterWithComplexJavadoc;

  @ParameterGroup(name = "group")
  private TestDocumentedParameterGroup group;
}
