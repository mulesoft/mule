/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Thread.currentThread;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.MulePluginClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.hamcrest.core.Is;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

/**
 * MySql's resource cleaner test. The mysql-driver-v5/8.jar files where created to mock the cleanup thread package in both
 * versions.
 */
@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
@RunWith(Parameterized.class)
public class MySqlDriverLookupTestCase extends AbstractMuleTestCase {

  private static final String MYSQL_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/MySqlTestResourceReleaser.class";

  private final String classnameBeingTested;

  // Used to verify the connection cleaner class package found
  public static String foundClassname;
  private final String mySqlDriverJarname;
  private final ClassLoaderLookupPolicy testLookupPolicy;

  // Parameterized
  public MySqlDriverLookupTestCase(String cleanupThreadClassname, String jarName) {
    classnameBeingTested = cleanupThreadClassname;
    mySqlDriverJarname = jarName;

    testLookupPolicy = CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
  }

  @Before
  public void setUp() throws Exception {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), Is.is(false));
    foundClassname = "Wrong one";
  }

  @Parameterized.Parameters(name = "Testing classname {0}")
  public static Object[][] data() throws NoSuchFieldException, IllegalAccessException {
    return new Object[][] {
        {"com.mysql.jdbc.AbandonedConnectionCleanupThread", "mysql/mysql-driver-v5.jar"},
        {"com.mysql.cj.jdbc.AbandonedConnectionCleanupThread", "mysql/mysql-driver-v8.jar"}
    };
  }

  @Test
  public void testMySqlDriverCleanupThreadClassIsFound() throws ClassNotFoundException, IOException {
    try (MulePluginClassLoader artifactClassLoader =
        new MulePluginClassLoader("test", mock(ArtifactPluginDescriptor.class),
                                  new URL[] {ClassUtils.getResource(mySqlDriverJarname, this.getClass())},
                                  currentThread().getContextClassLoader(), testLookupPolicy)) {
      artifactClassLoader.setResourceReleaserClassLocation(MYSQL_RESOURCE_RELEASER_CLASS_LOCATION);
      // Force to load a Driver class so the jdbc resource releaser is created and executed
      artifactClassLoader.loadClass(TestDriver.class.getName());

      artifactClassLoader.dispose();
      assertThat(foundClassname, is(classnameBeingTested));
    }

  }
}
