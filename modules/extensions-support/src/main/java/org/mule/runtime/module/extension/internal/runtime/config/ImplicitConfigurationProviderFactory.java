/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

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
   * @param extensionModel             the {@link ExtensionModel} from which a {@link ConfigurationModel} is to be selected
   * @param implicitConfigurationModel the {@link ConfigurationModel} to be created.
   * @param muleEvent                  the current {@link CoreEvent}
   * @param reflectionCache            the {@link ReflectionCache} used to improve reflection lookups performance
   * @param expressionManager          the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   * @param muleContext                the Mule node.
   *
   * @return a {@link ConfigurationProvider}
   * @throws IllegalStateException if it's not possible to create an implicit configuration automatically
   */
  ConfigurationProvider createImplicitConfigurationProvider(ExtensionModel extensionModel,
                                                            ConfigurationModel implicitConfigurationModel,
                                                            CoreEvent muleEvent,
                                                            ReflectionCache reflectionCache,
                                                            ExpressionManager expressionManager,
                                                            MuleContext muleContext);

  /**
   * Returns an implicit {@link ConfigurationProvider} name, that can be used to look it up.
   *
   * @param extensionModel     The configurable {@link ExtensionModel}.
   * @param configurationModel The {@link ConfigurationModel} that represents the extensionModel configuration.
   * @param muleContext        The corresponding {@link MuleContext}.
   * @return The {@link ConfigurationProvider} name.
   * @see org.mule.runtime.module.extension.internal.manager.ExtensionRegistry#getConfigurationProvider(String)
   */
  String resolveImplicitConfigurationProviderName(ExtensionModel extensionModel, ConfigurationModel configurationModel,
                                                  MuleContext muleContext);

}
