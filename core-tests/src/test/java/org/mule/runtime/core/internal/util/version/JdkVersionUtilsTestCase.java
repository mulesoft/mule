/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.version;

import static org.mule.runtime.core.internal.util.version.JdkVersionUtils.isRecommendedJdkVersion;
import static org.mule.runtime.core.internal.util.version.JdkVersionUtils.isSupportedJdkVersion;
import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;
import static org.mule.test.allure.AllureConstants.SupportedEnvironmentsFeature.SUPPORTED_ENVIRONMENTS;
import static org.mule.test.allure.AllureConstants.SupportedEnvironmentsFeature.JdkVersionStory.JDK_VERSION;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.util.version.JdkVersionUtils.JdkVersion;
import org.mule.runtime.manifest.internal.DefaultMuleManifest;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SUPPORTED_ENVIRONMENTS)
@Story(JDK_VERSION)
public class JdkVersionUtilsTestCase extends AbstractMuleTestCase {

  private String originalJavaVersion;
  private Manifest originalManifest;

  @Before
  public void before() {
    originalJavaVersion = getProperty("java.version");
    originalManifest = getMuleManifest().getManifest();
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
    Field field = DefaultMuleManifest.class.getDeclaredField("manifest");
    field.setAccessible(true);
    field.set(null, manifest);
  }

  @Test
  public void testIsSupportedJdkVersion() {
    // supported
    assertThat("Java version `" + getProperty("java.version") + "` not supported",
               isSupportedJdkVersion(), is(true));

    List<String> supported = asList("17.0.0",
                                    "17.0.11",
                                    "21.0.1",
                                    "21.0.3");

    for (String version : supported) {
      setJdkVersion(version);
      assertThat("Java version `" + version + "` not supported",
                 isSupportedJdkVersion(), is(true));
    }

    // not supported
    List<String> notSupported = asList("1.7.2",
                                       "1.7.2_12",
                                       "22.0.0");

    for (String version : notSupported) {
      setJdkVersion(version);
      assertThat("Java version `" + version + "` supported",
                 isSupportedJdkVersion(), is(false));
    }
  }

  @Test
  public void testUndefinedJdkPreferences() throws Exception {
    setJdkVersion("1.4.2");

    // not defined - blank
    setJdkPreferences("");
    assertThat(JdkVersionUtils.getRecommendedJdks(), emptyString());
    assertThat(JdkVersionUtils.getSupportedJdks(), emptyString());

    assertThat("Java version `" + getProperty("java.version") + "` not recommended",
               isRecommendedJdkVersion());
    assertThat("Java vendor `" + getProperty("java.vm.vendor") + "` not supported",
               JdkVersionUtils.isSupportedJdkVendor());
    assertThat("Java version `" + getProperty("java.version") + "` not supported",
               isSupportedJdkVersion());

    // not defined - null
    setJdkPreferences(null);
    assertThat(JdkVersionUtils.getRecommendedJdks(), nullValue());
    assertThat(JdkVersionUtils.getSupportedJdks(), nullValue());

    assertThat("Java version `" + getProperty("java.version") + "` not recommended",
               isRecommendedJdkVersion());
    assertThat("Java vendor `" + getProperty("java.vm.vendor") + "` not supported",
               JdkVersionUtils.isSupportedJdkVendor());
    assertThat("Java version `" + getProperty("java.version") + "` not supported",
               isSupportedJdkVersion());
  }

  private void setJdkPreferences(String preference) throws Exception {
    // mock the manifest (this is where the jdk preferences are taken from
    Manifest manifest = mock(Manifest.class);
    Attributes attributes = mock(Attributes.class);
    when(attributes.getValue(any(Attributes.Name.class))).thenReturn(preference);
    when(manifest.getMainAttributes()).thenReturn(attributes);

    setManifest(manifest);
  }

  @Test
  public void testSupportedJdkVendor() {
    assertThat("Java version `" + getProperty("java.version") + "` not supported",
               JdkVersionUtils.isSupportedJdkVendor(), is(true));
  }

  @Test
  public void testRecommendedJdkVersion() {
    // recommended
    List<String> recommended = asList("17.0.0",
                                      "17.0.11",
                                      "21.0.1",
                                      "21.0.3");

    for (String version : recommended) {
      setJdkVersion(version);
      assertThat("Java version `" + version + "` not recommended",
                 isRecommendedJdkVersion(), is(true));
    }

    // not recommended
    List<String> notRecommended = asList("1.4.2",
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
                                         "18.0.0",
                                         "22.0.0");

    for (String version : notRecommended) {
      setJdkVersion(version);
      assertThat("Java version `" + version + "` recommended",
                 isRecommendedJdkVersion(), is(false));
    }
  }

  @Test
  public void testJdkVersion() {
    JdkVersion jdkVersion = new JdkVersion("1.7");
    assertThat(jdkVersion.getMajor(), is(1));
    assertThat(jdkVersion.getMinor(), is(7));
    assertThat(jdkVersion.getMicro(), nullValue());
    assertThat(jdkVersion.getUpdate(), nullValue());
    assertThat(jdkVersion.getMilestone(), nullValue());

    jdkVersion = new JdkVersion("1.7.0-ea");
    assertThat(jdkVersion.getMajor(), is(1));
    assertThat(jdkVersion.getMinor(), is(7));
    assertThat(jdkVersion.getMicro(), is(0));
    assertThat(jdkVersion.getUpdate(), nullValue());
    assertThat(jdkVersion.getMilestone(), is("ea"));

    jdkVersion = new JdkVersion("1.6.0_29-b05");
    assertThat(jdkVersion.getMajor(), is(1));
    assertThat(jdkVersion.getMinor(), is(6));
    assertThat(jdkVersion.getMicro(), is(0));
    assertThat(jdkVersion.getUpdate(), is(29));
    assertThat(jdkVersion.getMilestone(), is("b05"));
  }

  @Test
  public void testJdkVersionComparison() {
    JdkVersion jdk1_3 = new JdkVersion("1.3");
    JdkVersion jdk1_6_0_5 = new JdkVersion("1.6.0_5");
    JdkVersion jdk1_7 = new JdkVersion("1.7");
    JdkVersion jdk1_6_0_29 = new JdkVersion("1.6.0_29");
    JdkVersion jdk1_6_0_29_b04 = new JdkVersion("1.6.0_29-b04");
    JdkVersion jdk1_6_0_29_b05 = new JdkVersion("1.6.0_29-b05");

    assertThat(jdk1_3, lessThan(jdk1_7));
    assertThat(jdk1_7, greaterThan(jdk1_3));
    assertThat(jdk1_3, comparesEqualTo(jdk1_3));
    assertThat(jdk1_6_0_29_b05, comparesEqualTo(jdk1_6_0_29_b05));

    assertThat(jdk1_6_0_5, lessThan(jdk1_6_0_29_b04));
    assertThat(jdk1_6_0_29_b04, greaterThan(jdk1_6_0_5));
    assertThat(jdk1_6_0_29, greaterThan(jdk1_6_0_5));
    assertThat(jdk1_6_0_5, lessThan(jdk1_6_0_29));
    assertThat(jdk1_6_0_29, lessThan(jdk1_6_0_29_b04));
    assertThat(jdk1_6_0_29_b04, greaterThan(jdk1_6_0_29));

    assertThat(jdk1_6_0_29_b04, lessThan(jdk1_6_0_29_b05));
    assertThat(jdk1_6_0_29_b05, greaterThan(jdk1_6_0_29_b04));

    assertThat(jdk1_6_0_29_b04, lessThan(jdk1_7));
    assertThat(jdk1_7, greaterThan(jdk1_6_0_29_b04));
  }

  @Test
  public void testValidateJdk() {
    JdkVersionUtils.validateJdk();
    setJdkVersion("11.0.0");
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testValidateJdk5() {
    setJdkVersion("1.5.1");
    JdkVersionUtils.validateJdk();
  }

  @Test(expected = java.lang.RuntimeException.class)
  public void testValidateJdk8() {
    setJdkVersion("1.8.0");
    JdkVersionUtils.validateJdk();
  }
}
