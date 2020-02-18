/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;

/**
 * {@link ConfigurationBuilder} used to provide a mock implementation of {@link ExtensionManager} with an empty set of extensions.
 *
 * @since 4.2
 */
public class MockExtensionManagerConfigurationBuilder implements ConfigurationBuilder {

  @Override
  public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {

  }

  @Override
  public void configure(MuleContext muleContext) {
    if (muleContext.getExtensionManager() == null) {
      Thread currentThread = currentThread();
      ClassLoader originalClassLoader = currentThread.getContextClassLoader();
      ClassLoader contextClassLoader = MockExtensionManagerConfigurationBuilder.class.getClassLoader();
      setContextClassLoader(currentThread, originalClassLoader, contextClassLoader);
      try {
        ExtensionManager mockExtensionManager = mock(ExtensionManager.class, RETURNS_DEEP_STUBS.get());
        when(mockExtensionManager.getExtensions()).thenReturn(emptySet());
        muleContext.setExtensionManager(mockExtensionManager);
      } finally {
        setContextClassLoader(currentThread, contextClassLoader, originalClassLoader);
      }
    }
  }
}
