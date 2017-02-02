/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * This is some documentation.
 */
@Extension(name = "documentation", description = "Test Extension Description")
@Operations({TestDocumentedExtensionOperations.class})
@ConnectionProviders(TestDocumentedProvider.class)
@Xml(namespaceLocation = "namespaceLocation", namespace = "documentation")
public class TestExtensionWithDocumentation {

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
