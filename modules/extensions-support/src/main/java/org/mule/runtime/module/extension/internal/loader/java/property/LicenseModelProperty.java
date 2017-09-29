/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.meta.model.ModelProperty;

import java.util.Optional;

/**
 * {@link ModelProperty} that contains the information about the license requirements for the extensions.
 * 
 * @since 4.0
 */
public class LicenseModelProperty implements ModelProperty {

  private final boolean allowsEvaluationLicense;
  private boolean requiresEeLicense;
  private String requiredEntitlement;

  public LicenseModelProperty(boolean requiresEeLicense, boolean allowsEvaluationLicense, Optional<String> requiredEntitlement) {
    this.requiresEeLicense = requiresEeLicense;
    this.allowsEvaluationLicense = allowsEvaluationLicense;
    this.requiredEntitlement = requiredEntitlement.orElse(null);
  }

  @Override
  public String getName() {
    return "licenseModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * @return true if the extension requires an EE license, false otherwise.
   */
  public boolean requiresEeLicense() {
    return requiresEeLicense;
  }

  /**
   * @return true if the extension can be run with the evaluation license, false otherwise.
   */
  public boolean isAllowsEvaluationLicense() {
    return allowsEvaluationLicense;
  }

  /**
   * @return the required entitlement in the license to be able to run this extension. Empty if the extension does not require an
   *         entitlement.
   */
  public Optional<String> getRequiredEntitlement() {
    return ofNullable(requiredEntitlement);
  }

}

