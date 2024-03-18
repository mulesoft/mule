/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.api;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface MuleContainerLifecycleWrapperProvider {

  MuleContainerLifecycleWrapper getMuleContainerLifecycleWrapper();

  static MuleContainerLifecycleWrapperProvider load(ClassLoader classLoader) {
    ServiceLoader<MuleContainerLifecycleWrapperProvider> factories =
        ServiceLoader.load(MuleContainerLifecycleWrapperProvider.class, classLoader);
    Iterator<MuleContainerLifecycleWrapperProvider> iterator = factories.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format("Could not find %s service implementation through SPI",
                                                    MuleContainerLifecycleWrapperProvider.class.getName()));
    }
    return iterator.next();
  }

}
