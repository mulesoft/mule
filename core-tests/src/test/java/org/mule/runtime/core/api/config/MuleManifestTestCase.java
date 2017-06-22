/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.tck.size.SmallTest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class MuleManifestTestCase {

  @Test
  public void getCoreManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-foo-3.jar"));
    Assert.assertTrue(url.toExternalForm().contains("mule-core-3.jar"));
  }

  @Test
  public void getCoreEeManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-core-3.jar", "mule-core-ee-3.jar", "mule-foo-3.jar"));
    Assert.assertTrue(url.toExternalForm().contains("mule-core-ee-3.jar"));
  }

  @Test
  public void getEmbeddedManifest() throws Exception {
    MuleManifest.UrlPrivilegedAction action = new MuleManifest.UrlPrivilegedAction();
    URL url = action.getManifestJarURL(getUrlsEnum("mule-bas-3.jar", "mule-3-embedded.jar", "mule-foo-3.jar"));
    Assert.assertTrue(url.toExternalForm().contains("mule-3-embedded.jar"));
  }

  private Enumeration<URL> getUrlsEnum(String... names) throws MalformedURLException {
    List<URL> urls = new ArrayList<URL>();
    for (String name : names) {
      urls.add(new URL("file://" + name));
    }
    return Collections.enumeration(urls);
  }
}
