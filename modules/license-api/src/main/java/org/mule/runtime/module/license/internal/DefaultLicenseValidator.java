/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.license.internal;

import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.runtime.module.license.api.PluginLicenseValidationRequest;

public class DefaultLicenseValidator implements LicenseValidator {

  @Override
  public void validatePluginLicense(PluginLicenseValidationRequest pluginLicenseValidationRequest) {
    throw new IllegalStateException("Mule Runtime CE cannot run a licensed connector");
  }
}
