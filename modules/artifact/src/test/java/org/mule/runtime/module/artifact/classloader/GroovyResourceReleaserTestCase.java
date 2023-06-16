/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.core.api.util.ClassUtils.getFieldValue;
import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Class.forName;
import static java.lang.System.gc;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(LEAK_PREVENTION)
@RunWith(Parameterized.class)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class GroovyResourceReleaserTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 150;
  private static final int PROBER_POLLING_TIMEOUT = 6000;
  private final static String GROOVY_ARTIFACT_ID = "groovy";
  private final static String GROOVY_GROUP_ID = "org.codehaus.groovy";
  private static final String GROOVY_SCRIPT_ENGINE = "groovy.util.GroovyScriptEngine";
  private static final String GROOVY_LANG_BINDING = "groovy.lang.Binding";

  String groovyVersion;
  private final ClassLoaderLookupPolicy testLookupPolicy;
  MuleArtifactClassLoader artifactClassLoader = null;

  public GroovyResourceReleaserTestCase(String groovyVersion) {
    this.groovyVersion = groovyVersion;
    this.testLookupPolicy = new ClassLoaderLookupPolicy() {

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

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
        return null;
      }
    };
  }

  @Parameterized.Parameters(name = "Testing artifact {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "2.4.21",
        "2.5.22",
        "3.0.0",
        "3.0.17"
    };
  }

  @Before
  public void setup() throws Exception {

    URL settingsUrl = getClass().getClassLoader().getResource("custom-settings.xml");
    final MavenClientProvider mavenClientProvider = discoverProvider(this.getClass().getClassLoader());

    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
        newMavenConfigurationBuilder().globalSettingsLocation(toFile(settingsUrl));

    MavenClient mavenClient = mavenClientProvider
        .createMavenClient(mavenConfigurationBuilder.localMavenRepositoryLocation(localMavenRepository.get()).build());

    BundleDescriptor bundleDescriptor = new BundleDescriptor.Builder().setGroupId(GROOVY_GROUP_ID)
        .setArtifactId(GROOVY_ARTIFACT_ID).setVersion(groovyVersion).build();

    BundleDependency dependency = mavenClient.resolveBundleDescriptor(bundleDescriptor);
    artifactClassLoader =
        new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class),
                                    new URL[] {dependency.getBundleUri().toURL()}, currentThread().getContextClassLoader(),
                                    testLookupPolicy);
  }

  @Test
  public void runGroovyScriptAndDispose() throws ClassNotFoundException,
      NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
    assertFalse(getFieldValue(artifactClassLoader, "shouldReleaseGroovyReferences", false));
    assertEquals("TEST", runScript());
    assertTrue(getFieldValue(artifactClassLoader, "shouldReleaseGroovyReferences", false));
    artifactClassLoader.dispose();
    artifactClassLoader = null;
    gc();
  }

  private String runScript() throws ClassNotFoundException,
      NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    URL[] roots = new URL[] {artifactClassLoader.getResource("groovy/example.groovy")};
    Class<?> groovyScriptEngineClass = forName(GROOVY_SCRIPT_ENGINE, true, artifactClassLoader);
    Object scriptEngine =
        groovyScriptEngineClass.getConstructor(URL[].class, ClassLoader.class).newInstance(roots, artifactClassLoader);
    Class<?> groovyBinding = forName(GROOVY_LANG_BINDING, true, artifactClassLoader);
    Method runMethod = groovyScriptEngineClass.getMethod("run", String.class, groovyBinding);
    String scriptBody = "example.groovy";
    return (String) runMethod.invoke(scriptEngine, scriptBody, groovyBinding.getConstructor().newInstance());
  }

  @Test
  public void releaseClassloader() {
    try {
      runScript();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    artifactClassLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  private void assertClassLoaderIsEnqueued() {
    PhantomReference<ClassLoader> artifactClassLoaderRef = new PhantomReference<>(artifactClassLoader, new ReferenceQueue<>());
    artifactClassLoader = null;
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      gc();
      assertThat(artifactClassLoaderRef.isEnqueued(), is(true));
      return true;
    }));
  }
}
