/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.api;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface MuleLog4jContextFactoryConfigurator {

  void createAndLoggerInstall();

  static MuleLog4jContextFactoryConfigurator load(ClassLoader classLoader) {
    ServiceLoader<MuleLog4jContextFactoryConfigurator> factories =
        ServiceLoader.load(MuleLog4jContextFactoryConfigurator.class, classLoader);
    Iterator<MuleLog4jContextFactoryConfigurator> iterator = factories.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format("Could not find %s service implementation through SPI",
                                                    MuleLog4jContextFactoryConfigurator.class.getName()));
    }
    return iterator.next();
  }

}
