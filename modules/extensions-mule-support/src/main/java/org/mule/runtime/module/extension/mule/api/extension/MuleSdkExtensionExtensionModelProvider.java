/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.extension;

import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule SDK Extensions
 *
 * @since 4.5
 */
@Experimental
@NoInstantiate
public class MuleSdkExtensionExtensionModelProvider extends ExtensionModelLoader {

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL =
      new LazyValue<>(() -> new MuleSdkExtensionExtensionModelProvider()
          .loadExtensionModel(new MuleSdkExtensionExtensionModelDeclarer().declareExtensionModel(),
                              builder(MuleSdkExtensionExtensionModelProvider.class.getClassLoader(),
                                      nullDslResolvingContext())
                                  .build()));

  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    // nothing to do
  }

  @Override
  public String getId() {
    return "mule-muleSdk";
  }

  /**
   * @return the {@link ExtensionModel} definition.
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
