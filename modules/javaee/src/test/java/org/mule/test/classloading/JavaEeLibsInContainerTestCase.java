/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;

import org.mule.functional.junit4.ArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@ArtifactClassLoaderRunnerConfig(
    applicationRuntimeLibs = {"org.apache.commons:commons-lang3"},
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"},
    providedExclusions = {
        "org.mule.tests:*:*:*:*",
    },
    testExclusions = {
        "org.mule.runtime:*:*:*:*",
        "org.mule.tests.plugin:*:*:*:*",
        // force Java EE libs to be loaded from the container
        "*:jakarta.activation:*:*:*",
        "*:jakarta.annotation-api:*:*:*",
        "*:jakarta.jws-api:*:*:*",
        "*:jakarta.xml.bind-api:*:*:*",
        "*:jaxb-impl:*:*:*",
        "*:jakarta.xml.soap-api:*:*:*",
        "*:jakarta.xml.ws-api:*:*:*",
        "*:jakarta.resource-api:*:*:*"
    },
    testInclusions = {
        "*:*:jar:tests:*",
        "*:*:test-jar:*:*"
    })
public class JavaEeLibsInContainerTestCase extends ArtifactFunctionalTestCase {

  private ClassLoader testRegionClassLoader;

  @Rule
  public final ExpectedException expected = ExpectedException.none();

  public JavaEeLibsInContainerTestCase() {
    if (isJavaVersionAtLeast(JAVA_17)) {
      expected.expect(ClassNotFoundException.class);
    }
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[0];
  }

  @Before
  public void obtainTestClassLoader() {
    this.testRegionClassLoader = this.getClass().getClassLoader().getParent();
  }

  @Test
  public void activation() throws ClassNotFoundException {
    testRegionClassLoader.loadClass("javax.activation.DataSource");
  }

  @Test
  public void jaxbImpl() throws ClassNotFoundException {
    testRegionClassLoader.loadClass("com.sun.xml.bind.Utils");
  }

  @Test
  public void istack() throws ClassNotFoundException {
    testRegionClassLoader.loadClass("com.sun.istack.Pool");
  }
}
