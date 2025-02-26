/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;

import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * This is a static class that provides access to the Mule core manifest file.
 *
 * @deprecated since 4.9, use {@link org.mule.runtime.manifest.api.MuleManifest} instead.
 */
@Deprecated(since = "4.9.0")
public class MuleManifest {

  public static String getProductVersion() {
    return getMuleManifest().getProductVersion();
  }

  public static String getProductVersionFromPropertiesFile() {
    return getMuleManifest().getProductVersion();
  }

  public static String getVendorName() {
    return getMuleManifest().getVendorName();
  }

  public static String getVendorUrl() {
    return getMuleManifest().getVendorUrl();
  }

  public static String getProductUrl() {
    return getMuleManifest().getProductUrl();
  }

  public static String getProductName() {
    return getMuleManifest().getProductName();
  }

  public static String getProductMoreInfo() {
    return getMuleManifest().getProductMoreInfo();
  }

  public static String getProductSupport() {
    return getMuleManifest().getProductSupport();
  }

  public static String getProductLicenseInfo() {
    return getMuleManifest().getProductLicenseInfo();
  }

  public static String getProductDescription() {
    return getMuleManifest().getProductDescription();
  }

  public static String getBuildNumber() {
    return getMuleManifest().getBuildNumber();
  }

  public static String getBuildDate() {
    return getMuleManifest().getBuildDate();
  }

  public static String getSupportedJdks() {
    return getMuleManifest().getSupportedJdks();
  }

  /**
   * @deprecated use {@link #getRecommendedJdks()} instead.
   */
  @Deprecated
  public static String getRecommndedJdks() {
    return getMuleManifest().getRecommendedJdks();
  }

  /**
   * @since 4.6
   */
  public static String getRecommendedJdks() {
    return getMuleManifest().getRecommendedJdks();
  }

  // synchronize this method as manifest initialized here.
  public static synchronized Manifest getManifest() {
    return getMuleManifest().getManifest();
  }

  private MuleManifest() {}
}
