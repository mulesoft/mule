/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.test.MavenTestUtils.getMavenProjectVersion;
import static org.mule.maven.client.test.MavenTestUtils.mavenPomFinder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactoryTestCase;
import org.mule.runtime.module.deployment.impl.internal.builder.DeployableFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.net.URISyntaxException;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public abstract class DeployableArtifactDescriptorFactoryTestCase<D extends DeployableArtifactDescriptor, B extends DeployableFileBuilder>
    extends AbstractMuleTestCase {

  private static final String MULE_PROJECT_VERSION =
      getMavenProjectVersion(mavenPomFinder(DeployableArtifactDescriptorFactoryTestCase.class));

  @ClassRule
  public static SystemProperty muleVersionProperty = new SystemProperty("mule.project.version", MULE_PROJECT_VERSION);

  private static File echoTestJarFile;

  private static File getResourceFile(String resource) throws URISyntaxException {
    return new File(ApplicationDescriptorFactoryTestCase.class.getResource(resource).toURI());
  }

  protected static final String ARTIFACT_NAME = "test";

  @BeforeClass
  public static void beforeClass() throws URISyntaxException {
    echoTestJarFile = new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java"))
        .including(getResourceFile("/test-resource.txt"), "META-INF/MANIFEST.MF")
        .including(getResourceFile("/test-resource.txt"), "README.txt")
        .compile("echo.jar");
  }

  @Rule
  public SystemProperty repositoryLocation = new SystemProperty("muleRuntimeConfig.maven.repositoryLocation",
                                                                discoverProvider(ApplicationDescriptorFactoryTestCase.class
                                                                    .getClassLoader()).getLocalRepositorySuppliers()
                                                                        .environmentMavenRepositorySupplier().get()
                                                                        .getAbsolutePath());

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() throws Exception {
    GlobalConfigLoader.reset();
  }

  @Test
  public void makesConfigFileRelativeToArtifactMuleFolder() throws Exception {
    DeployableFileBuilder artifactFileBuilder = createArtifactFileBuilder()
        .deployedWith("config.resources", "config1.xml,config2.xml");
    unzip(artifactFileBuilder.getArtifactFile(), getArtifactFolder());

    D desc = createArtifactDescriptor();

    assertThat(desc.getConfigResources(), hasSize(2));
    assertThat(desc.getConfigResources(), hasItems("config1.xml", "config2.xml"));
  }

  @Test
  public void duplicatesInConfigsAreRemoved() throws Exception {
    DeployableFileBuilder artifactFileBuilder = createArtifactFileBuilder()
        .deployedWith("config.resources", "config1.xml,config2.xml,config1.xml");
    unzip(artifactFileBuilder.getArtifactFile(), getArtifactFolder());

    D desc = createArtifactDescriptor();

    assertThat(desc.getConfigResources(), hasSize(2));
    assertThat(desc.getConfigResources(), hasItems("config1.xml", "config2.xml"));
  }

  @Test
  public void readsSharedLibs() throws Exception {
    DeployableFileBuilder artifactFileBuilder = (DeployableFileBuilder) createArtifactFileBuilder()
        .dependingOnSharedLibrary(new JarFileBuilder("shared", echoTestJarFile));
    unzip(artifactFileBuilder.getArtifactFile(), getArtifactFolder());

    D desc = createArtifactDescriptor();

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[0]).getPath(), equalTo(getArtifactFolder().toString()));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(),
               endsWith(getArtifactRootFolder() + "test/repository/org/mule/test/shared/1.0.0/shared-1.0.0.jar"));
    assertThat(desc.getClassLoaderModel().getExportedPackages(), contains("org.foo"));
    assertThat(desc.getClassLoaderModel().getExportedResources(), containsInAnyOrder("META-INF/MANIFEST.MF", "README.txt"));
  }

  @Test
  public void readsRuntimeLibs() throws Exception {
    DeployableFileBuilder artifactFileBuilder = (DeployableFileBuilder) createArtifactFileBuilder()
        .dependingOn(new JarFileBuilder("runtime", echoTestJarFile));
    unzip(artifactFileBuilder.getArtifactFile(), getArtifactFolder());

    D desc = createArtifactDescriptor();

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[0]).getPath(), equalTo(getArtifactFolder().toString()));
    assertThat(desc.getClassLoaderModel().getExportedPackages(), is(empty()));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(),
               endsWith(getArtifactRootFolder() + "test/repository/org/mule/test/runtime/1.0.0/runtime-1.0.0.jar"));
  }

  @Test
  public void loadsDescriptorFromJson() throws Exception {
    String artifactPath = getArtifactRootFolder() + "no-dependencies";
    D desc = createArtifactDescriptor(artifactPath);

    assertThat(desc.getMinMuleVersion(), is(new MuleVersion("4.0.0")));
    assertThat(desc.getConfigResources(), hasSize(1));
    assertThat(desc.getConfigResources().get(0), is(getDefaultConfigurationResourceLocation()));

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();
    assertThat(classLoaderModel.getDependencies().isEmpty(), is(true));
    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(toFile(classLoaderModel.getUrls()[0]).getPath(),
               is(getArtifact(artifactPath).getAbsolutePath()));

    assertThat(classLoaderModel.getExportedPackages().isEmpty(), is(true));
    assertThat(classLoaderModel.getExportedResources().isEmpty(), is(true));
    assertThat(classLoaderModel.getDependencies().isEmpty(), is(true));
  }

  @Test
  public void loadsDescriptorFromJsonWithCustomConfigFiles() throws Exception {
    String artifactPath = getArtifactRootFolder() + "custom-config-files";
    D desc = createArtifactDescriptor(artifactPath);

    assertThat(desc.getConfigResources(), contains("file1.xml", "file2.xml"));
  }

  @Test
  public void classLoaderModelWithIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "include-test-dependencies");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.isIncludeTestDependencies(), is(true));
  }

  @Test
  public void classLoaderModelWithoutIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "do-not-include-test-dependencies");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.isIncludeTestDependencies(), is(false));
  }

  @Test
  public void classLoaderModelDefaultIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "custom-config-files");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.isIncludeTestDependencies(), is(false));
  }

  @Test
  public void classLoaderModelWithSingleDependency() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "single-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies(), hasSize(1));
    BundleDependency commonsCollectionDependency = classLoaderModel.getDependencies().iterator().next();
    assertThat(commonsCollectionDependency, commonsCollectionDependencyMatcher());

    assertThat(classLoaderModel.getUrls().length, is(2));
    assertThat(asList(classLoaderModel.getUrls()), hasItem(commonsCollectionDependency.getBundleUri().toURL()));
  }

  @Test
  public void classLoaderModelWithPluginDependency() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "plugin-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(1));
    assertThat(classLoaderModel.getDependencies(), hasItem(socketsPluginDependencyMatcher()));

    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(asList(classLoaderModel.getUrls()), not(hasItem(classLoaderModel.getDependencies().iterator().next())));
  }

  @Test
  public void classLoaderModelWithPluginDependencyWithAnotherPlugin() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "plugin-dependency-with-another-plugin");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(2));
    assertThat(classLoaderModel.getDependencies(), hasItems(dependantPluginDependencyMatcher(), emptyPluginDependencyMatcher()));

    assertThat(classLoaderModel.getUrls().length, is(1));
    classLoaderModel.getDependencies().stream()
        .forEach(bundleDependency -> {
          assertThat(asList(classLoaderModel.getUrls()), not(hasItem(bundleDependency.getBundleUri())));
        });
  }

  @Test
  public void missingRequiredProduct() throws Exception {
    String artifactName = "no-required-product";
    requiredProductValidationExpectedException(artifactName);
    createArtifactDescriptor(getArtifactRootFolder() + artifactName);
  }

  @Test
  public void wrongRequiredProductValue() throws Exception {
    String artifactName = "bad-required-product";
    requiredProductValidationExpectedException(artifactName);
    createArtifactDescriptor(getArtifactRootFolder() + artifactName);
  }

  @Test
  public void descriptorWithNoRevisionVersion() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException
        .expectMessage("Artifact no-revision-artifact version 1.0 must contain a revision number. The version format must be x.y.z and the z part is missing");
    createArtifactDescriptor(getArtifactRootFolder() + "no-revision-artifact");
  }

  private void requiredProductValidationExpectedException(String appName) {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(Matchers
        .containsString(format("The artifact %s does not specifies a requiredProduct or the specified value is not valid. Valid values are MULE, MULE_EE",
                               appName)));
  }

  protected abstract String getArtifactRootFolder();

  protected abstract File getArtifactFolder();

  protected abstract D createArtifactDescriptor();

  protected abstract B createArtifactFileBuilder();

  protected abstract File getArtifact(String appPath) throws URISyntaxException;

  protected abstract D createArtifactDescriptor(String appPath) throws URISyntaxException;

  protected abstract String getDefaultConfigurationResourceLocation();

  protected ServiceRegistryDescriptorLoaderRepository createDescriptorLoaderRepository() {
    return new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry());
  }

  private Matcher<BundleDependency> commonsCollectionDependencyMatcher() {
    return new BaseMatcher<BundleDependency>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("invalid bundle configuration");
      }

      @Override
      public boolean matches(Object o) {
        if (!(o instanceof BundleDependency)) {
          return false;
        }

        BundleDependency bundleDependency = (BundleDependency) o;
        return bundleDependency.getScope().equals(COMPILE) &&
            !bundleDependency.getDescriptor().getClassifier().isPresent() &&
            bundleDependency.getDescriptor().getArtifactId().equals("commons-collections") &&
            bundleDependency.getDescriptor().getGroupId().equals("commons-collections") &&
            bundleDependency.getDescriptor().getVersion().equals("3.2.2");
      }
    };
  }

  private Matcher<BundleDependency> socketsPluginDependencyMatcher() {
    return new BaseMatcher<BundleDependency>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("invalid bundle configuration");
      }

      @Override
      public boolean matches(Object o) {
        if (!(o instanceof BundleDependency)) {
          return false;
        }

        BundleDependency bundleDependency = (BundleDependency) o;
        return bundleDependency.getScope().equals(COMPILE) &&
            bundleDependency.getDescriptor().getClassifier().isPresent() &&
            bundleDependency.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER) &&
            bundleDependency.getDescriptor().getArtifactId().equals("test-empty-plugin") &&
            bundleDependency.getDescriptor().getGroupId().equals("org.mule.tests") &&
            bundleDependency.getDescriptor().getVersion().equals(MULE_PROJECT_VERSION);
      }
    };
  }

  private Matcher<BundleDependency> dependantPluginDependencyMatcher() {
    return createPluginMatcher("test-dependant-plugin");
  }

  private Matcher<BundleDependency> emptyPluginDependencyMatcher() {
    return createPluginMatcher("test-empty-plugin");
  }

  private Matcher<BundleDependency> createPluginMatcher(String artifactId) {
    return new BaseMatcher<BundleDependency>() {

      @Override
      public void describeTo(Description description) {
        description.appendText(" invalid bundle configuration");
      }

      @Override
      public boolean matches(Object o) {
        if (!(o instanceof BundleDependency)) {
          return false;
        }

        BundleDependency bundleDependency = (BundleDependency) o;
        return bundleDependency.getScope().equals(COMPILE) &&
            bundleDependency.getDescriptor().getClassifier().isPresent() &&
            bundleDependency.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER) &&
            bundleDependency.getDescriptor().getArtifactId().equals(artifactId) &&
            bundleDependency.getDescriptor().getGroupId().equals("org.mule.tests") &&
            bundleDependency.getDescriptor().getVersion().equals("4.0.0-SNAPSHOT");
      }
    };
  }
}
