/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import static org.mule.runtime.jpms.api.JpmsUtils.exploreJdkModules;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_GENERATION;

import static org.apache.commons.lang3.JavaVersion.JAVA_11;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.assumeTrue;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(CLASSLOADER_GENERATION)
public class JpmsUtilsTestCase extends AbstractMuleTestCase {

  @BeforeClass
  public static void checkJdkVersion() {
    assumeTrue("This test tests functionality that applies only when running on Java 11+", isJavaVersionAtLeast(JAVA_11));
  }

  @Test
  @Issue("W-13565514")
  @Description("Assert that no other packages from modules in the bootLoader are taken as jre packages.")
  public void jrePackagesExclusively() {
    final Set<String> jrePackages = new HashSet<>();
    exploreJdkModules(jrePackages);

    assertThat(jrePackages, everyItem(anyOf(startsWith("jdk."), startsWith("java."))));
  }
}
