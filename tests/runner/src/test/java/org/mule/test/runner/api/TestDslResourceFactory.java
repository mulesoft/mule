/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
