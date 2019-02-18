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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.module.artifact.classloader.DefaultResourceReleaser;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MySqlResourceReleaserTestCase extends AbstractMuleTestCase {

  private final String testingClassname;

  public static String foundClassname = "";

  public MySqlResourceReleaserTestCase(String cleanupThreadClassname) {
    testingClassname = cleanupThreadClassname;
  }

  @Parameterized.Parameters(name = "Testing classname {0}")
  public static Collection<Object[]> data() throws NoSuchFieldException, IllegalAccessException {
    Field connectionsCleanerThreadClassnames =
        DefaultResourceReleaser.class.getDeclaredField("CONNECTION_CLEANUP_THREAD_KNOWN_CLASS_ADDRESES");
    connectionsCleanerThreadClassnames.setAccessible(true);
    Collection<String> cleanerThreadClassNames =
        (Collection<String>) connectionsCleanerThreadClassnames.get(DefaultResourceReleaser.class);
    return cleanerThreadClassNames.stream()
        .map(threadClassname -> new Object[] {threadClassname})
        .collect(Collectors.toCollection(() -> new ArrayList<>()));
  }

  public static final String MYSQL_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/MySqlTestResourceReleaser.class";

  private String classPackageFound;

  @Test
  public void testMySql5DriverCleanupThreadClassIsFound() throws ClassNotFoundException {
    ClassLoaderLookupPolicy lookupPolicyMock = mock(ClassLoaderLookupPolicy.class);
    when(lookupPolicyMock.getClassLookupStrategy(anyString())).thenReturn(mock(LookupStrategy.class));

    MuleArtifactClassLoader artifactClassLoader =
        spy(new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class), new URL[] {ClassUtils.getResource("mysql/mysql-driver-v5.jar",this.getClass())}, Thread.currentThread().getContextClassLoader(), new ClassLoaderLookupPolicy() {

          @Override
          public LookupStrategy getClassLookupStrategy(String className) {
            return PARENT_FIRST;
          }

          @Override
          public LookupStrategy getPackageLookupStrategy(String packageName) {
            return null;
          }

          @Override
          public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
            return null;
          }
        }));

    artifactClassLoader.setResourceReleaserClassLocation(MYSQL_RESOURCE_RELEASER_CLASS_LOCATION);

/*    doAnswer((Answer<Class>) invocationOnMock -> {
      Class driverClass = mock(Class.class);
      when(driverClass.getCanonicalName()).thenReturn("perro");
      return driverClass;
    }).when(artifactClassLoader).loadClass(anyString());*/

    artifactClassLoader.dispose();
    assertThat(foundClassname, is(testingClassname));
  }

}
