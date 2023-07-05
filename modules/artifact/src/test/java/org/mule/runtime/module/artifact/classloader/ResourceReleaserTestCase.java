/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.PARENT_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Thread.currentThread;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static java.sql.DriverManager.registerDriver;
import static java.util.Arrays.asList;
import static java.util.Collections.list;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import org.mule.module.artifact.classloader.JdbcResourceReleaser;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.runtime.module.artifactapi.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.MulePluginClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Driver;
import java.util.Collection;
import java.util.function.Function;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
@SmallTest
@RunWith(Parameterized.class)
@Issue("W-12204790")
public class ResourceReleaserTestCase extends AbstractMuleTestCase {

  public static final String TEST_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/TestResourceReleaser.class";

  @Parameters
  public static Collection<Function<ClassLoader, MuleArtifactClassLoader>> params() {
    return asList(TestPluginClassLoader::new,
                  // Libraries/drivers for which there is specific releaser code may be at the application/domain/policy level
                  // rather than the plugin if they are added through `sharedLibraries` rather that
                  // `additionalPluginDependencies`.
                  TestApplicationClassLoader::new);
  }

  private final Function<ClassLoader, MuleArtifactClassLoader> classLoaderFactory;

  public ResourceReleaserTestCase(Function<ClassLoader, MuleArtifactClassLoader> classLoaderFactory) {
    this.classLoaderFactory = classLoaderFactory;
  }

  @Before
  public void setup() throws Exception {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), Is.is(false));
  }

  @Test
  public void createdByCorrectArtifactClassLoader() throws Exception {
    ensureResourceReleaserIsCreatedByCorrectClassLoader(new TestPluginClassLoader(classLoaderFactory.apply(currentThread()
        .getContextClassLoader())));
  }

  @Test
  public void createdByCorrectParentArtifactClassLoader() throws Exception {
    ensureResourceReleaserIsCreatedByCorrectClassLoader(classLoaderFactory.apply(currentThread().getContextClassLoader()));
  }

  @Test
  public void jdbcResourceReleaserShouldNotBeCreatedIfDriverIsNotLoaded() {
    MuleArtifactClassLoader testArtifactClassLoader = classLoaderFactory.apply(currentThread().getContextClassLoader());
    testArtifactClassLoader.setResourceReleaserClassLocation(TEST_RESOURCE_RELEASER_CLASS_LOCATION);
    testArtifactClassLoader.dispose();

    // We must call the getClassLoader method from TestResourceReleaser dynamically in order to not load the
    // class by the current class loader, if not a java.lang.LinkageError is raised.
    ResourceReleaser jdbcResourceReleaserInstance =
        ((KeepResourceReleaserInstance) testArtifactClassLoader).getResourceReleaserInstance();
    assertThat(jdbcResourceReleaserInstance, is(nullValue()));
  }

  @Test
  public void jdbcResourceReleaserShouldNotBeCreatedIfDriverInterfaceIsLoaded() throws ClassNotFoundException {
    MuleArtifactClassLoader testArtifactClassLoader = classLoaderFactory.apply(currentThread().getContextClassLoader());
    testArtifactClassLoader.setResourceReleaserClassLocation(TEST_RESOURCE_RELEASER_CLASS_LOCATION);

    testArtifactClassLoader.loadClass(Driver.class.getName());
    testArtifactClassLoader.loadClass(TestAbstractDriver.class.getName());
    testArtifactClassLoader.loadClass(TestInterfaceDriver.class.getName());

    testArtifactClassLoader.dispose();

    // We must call the getClassLoader method from TestResourceReleaser dynamically in order to not load the
    // class by the current class loader, if not a java.lang.LinkageError is raised.
    ResourceReleaser jdbcResourceReleaserInstance =
        ((KeepResourceReleaserInstance) testArtifactClassLoader).getResourceReleaserInstance();
    assertThat(jdbcResourceReleaserInstance, is(nullValue()));
  }

  @Test
  public void notDeregisterJdbcDriversDifferentClassLoaders() throws Exception {
    Driver jdbcDriver = mock(Driver.class);
    TestPluginClassLoader classLoader =
        new TestPluginClassLoader(classLoaderFactory.apply(currentThread().getContextClassLoader()));
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
    new JdbcResourceReleaser().release();
    assertThat(list(getDrivers()), not(hasItem(jdbcDriver)));
  }

  @Test
  public void releaserCanBeInvokedMultipleTimes() throws Exception {
    Driver jdbcDriver = mock(Driver.class);
    registerDriver(jdbcDriver);

    assertThat(list(getDrivers()), hasItem(jdbcDriver));
    new JdbcResourceReleaser().release();
    new JdbcResourceReleaser().release();
    assertThat(list(getDrivers()), not(hasItem(jdbcDriver)));
  }

  private void ensureResourceReleaserIsCreatedByCorrectClassLoader(MuleArtifactClassLoader classLoader) throws Exception {
    assertThat(classLoader.getClass().getClassLoader(), is(currentThread().getContextClassLoader()));
    classLoader.setResourceReleaserClassLocation(TEST_RESOURCE_RELEASER_CLASS_LOCATION);

    // Have to force run the jdbc resource releaser by loading a class that is assignable to Driver
    classLoader.loadClass(TestDriver.class.getName());

    classLoader.dispose();

    // We must call the getClassLoader method from TestResourceReleaser dynamically in order to not load the
    // class by the current class loader, if not a java.lang.LinkageError is raised.
    ResourceReleaser resourceReleaserInstance = ((KeepResourceReleaserInstance) classLoader).getResourceReleaserInstance();
    Method getClassLoaderMethod = resourceReleaserInstance.getClass().getMethod("getClassLoader");
    ClassLoader resourceReleaserInstanceClassLoader = (ClassLoader) getClassLoaderMethod.invoke(resourceReleaserInstance);

    assertThat(resourceReleaserInstanceClassLoader, is(classLoader));
  }

  @Test
  public void createsInstanceOnlyOnce() throws IOException {
    try (MuleArtifactClassLoader testArtifactClassLoader = classLoaderFactory.apply(currentThread().getContextClassLoader())) {
      ResourceReleaser firstInstance = ((KeepResourceReleaserInstance) testArtifactClassLoader).createResourceReleaserInstance();
      ResourceReleaser secondInstance = ((KeepResourceReleaserInstance) testArtifactClassLoader).createResourceReleaserInstance();

      assertThat(firstInstance, sameInstance(secondInstance));
    }
  }

  private interface KeepResourceReleaserInstance {

    ResourceReleaser getResourceReleaserInstance();

    ResourceReleaser createResourceReleaserInstance();

  }

  private static class TestPluginClassLoader extends MulePluginClassLoader implements KeepResourceReleaserInstance {

    private ResourceReleaser resourceReleaserInstance;

    public TestPluginClassLoader(ClassLoader parentCl) {
      super("testId", new ArtifactDescriptor("test"), new URL[0], parentCl, PARENT_FIRST_CLASSLOADER_LOOKUP_POLICY);
    }

    @Override
    public ResourceReleaser createResourceReleaserInstance() {
      resourceReleaserInstance = super.createResourceReleaserInstance();
      return resourceReleaserInstance;
    }

    @Override
    public ResourceReleaser getResourceReleaserInstance() {
      return resourceReleaserInstance;
    }
  }

  private static class TestApplicationClassLoader extends MuleDeployableArtifactClassLoader
      implements KeepResourceReleaserInstance {

    private ResourceReleaser resourceReleaserInstance;

    public TestApplicationClassLoader(ClassLoader parentCl) {
      super("testId", new ArtifactDescriptor("test"), new URL[0], parentCl, PARENT_FIRST_CLASSLOADER_LOOKUP_POLICY);
    }

    @Override
    public ResourceReleaser createResourceReleaserInstance() {
      resourceReleaserInstance = super.createResourceReleaserInstance();
      return resourceReleaserInstance;
    }

    @Override
    public ResourceReleaser getResourceReleaserInstance() {
      return resourceReleaserInstance;
    }
  }
}

