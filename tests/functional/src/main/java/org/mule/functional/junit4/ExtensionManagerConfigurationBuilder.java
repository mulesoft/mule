/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;

/**
 * {@link ConfigurationBuilder} used to provide a mock implementation of {@link ExtensionManager} with an empty set of extensions.
 *
 * @since 4.4
 */
public class ExtensionManagerConfigurationBuilder implements ConfigurationBuilder {

  @Override
  public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {

  }

  @Override
  public void configure(MuleContext muleContext) {
    if (muleContext.getExtensionManager() == null) {
      withContextClassLoader(ExtensionManagerConfigurationBuilder.class.getClassLoader(), () -> {
        DefaultExtensionManager extensionManager = new DefaultExtensionManager();
        muleContext.setExtensionManager(extensionManager);
        try {
          initialiseIfNeeded(extensionManager, muleContext);
        } catch (InitialisationException e) {
          throw new MuleRuntimeException(e);
        }

        extensionManager.registerExtension(getExtensionModel());
      });
    }
  }
}
