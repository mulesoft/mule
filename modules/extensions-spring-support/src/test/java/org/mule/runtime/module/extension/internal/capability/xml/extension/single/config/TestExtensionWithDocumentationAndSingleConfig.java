/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.single.config;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Test Extension Description with single config
 */
@Extension(name = "single")
@Operations({TestSingleConfigExtensionOperations.class})
@ConnectionProviders({TestSingleConfigExtensionProvider.class})
@Sources(TestDocumentedSource.class)
@Xml(namespace = "namespaceLocation", prefix = "documentation")
public class TestExtensionWithDocumentationAndSingleConfig {

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
}

