/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.runtime.module.artifact.api.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.Map;
import java.util.stream.Stream;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(LEAK_PREVENTION)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
@Issue("W-12512289")
public class ClassLoaderDisposeErrorTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;

  @Test
  public void disposingContinuesAfterErrorWhileReleasingResources() throws ClassNotFoundException {
    MuleArtifactClassLoader artifactClassLoader =
        new TestArtifactClassLoader(MvelClassLoaderReleaserTestCase.class.getClassLoader());

    artifactClassLoader.loadClass(TestDriver.class.getName());

    ShutdownListener shutdownListener = new ShutdownListener() {

      @Override
      public void execute() {
        // Nothing to do
      }
    };

    artifactClassLoader.addShutdownListener(shutdownListener);
    PhantomReference<ShutdownListener> shutdownListenerRef = new PhantomReference<>(shutdownListener, new ReferenceQueue<>());

    artifactClassLoader.dispose();

    shutdownListener = null;

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(shutdownListenerRef.isEnqueued(), is(true));
      return true;
    }));
  }

  private static class TestArtifactClassLoader extends MuleArtifactClassLoader {

    public TestArtifactClassLoader(ClassLoader parentCl) {
      super("testId", new ArtifactDescriptor("test"), new URL[0], parentCl, new ClassLoaderLookupPolicy() {

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

        @Override
        public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy) {
          return null;
        }

        @Override
        public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
          return null;
        }

        @Override
        public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy, boolean overwrite) {
          return null;
        }
      });
    }

    @Override
    protected ResourceReleaser createResourceReleaserInstance() {
      throw new Error("Error while creating resource releaser instance");
    }
  }

}
