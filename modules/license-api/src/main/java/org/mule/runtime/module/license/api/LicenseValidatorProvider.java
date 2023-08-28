/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.license.api;

import org.mule.runtime.module.license.internal.DefaultLicenseValidator;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface LicenseValidatorProvider {

  static LicenseValidator discoverLicenseValidator(ClassLoader classLoader) {
    ServiceLoader<LicenseValidator> factories = ServiceLoader.load(LicenseValidator.class, classLoader);
    Iterator<LicenseValidator> iterator = factories.iterator();
    LicenseValidator licenseValidator = new DefaultLicenseValidator();
    while (iterator.hasNext()) {
      LicenseValidator discoveredLicenseValidator = iterator.next();
      if (licenseValidator == null || !(discoveredLicenseValidator instanceof DefaultLicenseValidator)) {
        licenseValidator = discoveredLicenseValidator;
      }
    }

    return licenseValidator;
  }

}
