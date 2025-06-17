/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.test.MavenTestUtils.getMavenProjectVersion;
import static org.mule.maven.client.test.MavenTestUtils.mavenPomFinder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.PROVIDED;
import static org.mule.runtime.module.deployment.impl.internal.BundleDependencyMatcher.bundleDependency;
import static org.mule.tck.MavenTestUtils.installArtifact;

import static java.lang.System.getProperty;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.toFile;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import static org.hamcrest.core.StringContains.containsString;

import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactoryTestCase;
import org.mule.runtime.module.deployment.impl.internal.builder.DeployableFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.tck.util.CompilerUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.qameta.allure.Issue;

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
  public SystemProperty activeProfile = new SystemProperty("muleRuntimeConfig.maven.activeProfiles.0", "test");

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);

  private List<File> installedArtifactFiles;

  @Before
  public void setUp() throws Exception {
    GlobalConfigLoader.reset();
    installedArtifactFiles = new ArrayList<>();
  }

  @After
  public void tearDown() {
    installedArtifactFiles
        .forEach(f -> f.delete());
    installedArtifactFiles.clear();
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

    assertThat(desc.getClassLoaderConfiguration().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderConfiguration().getUrls()[0]).getPath(), equalTo(getArtifactFolder().toString()));
    Path expectedPathEnd =
        get(getArtifactRootFolder(), "test", "repository", "org", "mule", "test", "shared", "1.0.0", "shared-1.0.0.jar");
    assertThat(toFile(desc.getClassLoaderConfiguration().getUrls()[1]).getPath(), endsWith(expectedPathEnd.toString()));
    assertThat(desc.getClassLoaderConfiguration().getExportedPackages(), contains("org.foo"));
    assertThat(desc.getClassLoaderConfiguration().getExportedResources(),
               containsInAnyOrder("META-INF/MANIFEST.MF", "README.txt"));
  }

  @Test
  public void readsRuntimeLibs() throws Exception {
    DeployableFileBuilder artifactFileBuilder = (DeployableFileBuilder) createArtifactFileBuilder()
        .dependingOn(new JarFileBuilder("runtime", echoTestJarFile));
    unzip(artifactFileBuilder.getArtifactFile(), getArtifactFolder());

    D desc = createArtifactDescriptor();

    assertThat(desc.getClassLoaderConfiguration().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderConfiguration().getUrls()[0]).getPath(), equalTo(getArtifactFolder().toString()));
    assertThat(desc.getClassLoaderConfiguration().getExportedPackages(), is(empty()));
    Path expectedPathEnd =
        get(getArtifactRootFolder(), "test", "repository", "org", "mule", "test", "runtime", "1.0.0", "runtime-1.0.0.jar");
    assertThat(toFile(desc.getClassLoaderConfiguration().getUrls()[1]).getPath(), endsWith(expectedPathEnd.toString()));
  }

  @Test
  public void loadsDescriptorFromJson() throws Exception {
    String artifactPath = getArtifactRootFolder() + "/no-dependencies";
    D desc = createArtifactDescriptor(artifactPath);

    assertThat(desc.getMinMuleVersion(), is(new MuleVersion("4.0.0")));
    assertThat(desc.getConfigResources(), hasSize(1));
    assertThat(desc.getConfigResources(), hasItem(getDefaultConfigurationResourceLocation()));

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();
    assertThat(classLoaderConfiguration.getDependencies().isEmpty(), is(true));
    assertThat(classLoaderConfiguration.getUrls().length, is(1));
    assertThat(toFile(classLoaderConfiguration.getUrls()[0]).getPath(),
               is(getArtifact(artifactPath).getAbsolutePath()));

    assertThat(classLoaderConfiguration.getExportedPackages().isEmpty(), is(true));
    assertThat(classLoaderConfiguration.getExportedResources().isEmpty(), is(true));
    assertThat(classLoaderConfiguration.getDependencies().isEmpty(), is(true));
  }

  @Test
  public void loadsDescriptorFromJsonWithCustomConfigFiles() throws Exception {
    String artifactPath = getArtifactRootFolder() + "/custom-config-files";
    D desc = createArtifactDescriptor(artifactPath);

    assertThat(desc.getConfigResources(), contains("file1.xml", "file2.xml"));
  }

  @Test
  public void classLoaderConfigurationWithIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/include-test-dependencies");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.isIncludeTestDependencies(), is(true));
  }

  @Test
  public void classLoaderConfigurationWithoutIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/do-not-include-test-dependencies");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.isIncludeTestDependencies(), is(false));
  }

  @Test
  public void classLoaderConfigurationDefaultIncludeTestDependencies() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/custom-config-files");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.isIncludeTestDependencies(), is(false));
  }

  @Test
  public void classLoaderConfigurationWithSingleDependency() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/single-dependency");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies(), hasSize(1));
    BundleDependency commonsCollectionDependency = classLoaderConfiguration.getDependencies().iterator().next();
    assertThat(commonsCollectionDependency, commonsCollectionDependencyMatcher());

    assertThat(classLoaderConfiguration.getUrls().length, is(2));
    assertThat(asList(classLoaderConfiguration.getUrls()), hasItem(commonsCollectionDependency.getBundleUri().toURL()));
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyDeclaredAsProvided() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/provided-plugin-dependency");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies().size(), is(1));
    assertThat(classLoaderConfiguration.getDependencies(),
               hasItem(testEmptyPluginDependencyMatcher(PROVIDED, false, true)));
  }

  @Test
  public void classLoaderConfigurationWithPluginDependency() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies().size(), is(1));
    assertThat(classLoaderConfiguration.getDependencies(), hasItem(testEmptyPluginDependencyMatcher()));

    assertThat(classLoaderConfiguration.getUrls().length, is(1));
    assertThat(asList(classLoaderConfiguration.getUrls()),
               not(hasItem(classLoaderConfiguration.getDependencies().iterator().next())));
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyWithTransitiveDependency() throws Exception {
    installArtifactInRepo(getArtifact("dependencies/plugin-with-transitive-dependency"));
    installArtifactInRepo(getArtifact("dependencies/library-1.0.0.pom"));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-transitive-dependency");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    final String expectedPluginArtifactId = "plugin-with-transitive-dependency";

    assertThat(classLoaderConfiguration.getDependencies().size(), is(1));
    assertThat(classLoaderConfiguration.getDependencies(), hasItem(bundleDependency(expectedPluginArtifactId)));

    assertThat(classLoaderConfiguration.getUrls().length, is(1));
    assertThat(asList(classLoaderConfiguration.getUrls()),
               not(hasItem(classLoaderConfiguration.getDependencies().iterator().next())));

    ArtifactPluginDescriptor pluginDescriptor = desc.getPlugins().stream().findFirst().get();

    assertThat(pluginDescriptor.getBundleDescriptor().getArtifactId(), equalTo(expectedPluginArtifactId));
    assertThat(pluginDescriptor.getClassLoaderConfiguration().getDependencies(), hasItem(bundleDependency("library")));
  }


  @Test
  public void classLoaderConfigurationWithPluginDependencyWithMultipleTransitiveDependenciesLevels() throws Exception {
    installArtifactInRepo(getArtifact("dependencies/plugin-with-transitive-dependencies"));
    installArtifactInRepo(getArtifact("dependencies/library-with-dependency-a-1.0.0.pom"));
    installArtifactInRepo(getArtifact("dependencies/library-with-dependency-b-1.0.0.pom"));
    installArtifactInRepo(getArtifact("dependencies/library-1.0.0.pom"));
    installArtifactInRepo(getArtifact("dependencies/library-2.0.0.pom"));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-transitive-dependencies");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    final String expectedPluginArtifactId = "plugin-with-transitive-dependencies";

    assertThat(classLoaderConfiguration.getDependencies().size(), is(1));
    assertThat(classLoaderConfiguration.getDependencies(), hasItem(bundleDependency(expectedPluginArtifactId)));

    assertThat(classLoaderConfiguration.getUrls().length, is(1));
    assertThat(asList(classLoaderConfiguration.getUrls()),
               not(hasItem(classLoaderConfiguration.getDependencies().iterator().next())));

    ArtifactPluginDescriptor pluginDescriptor = desc.getPlugins().stream().findFirst().get();

    assertThat(pluginDescriptor.getBundleDescriptor().getArtifactId(), equalTo(expectedPluginArtifactId));
    assertThat(pluginDescriptor.getClassLoaderConfiguration().getDependencies(), hasItems(
                                                                                          bundleDependency("library-with-dependency-a"),
                                                                                          bundleDependency("library-with-dependency-b"),
                                                                                          bundleDependency("library", "1.0.0")));
    assertThat(pluginDescriptor.getClassLoaderConfiguration().getDependencies(),
               not(hasItem(bundleDependency("library", "2.0.0"))));
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyAndAdditionalDependenciesLightweight() throws Exception {
    assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-lightweight");
  }

  @Test
  @Issue("MULE-19282")
  public void classLoaderConfigurationWithPluginDependencyAndAdditionalDependenciesInProfileLightweight() throws Exception {
    assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-in-profile-lightweight");
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyAndAdditionalDependenciesLightweightUseLocalRepository()
      throws Exception {
    replacePlaceholderInClassloaderModel();
    final String location = "/plugin-dependency-with-additional-dependencies-lightweight-local-repository";
    populateRepositoryDependencies(new File(getArtifact(getArtifactRootFolder() + location), "local-repository"));
    assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies(location);
  }

  private void replacePlaceholderInClassloaderModel() throws IOException {
    String root = getRoot();

    String content =
        readFile(getClass()
            .getResourceAsStream(root
                + "/plugin-dependency-with-additional-dependencies-lightweight-local-repository/META-INF/mule-artifact/classloader-model.json"));

    String content2 =
        readFile(getClass()
            .getResourceAsStream(root
                + "/plugin-dependency-with-additional-dependencies-lightweight-local-repository/META-INF/mule-artifact/org/mule/tests/test-dependant-plugin/4.2.0-SNAPSHOT/classloader-model.json"));

    System.setProperty("outputDirectory",
                       getClass().getProtectionDomain().getCodeSource().getLocation().toString());

    String replacedContent = replacePlaceholderWithSystemProperty(content, "outputDirectory");
    String replacedContent2 = replacePlaceholderWithSystemProperty(content2, "outputDirectory");


    writeFile(getClass()
        .getResource(root
            + "/plugin-dependency-with-additional-dependencies-lightweight-local-repository/META-INF/mule-artifact/classloader-model.json")
        .getPath(),
              replacedContent);
    writeFile(getClass()
        .getResource(root
            + "/plugin-dependency-with-additional-dependencies-lightweight-local-repository/META-INF/mule-artifact/org/mule/tests/test-dependant-plugin/4.2.0-SNAPSHOT/classloader-model.json")
        .getPath(),
              replacedContent2);
  }

  protected String getRoot() {
    return "/apps";
  }

  private static String readFile(InputStream inputStream) throws IOException {
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append(System.lineSeparator());
      }
    }
    return content.toString();
  }

  private static void writeFile(String filePath, String content) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(content);
    }
  }

  private static String replacePlaceholderWithSystemProperty(String content, String placeholder) {
    // Replace ${placeholder} with the system property value
    String placeholderString = "${" + placeholder + "}";
    String replacement = System.getProperty(placeholder);

    if (replacement != null) {
      content = content.replace(placeholderString, replacement);
    } else {
      fail("System property not set for " + placeholderString);
    }

    return content;
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyAndAdditionalDependenciesLightweightUsingSystemScope()
      throws Exception {
    assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies("/plugin-dependency-with-additional-dependencies-system-scope-lightweight");
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyAndAdditionalDependenciesHeavyweight() throws Exception {
    final String location = "/plugin-dependency-with-additional-dependencies-heavyweight";
    populateRepositoryDependencies(location);
    assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies(location);
  }

  @Test
  @Issue("MULE-19282")
  public void classLoaderConfigurationWithPluginDependencyAndAdditionalDependenciesInProfileHeavyweight() throws Exception {
    final String location = "/plugin-dependency-with-additional-dependencies-in-profile-heavyweight";
    populateRepositoryDependencies(location);
    assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies(location);
  }

  private void populateRepositoryDependencies(String location) throws Exception {
    populateRepositoryDependencies(new File(getArtifact(getArtifactRootFolder() + location), "repository"));
  }

  private void populateRepositoryDependencies(final File targetRepository) throws IOException {
    final Path commonsCollectionsJarLoc = Paths.get(getProperty("commons-collections4"));
    final File commonsCollectionsInRepo =
        new File(targetRepository,
                 Paths.get(getProperty("local-repo"))
                     .relativize(commonsCollectionsJarLoc.getParent()).toString());
    commonsCollectionsInRepo.mkdirs();
    for (File repoFile : commonsCollectionsJarLoc.toFile().getParentFile().listFiles()) {
      copyFileToDirectory(repoFile, commonsCollectionsInRepo);
    }

    final Path commonsIoJarLoc = Paths.get(getProperty("commons-io"));
    final File commonsIoInRepo =
        new File(targetRepository,
                 Paths.get(getProperty("local-repo"))
                     .relativize(commonsIoJarLoc.getParent()).toString());
    commonsIoInRepo.mkdirs();
    for (File repoFile : commonsIoJarLoc.toFile().getParentFile().listFiles()) {
      copyFileToDirectory(repoFile, commonsIoInRepo);
    }
  }

  private void assertClassLoaderConfigurationWithPluginDependencyAndAdditionalDependencies(String location) throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + location);

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies().size(), is(2));
    assertThat(classLoaderConfiguration.getDependencies(), hasItem(testEmptyPluginDependencyMatcher(COMPILE, true, false)));

    assertThat(classLoaderConfiguration.getUrls().length, is(1));
    assertThat(asList(classLoaderConfiguration.getUrls()),
               not(hasItem(classLoaderConfiguration.getDependencies().iterator().next().getBundleUri().toURL())));

    assertThat(desc.getPlugins(), hasSize(2));

    ArtifactPluginDescriptor testEmptyPluginDescriptor = desc.getPlugins().stream()
        .filter(plugin -> plugin.getBundleDescriptor().getArtifactId().contains("test-empty-plugin")).findFirst().get();
    assertThat(testEmptyPluginDescriptor.getClassLoaderConfiguration().getUrls().length, is(3));
    assertThat(of(testEmptyPluginDescriptor.getClassLoaderConfiguration().getUrls()).map(url -> FileUtils.toFile(url))
        .collect(toList()),
               everyItem(exists()));

    assertThat(of(testEmptyPluginDescriptor.getClassLoaderConfiguration().getUrls())
        .map(url -> FileUtils.toFile(url).getName())
        .collect(toList()),
               hasItems(startsWith("test-empty-plugin-"),
                        allOf(startsWith("commons-io-2."), endsWith(".jar")),
                        allOf(startsWith("commons-collections4-4."), endsWith(".jar"))));
    // additional dependencies declared by the deployable artifact for a plugin are not seen as dependencies, they just go to the
    // urls
    assertThat(testEmptyPluginDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(0));

    assertThat(testEmptyPluginDescriptor.getClassLoaderConfiguration().getLocalPackages(), hasSize(36));
    assertThat(testEmptyPluginDescriptor.getClassLoaderConfiguration().getLocalPackages(),
               hasItems("org.apache.commons.collections4",
                        "org.apache.commons.io"));
    assertThat(testEmptyPluginDescriptor.getClassLoaderConfiguration().getLocalResources(),
               hasItems("META-INF/maven/org.apache.commons/commons-collections4/pom.xml",
                        "META-INF/maven/commons-io/commons-io/pom.xml"));

    ArtifactPluginDescriptor dependantPluginDescriptor = desc.getPlugins().stream()
        .filter(plugin -> plugin.getBundleDescriptor().getArtifactId().contains("dependant")).findFirst().get();
    assertThat(dependantPluginDescriptor.getClassLoaderConfiguration().getUrls().length, is(1));
    assertThat(dependantPluginDescriptor.getClassLoaderConfiguration().getDependencies(), hasSize(1));
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyAndSharedLibrariesLightweight() throws Exception {
    assertClassLoaderConfigurationWithPluginDependencyAndSharedLibraries("/plugin-dependency-with-shared-libraries-lightweight");
  }

  @Test
  @Issue("MULE-19282")
  public void classLoaderConfigurationWithPluginDependencyAndSharedLibrariesInProfileLightweight() throws Exception {
    assertClassLoaderConfigurationWithPluginDependencyAndSharedLibraries("/plugin-dependency-with-shared-libraries-in-profile-lightweight");
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyAndSharedLibrariesHeavyweight() throws Exception {
    final String location = "/plugin-dependency-with-shared-libraries-heavyweight";
    populateRepositoryDependencies(location);
    assertClassLoaderConfigurationWithPluginDependencyAndSharedLibraries(location);
  }

  @Test
  @Issue("MULE-19282")
  public void classLoaderConfigurationWithPluginDependencyAndSharedLibrariesInProfileHeavyweight() throws Exception {
    final String location = "/plugin-dependency-with-shared-libraries-in-profile-heavyweight";
    populateRepositoryDependencies(location);
    assertClassLoaderConfigurationWithPluginDependencyAndSharedLibraries(location);
  }

  private void assertClassLoaderConfigurationWithPluginDependencyAndSharedLibraries(String location) throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + location);

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies().size(), is(4));
    assertThat(classLoaderConfiguration.getDependencies(), hasItem(testEmptyPluginDependencyMatcher(COMPILE, true, false)));

    assertThat(classLoaderConfiguration.getUrls().length, is(3));

    assertThat(classLoaderConfiguration.getDependencies().stream()
        .filter(dep -> "commons-collections4".equals(dep.getDescriptor().getArtifactId()))
        .findFirst().isPresent(), is(true));
    assertThat(classLoaderConfiguration.getDependencies().stream()
        .filter(dep -> "commons-io".equals(dep.getDescriptor().getArtifactId()))
        .findFirst().isPresent(), is(true));

    assertThat(classLoaderConfiguration.getExportedPackages(), hasItems("org.apache.commons.collections4",
                                                                        "org.apache.commons.io"));
    assertThat(classLoaderConfiguration.getExportedResources(),
               hasItems("META-INF/maven/org.apache.commons/commons-collections4/pom.xml",
                        "META-INF/maven/commons-io/commons-io/pom.xml"));

    assertThat(desc.getPlugins(), hasSize(2));
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
    installArtifactInRepo(getArtifact("dependencies/plugin-with-transitive-dependency"));
    installArtifactInRepo(getArtifact("dependencies/library-1.0.0.pom"));
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-as-system");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies().size(), is(1));

    assertThat(classLoaderConfiguration.getUrls().length, is(1));
  }

  @Test
  public void classLoaderConfigurationWithPluginDependencyWithAnotherPlugin() throws Exception {
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-another-plugin");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies().size(), is(2));
    assertThat(classLoaderConfiguration.getDependencies(),
               hasItems(dependantPluginDependencyMatcher(), emptyPluginDependencyMatcher()));

    assertThat(classLoaderConfiguration.getUrls().length, is(1));
    classLoaderConfiguration.getDependencies().stream()
        .forEach(bundleDependency -> {
          assertThat(asList(classLoaderConfiguration.getUrls()), not(hasItem(bundleDependency.getBundleUri())));
        });
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
    installArtifactInRepo(getArtifact("dependencies/library"));
    installArtifactInRepo(getArtifact("dependencies/library-test-jar"));
    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/same-dep-diff-classifier");

    ClassLoaderConfiguration classLoaderConfiguration = desc.getClassLoaderConfiguration();

    assertThat(classLoaderConfiguration.getDependencies(), contains(
                                                                    bundleDependency("library"),
                                                                    bundleDependency("library")));

    assertThat(stream(classLoaderConfiguration.getUrls()).map(URL::getPath).collect(toList()),
               contains(
                        containsString("same-dep-diff-classifier"),
                        containsString("library-1.0.0.jar"),
                        containsString("library-1.0.0-test-jar.jar")));
  }

  @Test
  public void appWithPluginWithSameDependencyWithDifferentClassifier() throws Exception {
    installArtifactInRepo(getArtifact("dependencies/library"));
    installArtifactInRepo(getArtifact("dependencies/library-test-jar"));
    installArtifactInRepo(getArtifact("dependencies/plugin-with-transitive-dependencies-different-classifier"));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-same-dep-diff-classifier");

    ArtifactPluginDescriptor plugin = desc.getPlugins().iterator().next();
    ClassLoaderConfiguration pluginClassLoderModel = plugin.getClassLoaderConfiguration();

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
    installArtifactInRepo(getArtifact("dependencies/library"));
    installArtifactInRepo(getArtifact("dependencies/library-test-jar"));

    D desc = createArtifactDescriptor(getArtifactRootFolder() + "/plugin-dependency-with-same-dep-diff-classifier-as-additional");

    ArtifactPluginDescriptor plugin = desc.getPlugins().iterator().next();
    ClassLoaderConfiguration pluginClassLoderModel = plugin.getClassLoaderConfiguration();

    assertThat(pluginClassLoderModel.getDependencies(), is(empty()));

    assertThat(stream(pluginClassLoderModel.getUrls()).map(URL::getPath).collect(toList()),
               contains(
                        containsString("test-empty-plugin"),
                        containsString("library-1.0.0.jar"),
                        containsString("library-1.0.0-test-jar.jar")));
  }

  private void installArtifactInRepo(File artifact) throws IOException {
    installedArtifactFiles.addAll(installArtifact(artifact, new File(repositoryLocation.getValue())));
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
    return new ServiceRegistryDescriptorLoaderRepository();
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
