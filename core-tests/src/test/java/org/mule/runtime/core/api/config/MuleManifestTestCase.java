/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.junit.Test;

@SmallTest
public class MuleManifestTestCase {

  private static final String TEST_VERSION_PROPERTIES_PATH = "/test-version.properties";
  private static final String MULE_VERSION_PROPERTY_NAME = "mule.version";

  @Test
  public void getCoreManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-core-3.jar"));
  }

  @Test
  public void getExtModelManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-runtime-extension-model-3.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-runtime-extension-model-3.jar"));
  }

  @Test
  public void getCoreEeManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-core-ee-3.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-core-ee-3.jar"));
  }

  @Test
  public void getEeExtModelManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-runtime-ee-extension-model-3.jar",
                                                   "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-runtime-ee-extension-model-3.jar"));
  }

  @Test
  public void getEmbeddedManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-3-embedded.jar", "mule-foo-3.jar"));
    assertThat(url.toExternalForm(), containsString("mule-3-embedded.jar"));
  }

  @Test
  public void getVersionFromManifestProperty() throws Exception {
    String manifestVersion = MuleManifest.getProductVersion();
    String mavenVersion = getPropertyFromPropertiesFile(TEST_VERSION_PROPERTIES_PATH, MULE_VERSION_PROPERTY_NAME);
    assertThat(manifestVersion, equalTo(mavenVersion));
  }

  @Test
  public void getVersionFromFallbackOption() throws Exception {
    String fallbackVersion = MuleManifest.getProductVersionFromFromPropertiesFile();
    String mavenVersion = getPropertyFromPropertiesFile(TEST_VERSION_PROPERTIES_PATH, MULE_VERSION_PROPERTY_NAME);
    assertThat(fallbackVersion, equalTo(mavenVersion));
  }

  private String getPropertyFromPropertiesFile(String propertiesFilePath, String propertyName) throws IOException {
    InputStream versionPropsInputStream = getClass().getResourceAsStream(propertiesFilePath);
    Properties versionProps = new Properties();
    versionProps.load(versionPropsInputStream);
    return versionProps.getProperty(propertyName);
  }


  private Enumeration<URL> getUrlsEnum(String... names) throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    for (String name : names) {
      urls.add(new URL("file://" + name));
    }
    return Collections.enumeration(urls);
  }
}
