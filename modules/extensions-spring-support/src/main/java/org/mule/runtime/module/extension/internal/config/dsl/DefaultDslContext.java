/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslResolvingContext;

import java.util.Optional;

/**
 * Default implementation of {@link DslResolvingContext} that uses the {@link ExtensionManager} to provide the required
 * {@link ExtensionModel}s
 *
 * @since 4.0
 */
public class DefaultDslContext implements DslResolvingContext {


  private final ExtensionManager extensionManager;

  public DefaultDslContext(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ExtensionModel> getExtension(String name) {
    return extensionManager.getExtension(name).map(e -> e);
  }
}
