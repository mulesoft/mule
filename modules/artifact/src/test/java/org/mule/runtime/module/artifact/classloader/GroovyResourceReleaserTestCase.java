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
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.tck.util.CollectableReference.collectedByGc;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Class.forName;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.util.CollectableReference;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;
import java.net.URLClassLoader;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.aspectj.weaver.loadtime.Aj;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(LEAK_PREVENTION)
@RunWith(Parameterized.class)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class GroovyResourceReleaserTestCase extends AbstractMuleTestCase {

  private static final ClassLoaderLookupPolicy CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY = new ClassLoaderLookupPolicy() {

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

  private static List<String> oldAjLoadersToSkip;

  @BeforeClass
  public static void avoidAspectjAgentLeak() {
    oldAjLoadersToSkip = Aj.loadersToSkip;
    Aj.loadersToSkip = asList(MuleArtifactClassLoader.class.getName(),
                              "groovy.lang.GroovyClassLoader$InnerLoader");
  }

  @AfterClass
  public static void restoreAjLoadersToSkip() {
    Aj.loadersToSkip = oldAjLoadersToSkip;
  }

  private static final String GROOVY_ARTIFACT_ID = "groovy";
  private static final String GROOVY_GROUP_ID = "org.codehaus.groovy";
  private static final String GROOVY_SCRIPT_ENGINE = "groovy.util.GroovyScriptEngine";
  private static final String GROOVY_LANG_BINDING = "groovy.lang.Binding";

  private final URL groovyUrl;

  public GroovyResourceReleaserTestCase(String groovyVersion) throws MalformedURLException {
    groovyUrl = getGroovyUrl(groovyVersion);
  }

  @Parameterized.Parameters(name = "Testing artifact {0}")
  public static String[] data() {
    return new String[] {
        "2.4.21",
        "2.5.22",
        "3.0.0",
        "3.0.17"
    };
  }

  @Test
  public void runGroovyScriptAndDispose() throws ReflectiveOperationException {
    createAppClassLoaderRunAndAssert(currentThread().getContextClassLoader(), new URL[] {groovyUrl});
  }

  @Test
  @Issue("W-15750766")
  public void runGroovyScriptAndDisposeWhenGroovyIsInDomain() throws ReflectiveOperationException {
    CollectableReference<MuleArtifactClassLoader> domainClassLoader = createClassLoader("Domain",
                                                                                        currentThread().getContextClassLoader(),
                                                                                        new URL[] {groovyUrl});
    // This is to make the hierarchy more similar to reality, in particular, this affects the way in which classloaders are
    // reachable through the parents.
    RegionClassLoader regionClassLoader = new RegionClassLoader(domainClassLoader.get());

    createAppClassLoaderRunAndAssert(regionClassLoader);
    createAppClassLoaderRunAndAssert(regionClassLoader);

    regionClassLoader = null;
    disposeClassLoaderAndAssertCollected(domainClassLoader);
  }

  private void createAppClassLoaderRunAndAssert(ClassLoader parentClassLoader) throws ReflectiveOperationException {
    createAppClassLoaderRunAndAssert(parentClassLoader, new URL[] {});
  }

  private CollectableReference<MuleArtifactClassLoader> createClassLoader(String name, ClassLoader parentClassLoader,
                                                                          URL[] classPaths) {
    return new CollectableReference<>(new MuleArtifactClassLoader(name,
                                                                  mock(ArtifactDescriptor.class),
                                                                  classPaths,
                                                                  parentClassLoader,
                                                                  CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY));
  }

  private void createAppClassLoaderRunAndAssert(ClassLoader parentClassLoader, URL[] classPaths)
      throws ReflectiveOperationException {
    CollectableReference<MuleArtifactClassLoader> collectableReference =
        createClassLoader("Application", parentClassLoader, classPaths);

    assertThat(getFieldValue(collectableReference.get(), "shouldReleaseGroovyReferences", false), is(false));
    assertThat(runScript(collectableReference.get()), is("hello"));
    assertThat(getFieldValue(collectableReference.get(), "shouldReleaseGroovyReferences", false), is(true));

    disposeClassLoaderAndAssertCollected(collectableReference);
  }

  private void disposeClassLoaderAndAssertCollected(CollectableReference<MuleArtifactClassLoader> collectableReference) {
    collectableReference.get().dispose();
    assertThat(collectableReference, is(eventually(collectedByGc())));
  }

  private String runScript(MuleArtifactClassLoader artifactClassLoader) throws ReflectiveOperationException {
    URL[] roots = new URL[] {artifactClassLoader.getResource("groovy/example.groovy")};
    Class<?> groovyScriptEngineClass = forName(GROOVY_SCRIPT_ENGINE, true, artifactClassLoader);
    Object scriptEngine =
        groovyScriptEngineClass.getConstructor(URL[].class, ClassLoader.class).newInstance(roots, artifactClassLoader);
    Class<?> groovyBinding = forName(GROOVY_LANG_BINDING, true, artifactClassLoader);
    Method runMethod = groovyScriptEngineClass.getMethod("run", String.class, groovyBinding);
    String scriptBody = "example.groovy";
    return (String) runMethod.invoke(scriptEngine, scriptBody, groovyBinding.getConstructor().newInstance());
  }

  private URL getGroovyUrl(String groovyVersion) throws MalformedURLException {
    URL settingsUrl = getClass().getClassLoader().getResource("custom-settings.xml");
    final MavenClientProvider mavenClientProvider = discoverProvider(this.getClass().getClassLoader());

    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
        newMavenConfigurationBuilder().globalSettingsLocation(toFile(settingsUrl));

    MavenClient mavenClient = mavenClientProvider
        .createMavenClient(mavenConfigurationBuilder.localMavenRepositoryLocation(localMavenRepository.get()).build());

    BundleDescriptor bundleDescriptor = new BundleDescriptor.Builder()
        .setGroupId(GROOVY_GROUP_ID)
        .setArtifactId(GROOVY_ARTIFACT_ID)
        .setVersion(groovyVersion)
        .build();

    BundleDependency dependency = mavenClient.resolveBundleDescriptor(bundleDescriptor);

    return dependency.getBundleUri().toURL();
  }

  private static class RegionClassLoader extends URLClassLoader {

    private final MuleArtifactClassLoader registeredClassLoader;

    public RegionClassLoader(MuleArtifactClassLoader registeredClassLoader) {
      super(new URL[] {});
      this.registeredClassLoader = registeredClassLoader;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      return registeredClassLoader.loadClass(name);
    }
  }
}
