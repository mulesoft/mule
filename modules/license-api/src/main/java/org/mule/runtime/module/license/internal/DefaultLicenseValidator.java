/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
