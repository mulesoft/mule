/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.mule.module.artifact.classloader.ScalaClassValueReleaser;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
@Issue("MULE-18288")
public class ScalaClassValueReleaserTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;

  private MuleArtifactClassLoader classLoader = TestArtifactClassLoader.createFrom(this.getClass());

  @Before
  public void setup() throws Exception {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), is(false));
  }

  @Test
  public void classValueMapsAreCleared() {
    computeClassValueRetainingClassLoader();
    classLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  @Test
  public void releaserCanBeCalledMultipleTimes() {
    computeClassValueRetainingClassLoader();
    new ScalaClassValueReleaser().release();
    classLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  private void computeClassValueRetainingClassLoader() {
    RetainingClassLoaderClassValue testClassValue = new RetainingClassLoaderClassValue(classLoader);
    testClassValue.get(String.class);
    testClassValue = null;
  }

  private void assertClassLoaderIsEnqueued() {
    PhantomReference<ClassLoader> artifactClassLoaderRef = new PhantomReference<>(classLoader, new ReferenceQueue<>());
    classLoader = null;

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(artifactClassLoaderRef.isEnqueued(), is(true));
      return true;
    }));
  }

  private static class RetainingClassLoaderClassValue extends ClassValue<Object> {

    private final ClassLoader classLoaderToRetain;

    private RetainingClassLoaderClassValue(ClassLoader classLoaderToRetain) {
      this.classLoaderToRetain = classLoaderToRetain;
    }

    @Override
    protected Object computeValue(Class type) {
      try {
        return instantiateClass(classLoaderToRetain.loadClass(ClassLoaderRetainingClass.class.getName()));
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class TestArtifactClassLoader extends MuleDeployableArtifactClassLoader {

    public static TestArtifactClassLoader createFrom(Class<?> testClass) {
      return new TestArtifactClassLoader(testClass.getClassLoader(),
                                         new URL[] {testClass.getProtectionDomain().getCodeSource().getLocation()});
    }

    private TestArtifactClassLoader(ClassLoader parentCl, URL[] urls) {
      super("testId", new ArtifactDescriptor("test"), urls, parentCl, CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY);
    }
  }

  public static class ClassLoaderRetainingClass {
    // No need to do anything
  }

}
