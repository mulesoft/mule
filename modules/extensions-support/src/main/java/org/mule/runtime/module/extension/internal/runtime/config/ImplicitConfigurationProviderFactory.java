/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

/**
 * Provides implicit configuration providers which are compliant with a {@link ConfigurationModel}. These are used when no
 * configuration has been specified. A best effort is made to automatically provide a default one but it may not be possible.
 *
 * @since 3.8.0
 */
public interface ImplicitConfigurationProviderFactory {

  /**
   * Creates an implicit configuration provider
   *
   * @param extensionModel the {@link ExtensionModel} from which a {@link ConfigurationModel} is to be selected
   * @param muleEvent the current {@link MuleEvent}
   * @param <C> the generic type of the returned {@link ConfigurationInstance}
   * @return a {@link ConfigurationProvider}
   * @throws IllegalStateException if it's not possible to create an implicit configuration automatically
   */
  <C> ConfigurationProvider<C> createImplicitConfigurationProvider(ExtensionModel extensionModel, MuleEvent muleEvent);
}
