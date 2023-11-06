/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.classloader.DependencyResolver.getDependencyFromMaven;
import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Class.forName;
import static java.lang.System.gc;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.aspectj.weaver.loadtime.Aj;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(LEAK_PREVENTION)
@RunWith(Parameterized.class)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class GroovyResourceReleaserTestCase extends AbstractMuleTestCase {

  private static List<String> oldAjLoadersToSkip;

  @BeforeClass
  public static void avoidAspectjAgentLeak() {
    oldAjLoadersToSkip = Aj.loadersToSkip;
    Aj.loadersToSkip = singletonList(MuleArtifactClassLoader.class.getName());
  }

  @AfterClass
  public static void restoreAjLoadersToSkip() {
    Aj.loadersToSkip = oldAjLoadersToSkip;
  }

  private static final int PROBER_POLLING_INTERVAL = 150;
  private static final int PROBER_POLLING_TIMEOUT = 6000;
  private static final String GROOVY_ARTIFACT_ID = "groovy";
  private static final String GROOVY_GROUP_ID = "org.codehaus.groovy";
  private static final String GROOVY_SCRIPT_ENGINE = "groovy.util.GroovyScriptEngine";
  private static final String GROOVY_LANG_BINDING = "groovy.lang.Binding";

  private final String groovyVersion;
  private final ClassLoaderLookupPolicy testLookupPolicy;
  private MuleArtifactClassLoader artifactClassLoader = null;

  public GroovyResourceReleaserTestCase(String groovyVersion) {
    this.groovyVersion = groovyVersion;
    this.testLookupPolicy = CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
  }

  @Parameterized.Parameters(name = "Testing artifact {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "2.4.21",
        "2.5.22",
        "3.0.19"
    };
  }

  @Before
  public void setup() throws Exception {
    artifactClassLoader =
        new MuleArtifactClassLoader("GroovyResourceReleaserTestCase",
                                    mock(ArtifactDescriptor.class),
                                    new URL[] {getDependencyFromMaven(GROOVY_GROUP_ID, GROOVY_ARTIFACT_ID, groovyVersion)},
                                    currentThread().getContextClassLoader(),
                                    testLookupPolicy);
  }

  @Test
  public void runGroovyScriptAndDispose() throws ReflectiveOperationException {
    assertEquals("TEST", runScript());
    artifactClassLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  private String runScript() throws ReflectiveOperationException {
    URL[] roots = new URL[] {artifactClassLoader.getResource("groovy/example.groovy")};
    Class<?> groovyScriptEngineClass = forName(GROOVY_SCRIPT_ENGINE, true, artifactClassLoader);
    Object scriptEngine =
        groovyScriptEngineClass.getConstructor(URL[].class, ClassLoader.class).newInstance(roots, artifactClassLoader);
    Class<?> groovyBinding = forName(GROOVY_LANG_BINDING, true, artifactClassLoader);
    Method runMethod = groovyScriptEngineClass.getMethod("run", String.class, groovyBinding);
    String scriptBody = "example.groovy";
    return (String) runMethod.invoke(scriptEngine, scriptBody, groovyBinding.getConstructor().newInstance());
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
