/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.DslResourceFactory;

import java.util.Optional;

/**
 * Test implementation for generating DSL resources in order to test {@link ExtensionPluginMetadataGenerator}.
 */
public class TestDslResourceFactory implements DslResourceFactory {

  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel, DslResolvingContext dslResolvingContext) {
    return generateResource(extensionModel);
  }

  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    return Optional.of(new GeneratedResource(extensionModel.getName().toLowerCase() + ".xsd", new byte[] {1}));
  }

}
