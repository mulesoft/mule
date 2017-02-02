/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * This is some Another Config documentation.
 */
@Configuration(name = "anotherConfig")
@ConnectionProviders(TestAnotherDocumentedProvider.class)
@Operations({TestDocumentedExtensionOperations.class})
public class TestAnotherDocumentedConfig {

  /**
   * Connection parameter
   */
  @Parameter
  private String magicParam;
}
