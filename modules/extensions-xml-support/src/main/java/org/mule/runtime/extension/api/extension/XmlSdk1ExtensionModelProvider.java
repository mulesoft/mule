/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.api.extension;

import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule's XML SDK v1
 *
 * @since 4.4
 */
@NoInstantiate
public class XmlSdk1ExtensionModelProvider {

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(new DefaultExtensionLoadingContext(new XmlSdk1ExtensionModelDeclarer().createExtensionModel(),
                                                 builder(MuleExtensionModelProvider.class.getClassLoader(),
                                                         new NullDslResolvingContext()).build())));

  /**
   * @return the {@link ExtensionModel} definition for Mule's EE Runtime
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
