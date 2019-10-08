/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.test.MavenTestUtils.getMavenProjectVersion;
import static org.mule.maven.client.test.MavenTestUtils.mavenPomFinder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.PROVIDED;
import static org.mule.runtime.module.deployment.impl.internal.BundleDependencyMatcher.bundleDependency;
import static org.mule.runtime.module.deployment.impl.internal.MavenTestUtils.installArtifact;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
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
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
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
    Path expectedPathEnd =
        get(getArtifactRootFolder(), "test", "repository", "org", "mule", "test", "shared", "1.0.0", "shared-1.0.0.jar");
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(), endsWith(expectedPathEnd.toString()));
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
    Path expectedPathEnd =
        get(getArtifactRootFolder(), "test", "repository", "org", "mule", "test", "runtime", "1.0.0", "runtime-1.0.0.jar");
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(), endsWith(expectedPathEnd.toString()));
  }

  @Test
  public void loadsDescriptorFromJson() throws Exception {
    String artifactPath = getArtifactRootFolder() + "/no-dependencies";
    D desc = createArtifactDescriptor(artifactPath);

    assertThat(desc.getMinMuleVersion(), is(new MuleVersion("4.0.0")));
    assertThat(desc.getConfigResources(), hasSize(1));
    assertThat(desc.getConfigResources(), hasItem(getDefaultConfigurationResourceLocation()));

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
    String artifactPath = getArtifactRootFolder() + "/custom-config-files";
    D desc = createArtifactDescriptor(artifactPath);

    assertThat(desc.getConfigResources(), contains("file1.xml", "file2.xml"));
  }

  @Test
  public void classLoaderModelWithIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/include-test-dependencies");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.isIncludeTestDependencies(), is(true));
  }

  @Test
  public void classLoaderModelWithoutIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/do-not-include-test-dependencies");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.isIncludeTestDependencies(), is(false));
  }

  @Test
  public void classLoaderModelDefaultIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/custom-config-files");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.isIncludeTestDependencies(), is(false));
  }

  @Test
  public void classLoaderModelWithSingleDependency() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/single-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies(), hasSize(1));
    BundleDependency commonsCollectionDependency = classLoaderModel.getDependencies().iterator().next();
    assertThat(commonsCollectionDependency, commonsCollectionDependencyMatcher());

    assertThat(classLoaderModel.getUrls().length, is(2));
    assertThat(asList(classLoaderModel.getUrls()), hasItem(commonsCollectionDependency.getBundleUri().toURL()));
  }

  @Test
  public void classLoaderModelWithPluginDependencyDeclaredAsProvided() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/provided-plugin-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(1));
    assertThat(classLoaderModel.getDependencies(),
               hasItem(testEmptyPluginDependencyMatcher(PROVIDED, false, true)));
  }

  @Test
  public void classLoaderModelWithPluginDependency() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(1));
    assertThat(classLoaderModel.getDependencies(), hasItem(testEmptyPluginDependencyMatcher()));

    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(asList(classLoaderModel.getUrls()), not(hasItem(classLoaderModel.getDependencies().iterator().next())));
  }

  @Test
  public void classLoaderModelWithPluginDependencyWithTransitiveDependency() throws Exception {
    installArtifact(getArtifact("dependencies/plugin-with-transitive-dependency"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-1.0.0.pom"), new File(repositoryLocation.getValue()));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-transitive-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    final String expectedPluginArtifactId = "plugin-with-transitive-dependency";

    assertThat(classLoaderModel.getDependencies().size(), is(1));
    assertThat(classLoaderModel.getDependencies(), hasItem(bundleDependency(expectedPluginArtifactId)));

    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(asList(classLoaderModel.getUrls()), not(hasItem(classLoaderModel.getDependencies().iterator().next())));

    ArtifactPluginDescriptor pluginDescriptor = desc.getPlugins().stream().findFirst().get();

    assertThat(pluginDescriptor.getBundleDescriptor().getArtifactId(), equalTo(expectedPluginArtifactId));
    assertThat(pluginDescriptor.getClassLoaderModel().getDependencies(), hasItem(bundleDependency("library")));
  }


  @Test
  public void classLoaderModelWithPluginDependencyWithMultipleTransitiveDependenciesLevels() throws Exception {
    installArtifact(getArtifact("dependencies/plugin-with-transitive-dependencies"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-with-dependency-a-1.0.0.pom"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-with-dependency-b-1.0.0.pom"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-1.0.0.pom"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-2.0.0.pom"), new File(repositoryLocation.getValue()));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-transitive-dependencies");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    final String expectedPluginArtifactId = "plugin-with-transitive-dependencies";

    assertThat(classLoaderModel.getDependencies().size(), is(1));
    assertThat(classLoaderModel.getDependencies(), hasItem(bundleDependency(expectedPluginArtifactId)));

    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(asList(classLoaderModel.getUrls()), not(hasItem(classLoaderModel.getDependencies().iterator().next())));

    ArtifactPluginDescriptor pluginDescriptor = desc.getPlugins().stream().findFirst().get();

    assertThat(pluginDescriptor.getBundleDescriptor().getArtifactId(), equalTo(expectedPluginArtifactId));
    assertThat(pluginDescriptor.getClassLoaderModel().getDependencies(), hasItems(
                                                                                  bundleDependency("library-with-dependency-a"),
                                                                                  bundleDependency("library-with-dependency-b"),
                                                                                  bundleDependency("library", "1.0.0")));
    assertThat(pluginDescriptor.getClassLoaderModel().getDependencies(), not(hasItem(bundleDependency("library", "2.0.0"))));
  }


  @Test
  public void classLoaderModelWithPluginDependencyAndAdditionalDependenciesLightweight() throws Exception {
    assertClassLoaderModelWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-lightweight");
  }

  @Test
  public void classLoaderModelWithPluginDependencyAndAdditionalDependenciesLightweightUseLocalRepository() throws Exception {
    assertClassLoaderModelWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-lightweight-local-repository");
  }

  @Test
  public void classLoaderModelWithPluginDependencyAndAdditionalDependenciesLightweightUsingSystemScope() throws Exception {
    assertClassLoaderModelWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-system-scope-lightweight");
  }

  @Test
  public void classLoaderModelWithPluginDependencyAndAdditionalDependenciesHeavyweight() throws Exception {
    assertClassLoaderModelWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-heavyweight");
  }

  private void assertClassLoaderModelWithPluginDependencyAndAdditionalDependencies(String location) throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + location);

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(2));
    assertThat(classLoaderModel.getDependencies(), hasItem(testEmptyPluginDependencyMatcher(COMPILE, true, false)));

    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(asList(classLoaderModel.getUrls()), not(hasItem(classLoaderModel.getDependencies().iterator().next())));

    assertThat(desc.getPlugins(), hasSize(2));

    ArtifactPluginDescriptor testEmptyPluginDescriptor = desc.getPlugins().stream()
        .filter(plugin -> plugin.getBundleDescriptor().getArtifactId().contains("test-empty-plugin")).findFirst().get();
    assertThat(testEmptyPluginDescriptor.getClassLoaderModel().getUrls().length, is(3));
    assertThat(of(testEmptyPluginDescriptor.getClassLoaderModel().getUrls()).map(url -> FileUtils.toFile(url)).collect(toList()),
               everyItem(exists()));

    assertThat(of(testEmptyPluginDescriptor.getClassLoaderModel().getUrls()).map(url -> FileUtils.toFile(url).getName())
        .collect(toList()),
               hasItems(startsWith("test-empty-plugin-"), equalTo("commons-io-2.6.jar"),
                        equalTo("commons-collections-3.2.1.jar")));
    // additional dependencies declared by the deployable artifact for a plugin are not seen as dependencies, they just go to the
    // urls
    assertThat(testEmptyPluginDescriptor.getClassLoaderModel().getDependencies(), hasSize(0));

    ArtifactPluginDescriptor dependantPluginDescriptor = desc.getPlugins().stream()
        .filter(plugin -> plugin.getBundleDescriptor().getArtifactId().contains("dependant")).findFirst().get();
    assertThat(dependantPluginDescriptor.getClassLoaderModel().getUrls().length, is(1));
    assertThat(dependantPluginDescriptor.getClassLoaderModel().getDependencies(), hasSize(1));
  }

  private static Matcher<File> exists() {
    return new TypeSafeMatcher<File>() {

      File fileTested;

      @Override
      public boolean matchesSafely(File item) {
        fileTested = item;
        return item.exists();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("file ");
        description.appendValue(fileTested);
        description.appendText(" should exists");
      }
    };
  }

  @Test
  public void appWithPluginAsSystemDependencyIsResolved() throws Exception {
    installArtifact(getArtifact("dependencies/plugin-with-transitive-dependency"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-1.0.0.pom"), new File(repositoryLocation.getValue()));
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-as-system");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(1));

    assertThat(classLoaderModel.getUrls().length, is(1));
  }

  @Test
  public void classLoaderModelWithPluginDependencyWithAnotherPlugin() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-another-plugin");

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
    createArtifactDescriptor(getArtifactRootFolder() + "/" + artifactName);
  }

  @Test
  public void wrongRequiredProductValue() throws Exception {
    String artifactName = "bad-required-product";
    requiredProductValidationExpectedException(artifactName);
    createArtifactDescriptor(getArtifactRootFolder() + separator + artifactName);
  }

  @Test
  public void descriptorWithNoRevisionVersion() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException
        .expectMessage("Artifact no-revision-artifact version 1.0 must contain a revision number. The version format must be x.y.z and the z part is missing");
    createArtifactDescriptor(getArtifactRootFolder() + separator + "no-revision-artifact");
  }

  @Test
  public void getLogConfigFileFromFullFilePath() {
    MuleDeployableModel deployableModel = mock(MuleDeployableModel.class);
    File logConfigFile = Paths.get("custom-log4j2.xml").toAbsolutePath().toFile();
    when(deployableModel.getLogConfigFile()).thenReturn(logConfigFile.getAbsolutePath());

    File resolvedFile = createDeployableDescriptorFactory().getLogConfigFile(deployableModel);
    assertThat(resolvedFile.toPath(), is(logConfigFile.toPath()));
  }

  @Test
  public void appWithSameDependencyWithDifferentClassifier() throws Exception {
    installArtifact(getArtifact("dependencies/library"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-test-jar"), new File(repositoryLocation.getValue()));
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/same-dep-diff-classifier");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies(), contains(
                                                            bundleDependency("library"),
                                                            bundleDependency("library")));

    assertThat(stream(classLoaderModel.getUrls()).map(URL::getPath).collect(toList()),
               contains(
                        containsString("same-dep-diff-classifier"),
                        containsString("library-1.0.0.jar"),
                        containsString("library-1.0.0-test-jar.jar")));
  }

  @Test
  public void appWithPluginWithSameDependencyWithDifferentClassifier() throws Exception {
    installArtifact(getArtifact("dependencies/library"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-test-jar"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/plugin-with-transitive-dependencies-different-classifier"),
                    new File(repositoryLocation.getValue()));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-same-dep-diff-classifier");

    ArtifactPluginDescriptor plugin = desc.getPlugins().iterator().next();
    ClassLoaderModel pluginClassLoderModel = plugin.getClassLoaderModel();

    assertThat(pluginClassLoderModel.getDependencies(), contains(
                                                                 bundleDependency("library"),
                                                                 bundleDependency("library")));

    assertThat(stream(pluginClassLoderModel.getUrls()).map(URL::getPath).collect(toList()),
               contains(
                        containsString("plugin-with-transitive-dependencies-different-classifier-1.0.0-mule-plugin.jar"),
                        containsString("library-1.0.0.jar"),
                        containsString("library-1.0.0-test-jar.jar")));
  }

  @Test
  public void appWithPluginWithSameDependencyWithDifferentClassifierAsAdditionalDependencies() throws Exception {
    installArtifact(getArtifact("dependencies/library"), new File(repositoryLocation.getValue()));
    installArtifact(getArtifact("dependencies/library-test-jar"), new File(repositoryLocation.getValue()));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-same-dep-diff-classifier-as-additional");

    ArtifactPluginDescriptor plugin = desc.getPlugins().iterator().next();
    ClassLoaderModel pluginClassLoderModel = plugin.getClassLoaderModel();

    assertThat(pluginClassLoderModel.getDependencies(), is(empty()));

    assertThat(stream(pluginClassLoderModel.getUrls()).map(URL::getPath).collect(toList()),
               contains(
                        containsString("test-empty-plugin"),
                        containsString("library-1.0.0.jar"),
                        containsString("library-1.0.0-test-jar.jar")));
  }

  private void requiredProductValidationExpectedException(String appName) {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(Matchers
        .containsString(format("Invalid artifact descriptor: \"%s\". Mandatory field \"requiredProduct\" is missing or has an invalid value. Valid values are MULE, MULE_EE",
                               appName)));
  }

  protected abstract String getArtifactRootFolder();

  protected abstract File getArtifactFolder();

  protected abstract D createArtifactDescriptor();

  protected abstract B createArtifactFileBuilder();

  protected abstract File getArtifact(String appPath) throws URISyntaxException;

  protected abstract D createArtifactDescriptor(String appPath) throws URISyntaxException;

  protected abstract String getDefaultConfigurationResourceLocation();

  protected abstract <T extends DeployableArtifactDescriptor> AbstractDeployableDescriptorFactory createDeployableDescriptorFactory();

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

  private Matcher<BundleDependency> testEmptyPluginDependencyMatcher() {
    return testEmptyPluginDependencyMatcher(COMPILE, true, true);
  }

  private Matcher<BundleDependency> testEmptyPluginDependencyMatcher(BundleScope scope, boolean hasUri, boolean checkVersion) {
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

        return bundleDependency.getDescriptor().getClassifier().isPresent() &&
            (bundleDependency.getScope() == null || bundleDependency.getScope().equals(scope)) &&
            bundleDependency.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER) &&
            bundleDependency.getDescriptor().getArtifactId().equals("test-empty-plugin") &&
            bundleDependency.getDescriptor().getGroupId().equals("org.mule.tests") &&
            (!checkVersion || bundleDependency.getDescriptor().getVersion().equals(MULE_PROJECT_VERSION)) &&
            (hasUri == (bundleDependency.getBundleUri() != null));
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
        return bundleDependency.getDescriptor().getClassifier().isPresent() &&
            (bundleDependency.getScope() == null || bundleDependency.getScope().equals(COMPILE)) &&
            bundleDependency.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER) &&
            bundleDependency.getDescriptor().getArtifactId().equals(artifactId) &&
            bundleDependency.getDescriptor().getGroupId().equals("org.mule.tests") &&
            bundleDependency.getDescriptor().getVersion().equals(MULE_PROJECT_VERSION);
      }
    };
  }
}
