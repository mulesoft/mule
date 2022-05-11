/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.info;


/**
 * Class that models the information of the RequiresEnterpriseLicense annotations.
 *
 * @since 4.5
 */
public class RequiresEnterpriseLicenseInfo {

  private final boolean allowEvaluationLicense;

  public RequiresEnterpriseLicenseInfo(boolean allowEvaluationLicense) {
    this.allowEvaluationLicense = allowEvaluationLicense;
  }

  public boolean isAllowEvaluationLicense() {
    return allowEvaluationLicense;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    RequiresEnterpriseLicenseInfo that = (RequiresEnterpriseLicenseInfo) o;

    return allowEvaluationLicense == that.allowEvaluationLicense;
  }

  @Override
  public int hashCode() {
    return (allowEvaluationLicense ? 1 : 0);
  }
}
