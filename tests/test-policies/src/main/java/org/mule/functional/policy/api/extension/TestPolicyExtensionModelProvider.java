/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.policy.api.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

/**
 * Utility class to access the {@link ExtensionModel} definition for test Policy components
 *
 * @since 4.4
 */
public final class TestPolicyExtensionModelProvider {

  private TestPolicyExtensionModelProvider() {
    // Nothing to do
  }

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(new DefaultExtensionLoadingContext(new TestPolicyExtensionModelDeclarer().createExtensionModel(),
                                                 TestPolicyExtensionModelProvider.class.getClassLoader(),
                                                 new NullDslResolvingContext())));

  /**
   * @return the {@link ExtensionModel} definition for test policies
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
