/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static java.sql.DriverManager.registerDriver;
import static java.util.Collections.list;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Driver;

import org.junit.Test;

@SmallTest
public class ResourceReleaserTestCase extends AbstractMuleTestCase {

  public static final String TEST_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/TestResourceReleaser.class";

  @Test
  public void createdByCorrectArtifactClassLoader() throws Exception {
    ensureResourceReleaserIsCreatedByCorrectClassLoader(new TestArtifactClassLoader(new TestArtifactClassLoader(Thread
        .currentThread().getContextClassLoader())));
  }

  @Test
  public void createdByCorrectParentArtifactClassLoader() throws Exception {
    ensureResourceReleaserIsCreatedByCorrectClassLoader(new TestArtifactClassLoader(Thread.currentThread()
        .getContextClassLoader()));
  }

  @Test
  public void notDeregisterJdbcDriversDifferentClassLoaders() throws Exception {
    Driver jdbcDriver = mock(Driver.class);
    TestArtifactClassLoader classLoader =
        new TestArtifactClassLoader(new TestArtifactClassLoader(Thread.currentThread().getContextClassLoader()));
    try {
      registerDriver(jdbcDriver);

      assertThat(list(getDrivers()), hasItem(jdbcDriver));
      classLoader.dispose();
      assertThat(list(getDrivers()), hasItem(jdbcDriver));
    } finally {
      deregisterDriver(jdbcDriver);
      classLoader.close();
    }
  }

  @Test
  public void deregisterJdbcDriversSameClassLoaders() throws Exception {
    Driver jdbcDriver = mock(Driver.class);
    registerDriver(jdbcDriver);

    assertThat(list(getDrivers()), hasItem(jdbcDriver));
    new DefaultResourceReleaser().release();
    assertThat(list(getDrivers()), not(hasItem(jdbcDriver)));
  }

  private void ensureResourceReleaserIsCreatedByCorrectClassLoader(MuleArtifactClassLoader classLoader) throws Exception {
    assertThat(classLoader.getClass().getClassLoader(), is(Thread.currentThread().getContextClassLoader()));
    classLoader.setResourceReleaserClassLocation(TEST_RESOURCE_RELEASER_CLASS_LOCATION);
    classLoader.dispose();

    // We must call the getClassLoader method from TestResourceReleaser dynamically in order to not load the
    // class by the current class loader, if not a java.lang.LinkageError is raised.
    ResourceReleaser resourceReleaserInstance = ((KeepResourceReleaserInstance) classLoader).getResourceReleaserInstance();
    Method getClassLoaderMethod = resourceReleaserInstance.getClass().getMethod("getClassLoader");
    ClassLoader resourceReleaserInstanceClassLoader = (ClassLoader) getClassLoaderMethod.invoke(resourceReleaserInstance);

    assertThat(resourceReleaserInstanceClassLoader, is((ClassLoader) classLoader));
  }

  private interface KeepResourceReleaserInstance {

    ResourceReleaser getResourceReleaserInstance();
  }

  private static class TestArtifactClassLoader extends MuleArtifactClassLoader implements KeepResourceReleaserInstance {

    private ResourceReleaser resourceReleaserInstance;

    public TestArtifactClassLoader(ClassLoader parentCl) {
      super("testArtifact", new URL[0], parentCl, mock(ClassLoaderLookupPolicy.class));
    }

    @Override
    protected ResourceReleaser createResourceReleaserInstance() {
      resourceReleaserInstance = super.createResourceReleaserInstance();
      return resourceReleaserInstance;
    }

    @Override
    public ResourceReleaser getResourceReleaserInstance() {
      return resourceReleaserInstance;
    }
  }
}

