/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.extension;

import static org.mule.runtime.api.util.MuleSystemProperties.FORCE_EXTENSION_VALIDATION_PROPERTY_NAME;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule SDK Extensions
 *
 * @since 4.5
 */
@NoInstantiate
public class MuleSdkExtensionExtensionModelProvider {

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(contextFor(new MuleSdkExtensionExtensionModelDeclarer().declareExtensionModel())));

  private static ExtensionLoadingContext contextFor(ExtensionDeclarer declarer) {
    return new DefaultExtensionLoadingContext(declarer, loadingRequest());
  }

  private static ExtensionModelLoadingRequest loadingRequest() {
    return ExtensionModelLoadingRequest
        .builder(MuleSdkExtensionExtensionModelProvider.class.getClassLoader(), new NullDslResolvingContext())
        .build();
  }

  /**
   * @return the {@link ExtensionModel} definition.
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
