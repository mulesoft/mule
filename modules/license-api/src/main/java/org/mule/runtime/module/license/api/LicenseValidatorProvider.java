/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.license.api;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface LicenseValidatorProvider {

  static LicenseValidator discoverLicenseValidator(ClassLoader classLoader) {
    ServiceLoader<LicenseValidator> factories = ServiceLoader.load(LicenseValidator.class, classLoader);
    Iterator<LicenseValidator> iterator = factories.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format("Could not find %s service implementation through SPI",
                                                    LicenseValidator.class.getName()));
    }
    return iterator.next();
  }

}
