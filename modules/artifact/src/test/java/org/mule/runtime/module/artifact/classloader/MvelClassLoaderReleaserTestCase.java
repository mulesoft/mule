/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.mvel2.optimizers.impl.asm.ASMAccessorOptimizer.getMVELClassLoader;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_COMPATIBILITY;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.mule.module.artifact.classloader.MvelClassLoaderReleaser;
import org.mule.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(LEAK_PREVENTION)
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(SUPPORT_MVEL_COMPATIBILITY)})
@Issue("W-11785664")
public class MvelClassLoaderReleaserTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;

  @Test
  public void classLoaderReferencesInMelIsCleared() {
    MuleArtifactClassLoader appClassLoader = new TestArtifactClassLoader(MvelClassLoaderReleaserTestCase.class.getClassLoader());

    withContextClassLoader(appClassLoader, () ->
    // MVEL optimizer initialization that sets the reference to the classloader.
    new DynamicOptimizer().init());

    PhantomReference<MuleArtifactClassLoader> clRef = new PhantomReference<>(appClassLoader, new ReferenceQueue<>());
    new MvelClassLoaderReleaser(appClassLoader).release();

    appClassLoader = null;

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(clRef.isEnqueued(), is(true));
      return true;
    }));
  }

  @Test
  public void otherClassLoaderReferencesInMvelIsKept() {
    MuleArtifactClassLoader firstAppClassLoader =
        new TestArtifactClassLoader(MvelClassLoaderReleaserTestCase.class.getClassLoader());
    MuleArtifactClassLoader otherAppClassLoader =
        new TestArtifactClassLoader(MvelClassLoaderReleaserTestCase.class.getClassLoader());

    withContextClassLoader(firstAppClassLoader, () ->
    // MVEL optimizer initialization that sets the reference to the classloader.
    new DynamicOptimizer().init());

    new MvelClassLoaderReleaser(otherAppClassLoader).release();

    assertThat(((ClassLoader) getMVELClassLoader()).getParent(), sameInstance(firstAppClassLoader));
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
  }
}
