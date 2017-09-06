/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
