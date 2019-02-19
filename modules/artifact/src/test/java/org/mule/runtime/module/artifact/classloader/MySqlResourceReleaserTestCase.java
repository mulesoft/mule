/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public class MySqlResourceReleaserTestCase extends AbstractMuleTestCase {

  private static final String MYSQL_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/MySqlTestResourceReleaser.class";

  private final String classnameBeingTested;

  // Used to verify the connection cleaner class package found
  public static String foundClassname;
  private final String mySqlDriverJarname;
  private ClassLoaderLookupPolicy testLookupPolicy;

  // Parameterized
  public MySqlResourceReleaserTestCase(String cleanupThreadClassname, String jarName) {
    classnameBeingTested = cleanupThreadClassname;
    mySqlDriverJarname = jarName;

    testLookupPolicy = new ClassLoaderLookupPolicy() {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        return CHILD_FIRST;
      }

      @Override
      public LookupStrategy getPackageLookupStrategy(String packageName) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
        return null;
      }
    };
  }

  @Before
  public void setUp() throws Exception {
    foundClassname = "Wrong one";
  }

  @Parameterized.Parameters(name = "Testing classname {0}")
  public static Object[][] data() throws NoSuchFieldException, IllegalAccessException {
    return new Object[][]{
            {"com.mysql.jdbc.AbandonedConnectionCleanupThread", "mysql/mysql-driver-v5.jar"},
            {"com.mysql.cj.jdbc.AbandonedConnectionCleanupThread", "mysql/mysql-driver-v5.jar"}
    };
  }

  @Test
  public void testMySqlDriverCleanupThreadClassIsFound() throws ClassNotFoundException {
    MuleArtifactClassLoader artifactClassLoader =
        spy(new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class),
                                    new URL[] {ClassUtils.getResource(mySqlDriverJarname, this.getClass())},
                                    Thread.currentThread().getContextClassLoader(), testLookupPolicy));
    doAnswer((Answer) invocationOnMock -> {
      String classBeingLoaded = invocationOnMock.getArgument(0);
        if (classBeingLoaded.equals(classnameBeingTested) || classBeingLoaded.equals(MYSQL_RESOURCE_RELEASER_CLASS_LOCATION)) {
            return invocationOnMock.callRealMethod();
        } else {
          return null;
        }
    }).when(artifactClassLoader).loadClass(anyString());

    artifactClassLoader.setResourceReleaserClassLocation(MYSQL_RESOURCE_RELEASER_CLASS_LOCATION);
    artifactClassLoader.dispose();
    assertThat(foundClassname, is(classnameBeingTested));
  }
}
