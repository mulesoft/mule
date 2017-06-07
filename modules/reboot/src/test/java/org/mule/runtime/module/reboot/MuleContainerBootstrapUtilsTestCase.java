/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

public class MuleContainerBootstrapUtilsTestCase {

  @Before
  public void setUp() {
    System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, "foo");
  }

  /**
   * Test method for {@link MuleContainerBootstrapUtils#isStandalone()}.
   */
  @Test
  public void testIsStandaloneTrue() {
    assertTrue(MuleContainerBootstrapUtils.isStandalone());
  }

  /**
   * Test method for {@link MuleContainerBootstrapUtils#getMuleHome()}.
   */
  @Test
  public void testGetMuleHomeFile() {
    File muleHome = MuleContainerBootstrapUtils.getMuleHome();
    assertNotNull(muleHome.getAbsolutePath());
  }

  /**
   * Test method for {@link MuleContainerBootstrapUtils#getMuleAppsDir()}.
   */
  @Test
  public void testGetMuleAppsFile() {
    File muleApps = MuleContainerBootstrapUtils.getMuleAppsDir();
    assertNotNull(muleApps.getAbsolutePath());
  }

  /**
   * Test method for {@link MuleContainerBootstrapUtils#getMuleLibDir()}.
   */
  @Test
  public void testGetMuleLibDir() {
    File muleLib = MuleContainerBootstrapUtils.getMuleLibDir();
    assertNotNull(muleLib.getAbsolutePath());
  }

  /**
   * Test method for {@link MuleContainerBootstrapUtils#getMuleLocalJarFile()}.
   */
  @Test
  public void testGetMuleLocalJarFile() {
    File muleLocalJar = MuleContainerBootstrapUtils.getMuleLocalJarFile();
    assertNotNull(muleLocalJar.getAbsolutePath());
  }

  /**
   * Test method for
   * {@link MuleContainerBootstrapUtils#getResource(java.lang.String, java.lang.Class)}.
   * 
   * @throws IOException
   */
  @Test
  public void testGetResource() throws IOException {
    URL resource = MuleContainerBootstrapUtils.getResource("test-resource.txt", this.getClass());
    assertNotNull(resource);
    Object content = resource.getContent();
    assertTrue(content instanceof InputStream);
    BufferedReader in = new BufferedReader(new InputStreamReader((InputStream) content));
    assertEquals("msg=Hello World", in.readLine());
  }



}


