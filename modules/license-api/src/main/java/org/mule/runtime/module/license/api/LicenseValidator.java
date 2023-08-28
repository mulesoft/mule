/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
