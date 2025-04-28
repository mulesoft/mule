/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.policy.api.extension;

import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

/**
 * Utility class to access the {@link ExtensionModel} definition for test Policy components
 *
 * @since 4.4
 */
public final class TestPolicyExtensionModelProvider extends ExtensionModelLoader {

  private TestPolicyExtensionModelProvider() {
    // Nothing to do
  }

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new TestPolicyExtensionModelProvider()
      .loadExtensionModel(new TestPolicyExtensionModelDeclarer().createExtensionModel(),
                          builder(TestPolicyExtensionModelProvider.class.getClassLoader(),
                                  nullDslResolvingContext())
                              .build()));

  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    // nothing to do
  }

  @Override
  public String getId() {
    return "test-policy";
  }

  /**
   * @return the {@link ExtensionModel} definition for test policies
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
