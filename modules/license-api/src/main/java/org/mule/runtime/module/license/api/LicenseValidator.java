/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.license.api;

/**
 * This class represents a validator for licenses within the mule runtime.
 * 
 * @since 4.0
 */
public interface LicenseValidator {

  /**
   * Validates a plugin license.
   * 
   * @param pluginLicenseValidationRequest the plugin license validation request.
   */
  void validatePluginLicense(PluginLicenseValidationRequest pluginLicenseValidationRequest);

}
