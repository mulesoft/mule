/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.manifest.internal;

import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;
import static org.mule.test.allure.AllureConstants.MuleManifestFeature.MULE_MANIFEST;

import static java.lang.System.getProperty;
import static java.util.Collections.enumeration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(MULE_MANIFEST)
public class DefaultMuleManifestTestCase {

  public static final String MULE_VERSION = "muleVersion";

  @Test
  public void getCoreManifest() throws Exception {
    DefaultMuleManifest.UrlPrivilegedAction action = new DefaultMuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-core-3.jar"));
  }

  @Test
  public void getExtModelManifest() throws Exception {
    DefaultMuleManifest.UrlPrivilegedAction action = new DefaultMuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-runtime-extension-model-3.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-runtime-extension-model-3.jar"));
  }

  @Test
  public void getCoreEeManifest() throws Exception {
    DefaultMuleManifest.UrlPrivilegedAction action = new DefaultMuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-core-ee-3.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-core-ee-3.jar"));
  }

  @Test
  public void getEeExtModelManifest() throws Exception {
    DefaultMuleManifest.UrlPrivilegedAction action = new DefaultMuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-runtime-ee-extension-model-3.jar",
                                                   "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-runtime-ee-extension-model-3.jar"));
  }

  @Test
  public void getEmbeddedManifest() throws Exception {
    DefaultMuleManifest.UrlPrivilegedAction action = new DefaultMuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-3-embedded.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-3-embedded.jar"));
  }

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

  private Enumeration<URL> getUrlsEnum(String... names) throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    for (String name : names) {
      urls.add(new URL("file://" + name));
    }
    return enumeration(urls);
  }

}
