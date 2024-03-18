/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.tck.junit4.rule.LogCleanup.clearAllLogs;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.util.Locale.getDefault;
import static java.util.ResourceBundle.getBundle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.module.artifact.classloader.ClassLoaderResourceReleaser;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

/**
 * Tests for the cleanup of ResourceBundle caches when a {@link MuleArtifactClassLoader} is disposed of.
 * <p>
 * These caches hold weak references to the ClassLoaders in the keys (which are not a problem), but also soft references to the
 * {@link ResourceBundle} classes in the cache values. These soft references can become a problem if there is too much pressure on
 * the GC.
 *
 * @see ClassLoaderResourceReleaser
 * @see ResourceBundle#clearCache(ClassLoader)
 */
@Feature(LEAK_PREVENTION)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
@Issue("MULE-10643")
public class ResourceBundleCleanupTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;

  @Test
  public void whenClassLoaderIsDisposedThenResourceBundlesDoNotRetainIt() {
    TestClassLoader classLoader = new TestClassLoader(this.getClass());

    // This ResourceBundle class will be loaded with our TestClassLoader
    ResourceBundle resourceBundle = getBundle(TestResourceBundle.class.getName(), getDefault(), classLoader);
    assertThat(resourceBundle.getClass().getClassLoader(), is(sameInstance(classLoader)));
    resourceBundle = null;

    classLoader.dispose();

    PhantomReference<ArtifactClassLoader> reference = new PhantomReference<>(classLoader, new ReferenceQueue<>());
    classLoader = null;
    assertClassLoaderIsCollectable(reference);
  }

  @Test
  public void whenClassLoaderIsDisposedThenResourceBundleCachesAreInvalidated() {
    TestClassLoader classLoader = new TestClassLoader(this.getClass());

    // Gets the same ResourceBundle twice
    getBundle(TestResourceBundle.class.getName(), getDefault(), classLoader);
    getBundle(TestResourceBundle.class.getName(), getDefault(), classLoader);

    // Unsuccessful ResourceBundle lookups are also cached
    tryGetNonexistentBundle(classLoader);
    tryGetNonexistentBundle(classLoader);

    // Control tests to see we are not lying about the caching
    assertThat("Expected bundle resource loading attempts to be cached by ResourceBundle class",
               classLoader.getResourceLoadAttempts("aBundle.properties"), is(1));
    assertThat("Expected bundle resource loading attempts to be cached by ResourceBundle class",
               classLoader.getResourceLoadAttempts(TestResourceBundle.class.getName()), is(1));

    classLoader.dispose();

    getBundle(TestResourceBundle.class.getName(), getDefault(), classLoader);
    assertThat("Expected a miss on the ResourceBundle cache",
               classLoader.getResourceLoadAttempts(TestResourceBundle.class.getName()), is(2));

    tryGetNonexistentBundle(classLoader);
    assertThat("Expected a miss on the ResourceBundle cache",
               classLoader.getResourceLoadAttempts("aBundle.properties"), is(2));
  }

  @Test
  public void whenClassLoaderIsDisposedThenResourceBundlesCachesFromOtherClassLoadersAreNotAffected() {
    TestClassLoader classLoader = new TestClassLoader(this.getClass());
    TestClassLoader otherClassLoader = new TestClassLoader(this.getClass());

    getBundle(TestResourceBundle.class.getName(), getDefault(), classLoader);
    getBundle(TestResourceBundle.class.getName(), getDefault(), otherClassLoader);

    classLoader.dispose();

    getBundle(TestResourceBundle.class.getName(), getDefault(), classLoader);
    assertThat("Expected a miss on the ResourceBundle cache",
               classLoader.getResourceLoadAttempts(TestResourceBundle.class.getName()), is(2));
    getBundle(TestResourceBundle.class.getName(), getDefault(), otherClassLoader);
    assertThat("Expected a hit on the ResourceBundle cache",
               otherClassLoader.getResourceLoadAttempts(TestResourceBundle.class.getName()), is(1));

    otherClassLoader.dispose();
  }

  private void tryGetNonexistentBundle(ClassLoader classLoader) {
    try {
      getBundle("aBundle", getDefault(), classLoader);
      fail("Found a bundle that is not supposed to present for this test");
    } catch (MissingResourceException e) {
      // Expected
    }
  }

  private void assertClassLoaderIsCollectable(PhantomReference<ArtifactClassLoader> classLoaderReference) {
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      clearAllLogs();
      System.gc();
      assertThat(classLoaderReference.isEnqueued(), is(true));
      return true;
    }));
  }

  private static class TestClassLoader extends MuleArtifactClassLoader {

    // Using a Spy with call verifications does not work because the ClassLoader of the unnamed Module ends up being the original
    // one
    private final Map<String, Integer> resourceLoadAttempts = new ConcurrentHashMap<>();

    public TestClassLoader(Class<?> testClass) {
      super("testId",
            new ArtifactDescriptor("test"),
            // Adds the Jar file of the test case to this CL, so it can load classes itself instead of asking the parent CL.
            new URL[] {testClass.getProtectionDomain().getCodeSource().getLocation()},
            testClass.getClassLoader(),
            CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      resourceLoadAttempts.compute(name, this::increaseLoadAttempts);
      return super.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
      resourceLoadAttempts.compute(name, this::increaseLoadAttempts);
      return super.getResource(name);
    }

    public int getResourceLoadAttempts(String name) {
      return resourceLoadAttempts.getOrDefault(name, 0);
    }

    private Integer increaseLoadAttempts(String unused, Integer currentLoadAttempts) {
      return (currentLoadAttempts == null) ? 1 : currentLoadAttempts + 1;
    }
  }
}

