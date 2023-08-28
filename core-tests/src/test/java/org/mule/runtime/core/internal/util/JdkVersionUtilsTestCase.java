/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.util.JdkVersionUtils.isRecommendedJdkVersion;
import static org.mule.runtime.core.internal.util.JdkVersionUtils.isSupportedJdkVersion;
import static org.mule.test.allure.AllureConstants.SupportedEnvironmentsFeature.JdkVersionStory.JDK_VERSION;
import static org.mule.test.allure.AllureConstants.SupportedEnvironmentsFeature.SUPPORTED_ENVIRONMENTS;

import static java.lang.System.getProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@Feature(SUPPORTED_ENVIRONMENTS)
@Story(JDK_VERSION)
public class JdkVersionUtilsTestCase extends AbstractMuleTestCase {

  private String originalJavaVersion;
  private Manifest originalManifest;

  @Before
  public void before() {
    originalJavaVersion = getProperty("java.version");
    originalManifest = MuleManifest.getManifest();
  }

  @After
  public void after() throws Exception {
    setJdkVersion(originalJavaVersion);
    setManifest(originalManifest);
  }

  private static void setJdkVersion(String version) {
    System.setProperty("java.version", version);
  }

  private void setManifest(Manifest manifest)
      throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    Field field = MuleManifest.class.getDeclaredField("manifest");
    field.setAccessible(true);
    field.set(null, manifest);
  }

  @Test
  public void testIsSupportedJdkVersion() {
    // supported
    assertTrue(isSupportedJdkVersion());

    List<String> supported = Arrays.asList(
                                           "1.8.0",
                                           "1.8.20",
                                           "1.8.0_129",
                                           "9.0.0",
                                           "10.0.0",
                                           "11.0.0",
                                           "12.0.0",
                                           "13.0.0",
                                           "14.0.0",
                                           "15.0.0",
                                           "16.0.0",
                                           "17.0.0",
                                           "17.0.6");

    for (String version : supported) {
      setJdkVersion(version);
      assertTrue(isSupportedJdkVersion());
    }

    // not supported
    List<String> notSupported = Arrays.asList(
                                              "1.7.2",
                                              "1.7.2_12",
                                              "18.0.0");

    for (String version : notSupported) {
      setJdkVersion(version);
      assertFalse(isSupportedJdkVersion());
    }
  }

  @Test
  public void testUndefinedJdkPreferences() throws Exception {
    setJdkVersion("1.4.2");

    // not defined - blank
    setJdkPreferences("");
    assertEquals("", JdkVersionUtils.getRecommendedJdks());
    assertEquals("", JdkVersionUtils.getSupportedJdks());

    assertTrue(isRecommendedJdkVersion());
    assertTrue(JdkVersionUtils.isSupportedJdkVendor());
    assertTrue(isSupportedJdkVersion());

    // not defined - null
    setJdkPreferences(null);
    assertNull(JdkVersionUtils.getRecommendedJdks());
    assertNull(JdkVersionUtils.getSupportedJdks());

    assertTrue(isRecommendedJdkVersion());
    assertTrue(JdkVersionUtils.isSupportedJdkVendor());
    assertTrue(isSupportedJdkVersion());
  }

  private void setJdkPreferences(String preference) throws Exception {
    // mock the manifest (this is where the jdk preferences are taken from
    Manifest manifest = Mockito.mock(Manifest.class);
    Attributes attributes = Mockito.mock(Attributes.class);
    Mockito.when(attributes.getValue(Mockito.any(Attributes.Name.class))).thenReturn(preference);
    Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);

    setManifest(manifest);
  }

  @Test
  public void testSupportedJdkVendor() {
    assertTrue(JdkVersionUtils.isSupportedJdkVendor());
  }

  @Test
  public void testRecommendedJdkVersion() {
    // recommended
    List<String> recommended = Arrays.asList(
                                             "1.8.0_181",
                                             "1.8.20",
                                             "11.0.0",
                                             "17.0.0",
                                             "17.0.6");

    for (String version : recommended) {
      setJdkVersion(version);
      assertTrue(isRecommendedJdkVersion());
    }

    // not recommended
    List<String> notRecommended = Arrays.asList(
                                                "1.4.2",
                                                "1.6",
                                                "1.6.0_5",
                                                "1.7.0",
                                                "9.0.0",
                                                "10.0.0",
                                                "12.0.0",
                                                "13.0.0",
                                                "14.0.0",
                                                "15.0.0",
                                                "16.0.0",
                                                "18.0.0");

    for (String version : notRecommended) {
      setJdkVersion(version);
      assertFalse(isRecommendedJdkVersion());
    }
  }

  @Test
  public void testJdkVersion() {
    JdkVersion jdkVersion = new JdkVersion("1.7");
    assertEquals(new Integer(1), jdkVersion.getMajor());
    assertEquals(new Integer(7), jdkVersion.getMinor());
    assertNull(jdkVersion.getMicro());
    assertNull(jdkVersion.getUpdate());
    assertNull(jdkVersion.getMilestone());

    jdkVersion = new JdkVersion("1.7.0-ea");
    assertEquals(new Integer(1), jdkVersion.getMajor());
    assertEquals(new Integer(7), jdkVersion.getMinor());
    assertEquals(new Integer(0), jdkVersion.getMicro());
    assertNull(jdkVersion.getUpdate());
    assertEquals("ea", jdkVersion.getMilestone());

    jdkVersion = new JdkVersion("1.6.0_29-b05");
    assertEquals(new Integer(1), jdkVersion.getMajor());
    assertEquals(new Integer(6), jdkVersion.getMinor());
    assertEquals(new Integer(0), jdkVersion.getMicro());
    assertEquals(new Integer(29), jdkVersion.getUpdate());
    assertEquals("b05", jdkVersion.getMilestone());
  }

  @Test
  public void testJdkVersionComparison() {
    JdkVersion jdk1_3 = new JdkVersion("1.3");
    JdkVersion jdk1_6_0_5 = new JdkVersion("1.6.0_5");
    JdkVersion jdk1_7 = new JdkVersion("1.7");
    JdkVersion jdk1_6_0_29 = new JdkVersion("1.6.0_29");
    JdkVersion jdk1_6_0_29_b04 = new JdkVersion("1.6.0_29-b04");
    JdkVersion jdk1_6_0_29_b05 = new JdkVersion("1.6.0_29-b05");

    assertTrue(jdk1_3.compareTo(jdk1_7) < 0);
    assertTrue(jdk1_7.compareTo(jdk1_3) > 0);
    assertTrue(jdk1_3.compareTo(jdk1_3) == 0);
    assertTrue(jdk1_6_0_29_b05.compareTo(jdk1_6_0_29_b05) == 0);

    assertTrue(jdk1_6_0_5.compareTo(jdk1_6_0_29_b04) < 0);
    assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_6_0_5) > 0);
    assertTrue(jdk1_6_0_29.compareTo(jdk1_6_0_5) > 0);
    assertTrue(jdk1_6_0_5.compareTo(jdk1_6_0_29) < 0);
    assertTrue(jdk1_6_0_29.compareTo(jdk1_6_0_29_b04) < 0);
    assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_6_0_29) > 0);

    assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_6_0_29_b05) < 0);
    assertTrue(jdk1_6_0_29_b05.compareTo(jdk1_6_0_29_b04) > 0);

    assertTrue(jdk1_6_0_29_b04.compareTo(jdk1_7) < 0);
    assertTrue(jdk1_7.compareTo(jdk1_6_0_29_b04) > 0);
  }

  @Test
  public void testValidateJdk() {
    JdkVersionUtils.validateJdk();
    setJdkVersion("1.8.0");
    JdkVersionUtils.validateJdk();
    setJdkVersion("1.8.0_129");
    JdkVersionUtils.validateJdk();
    setJdkVersion("1.8.20");
    JdkVersionUtils.validateJdk();
    setJdkVersion("1.9.0");
    JdkVersionUtils.validateJdk();
    setJdkVersion("1.9.0_03");
    JdkVersionUtils.validateJdk();
    setJdkVersion("1.9.0_51");
    JdkVersionUtils.validateJdk();

  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testValidateJdk5() {
    setJdkVersion("1.5.1");
    JdkVersionUtils.validateJdk();
  }

  @Test
  public void testValidateJdk8() {
    setJdkVersion("1.8.0");
    JdkVersionUtils.validateJdk();
  }
}
