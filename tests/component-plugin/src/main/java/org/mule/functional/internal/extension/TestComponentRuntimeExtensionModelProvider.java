/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.internal.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

public class TestComponentRuntimeExtensionModelProvider implements RuntimeExtensionModelProvider {

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(new DefaultExtensionLoadingContext(new TestComponentExtensionModelDeclarer().createExtensionModel(),
          TestComponentRuntimeExtensionModelProvider.class.getClassLoader(),
          new NullDslResolvingContext())));


  @Override
  public ExtensionModel createExtensionModel() {
    return EXTENSION_MODEL.get();
  }
}
