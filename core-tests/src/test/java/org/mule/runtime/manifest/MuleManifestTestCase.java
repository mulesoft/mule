/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.manifest;

import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;
import static org.mule.test.allure.AllureConstants.MuleManifestFeature.MULE_MANIFEST;

import static java.lang.System.getProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.runtime.manifest.internal.DefaultMuleManifest;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(MULE_MANIFEST)
public class MuleManifestTestCase {

  public static final String MULE_VERSION = "muleVersion";

  @Test
  public void getVersionFromManifestProperty() {
    String manifestVersion = getMuleManifest().getProductVersion();
    assertThat(manifestVersion, equalTo(getProperty(MULE_VERSION)));
  }

  @Test
  public void getVersionFromFallbackOption() {
    String fallbackVersion = ((DefaultMuleManifest) getMuleManifest()).getProductVersionFromPropertiesFile();
    assertThat(fallbackVersion, equalTo(getProperty(MULE_VERSION)));
  }

}
