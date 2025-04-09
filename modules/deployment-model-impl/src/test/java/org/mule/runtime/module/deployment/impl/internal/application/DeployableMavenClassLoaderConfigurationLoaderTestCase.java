/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.maven.pom.parser.api.model.BundleScope.COMPILE;
import static org.mule.runtime.api.artifact.ArtifactType.APP;
import static org.mule.runtime.api.artifact.ArtifactType.PLUGIN;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION_LOADER;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginExtendedClassLoaderConfigurationAttributes;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginExtendedDeploymentProperties;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginMavenClassLoaderConfigurationLoader;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Stories({@Story(CLASSLOADER_CONFIGURATION_LOADER), @Story(CLASSLOADER_CONFIGURATION)})
public class DeployableMavenClassLoaderConfigurationLoaderTestCase {

  private static final String APPS_FOLDER = "apps";
  private static final String PATCHED_PLUGIN_APP = "patched-plugin-app";
  private static final String PATCHED_JAR_APP_WHITESPACES = "patched jar app";
  private static final String PATCHED_JAR_APP = "patched-jar-app";
  private static final String PATCHED_JAR_AND_PLUGIN_APP = "patched-jar-and-plugin-app";
  private static final org.mule.maven.pom.parser.api.model.BundleDependency API_BUNDLE =
      createBundleDependency("some.company", "dummy-api", "1.0.0", "raml");
  private static final org.mule.maven.pom.parser.api.model.BundleDependency LIB_BUNDLE =
      createBundleDependency("other.company", "dummy-lib", "1.2.0", "raml-fragment");
  private static final org.mule.maven.pom.parser.api.model.BundleDependency TRAIT_BUNDLE =
      createBundleDependency("some.company", "dummy-trait", "1.0.3", "raml-fragment");
  private static final String POM_FORMAT = "%s-%s.pom";
  private static final String SOURCE_TEST_CLASSES = "/source-test-classes";
  private final List<org.mule.maven.pom.parser.api.model.BundleDependency> BASE_DEPENDENCIES =
      asList(API_BUNDLE, LIB_BUNDLE, TRAIT_BUNDLE);

  private final MavenClient mockMavenClient = mock(MavenClient.class, RETURNS_DEEP_STUBS);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  @Description("Heavyweight packaged apps will deploy ok with shared libraries information in classloader-model.json")
  public void sharedLibrariesAreReadFromModel() throws Exception {
    URL patchedAppUrl = getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, "shared-libraries-in-model").toString());
    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(toFile(patchedAppUrl));
    assertThat(classLoaderConfiguration.getExportedResources(), is(not(empty())));
  }

  private void doTestPackagesResourcesLoaded(URL appUrl, boolean useJarExplorer) throws Exception {
    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(toFile(appUrl), ImmutableMap
        .of(EXPORTED_PACKAGES, ImmutableList.of("com.mycompany.api"), EXPORTED_RESOURCES, ImmutableList.of("tls.properties")),
                                                                                      useJarExplorer);

    assertThat(classLoaderConfiguration.getExportedPackages(), hasItems("com.mycompany.api", "org.apache.commons.csv"));
    assertThat(classLoaderConfiguration.getLocalPackages(),
               everyItem(not(in(newArrayList("com.mycompany.api", "org.apache.commons.csv")))));
    assertThat(classLoaderConfiguration.getLocalPackages(), hasItems("com.mycompany.internal", "org.apache.commons.io"));

    assertThat(classLoaderConfiguration.getExportedResources(), hasItem("tls.properties"));
    assertThat(classLoaderConfiguration.getLocalResources(), everyItem(not(in(newArrayList("tls.properties")))));
    assertThat(classLoaderConfiguration.getLocalResources(), hasItem("META-INF/maven/com/mycompany/test/pom.xml"));

    Optional<BundleDependency> mulePluginBundleDependency = classLoaderConfiguration.getDependencies().stream().filter(
                                                                                                                       bundleDependency -> MULE_PLUGIN_CLASSIFIER
                                                                                                                           .equals(bundleDependency
                                                                                                                               .getDescriptor()
                                                                                                                               .getClassifier()
                                                                                                                               .orElse(null)))
        .findFirst();
    assertThat(mulePluginBundleDependency.isPresent(), is(true));

    BundleDependency bundleDependency = mulePluginBundleDependency.get();

    ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor("app");
    applicationDescriptor.setClassLoaderConfiguration(classLoaderConfiguration);
    PluginExtendedDeploymentProperties pluginExtendedDeploymentProperties =
        new PluginExtendedDeploymentProperties(new Properties(), bundleDependency.getDescriptor(), applicationDescriptor);
    PluginExtendedClassLoaderConfigurationAttributes pluginExtendedClassLoaderConfigurationAttributes =
        new PluginExtendedClassLoaderConfigurationAttributes(pluginExtendedDeploymentProperties, applicationDescriptor);
    pluginExtendedClassLoaderConfigurationAttributes
        .put(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.class.getName(),
             bundleDependency.getDescriptor());

    pluginExtendedClassLoaderConfigurationAttributes.put(EXPORTED_PACKAGES, ImmutableList.of("org.mule.tests.simple.plugin.api"));
    pluginExtendedClassLoaderConfigurationAttributes.put(EXPORTED_RESOURCES, ImmutableList.of("simple-plugin.properties"));

    ClassLoaderConfiguration pluginClassLoaderConfiguration =
        buildPluginClassLoaderConfiguration(toFile(bundleDependency.getBundleUri().toURL()),
                                            pluginExtendedClassLoaderConfigurationAttributes);
    assertThat(pluginClassLoaderConfiguration.getExportedPackages(), hasItem("org.mule.tests.simple.plugin.api"));
    assertThat(pluginClassLoaderConfiguration.getLocalPackages(),
               everyItem(not(in(newArrayList("org.mule.tests.simple.plugin.api")))));
    assertThat(pluginClassLoaderConfiguration.getLocalPackages(),
               hasItems("org.mule.tests.simple.plugin.internal", "org.apache.commons.collections"));

    assertThat(pluginClassLoaderConfiguration.getExportedResources(), hasItem("simple-plugin.properties"));
    assertThat(pluginClassLoaderConfiguration.getLocalResources(),
               everyItem(not(in(newArrayList("simple-plugin.properties")))));
    assertThat(pluginClassLoaderConfiguration.getLocalResources(),
               hasItems("META-INF/simple-plugin/internal.txt", "META-INF/maven/commons-collections/commons-collections/pom.xml"));
  }

  @Test
  public void packagesResourcesLoadedFromClassLoaderModelJson() throws Exception {
    doTestPackagesResourcesLoaded(getClass().getClassLoader()
        .getResource(Paths.get(APPS_FOLDER,
                               "packages-resources-loaded-from-class-loader-model")
            .toString()), false);
  }

  @Test
  public void packagesResourcesNotLoadedFromClassLoaderModelJson() throws Exception {
    URL appUrl = getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER,
                                                                   "packages-resources-not-loaded-from-class-loader-model")
        .toString());
    File appFolder = toFile(appUrl);
    File simpleClassApi = new CompilerUtils.SingleClassCompiler()
        .compile(getResourceFile(SOURCE_TEST_CLASSES + "/com/mycompany/api/SimpleClass.java"));
    File apiPackageFolder = Paths.get(appFolder.getAbsolutePath(), "com", "mycompany", "api").toFile();
    if (!apiPackageFolder.exists()) {
      assertThat(apiPackageFolder.mkdirs(), is(true));
    }
    copyFile(simpleClassApi, new File(apiPackageFolder, "SimpleClass.class"), false);

    File simpleClassInternal = new CompilerUtils.SingleClassCompiler()
        .compile(getResourceFile(SOURCE_TEST_CLASSES + "/com/mycompany/internal/SimpleClass.java"));
    File internalPackageFolder = Paths.get(appFolder.getAbsolutePath(), "com", "mycompany", "internal").toFile();
    if (!internalPackageFolder.exists()) {
      assertThat(internalPackageFolder.mkdirs(), is(true));
    }
    copyFile(simpleClassInternal, new File(internalPackageFolder, "SimpleClass.class"), false);

    doTestPackagesResourcesLoaded(appUrl, true);
  }

  @Test
  public void classLoaderModel120WithSharedDepsWithMissingPackagesAndResourcesIsCorrectlyLoaded() throws Exception {
    validateMissingPackagesAndResources("classloader-model-120-with-shared-deps-empty-packages");
  }

  @Test
  public void classLoaderModel120WithAdditionalDepsWithMissingPackagesAndResourcesIsCorrectlyLoaded() throws Exception {
    validateMissingPackagesAndResources("classloader-model-120-additional-deps-empty-packages");
  }

  private void validateMissingPackagesAndResources(String appName) throws Exception {
    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(
                                                                                      toFile(
                                                                                             getClass().getClassLoader()
                                                                                                 .getResource(Paths
                                                                                                     .get(APPS_FOLDER, appName)
                                                                                                     .toString())),
                                                                                      ImmutableMap.of(EXPORTED_PACKAGES,
                                                                                                      ImmutableList
                                                                                                          .of("com.mycompany.api"),
                                                                                                      EXPORTED_RESOURCES,
                                                                                                      ImmutableList
                                                                                                          .of("tls.properties")),
                                                                                      false);

    Optional<BundleDependency> mulePluginBundleDependency = classLoaderConfiguration.getDependencies().stream().filter(
                                                                                                                       bundleDependency -> MULE_PLUGIN_CLASSIFIER
                                                                                                                           .equals(bundleDependency
                                                                                                                               .getDescriptor()
                                                                                                                               .getClassifier()
                                                                                                                               .orElse(null)))
        .findFirst();
    assertThat(mulePluginBundleDependency.isPresent(), is(true));

    BundleDependency bundleDependency = mulePluginBundleDependency.get();

    ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor("app");
    applicationDescriptor.setClassLoaderConfiguration(classLoaderConfiguration);
    PluginExtendedDeploymentProperties pluginExtendedDeploymentProperties =
        new PluginExtendedDeploymentProperties(new Properties(), bundleDependency.getDescriptor(), applicationDescriptor);
    PluginExtendedClassLoaderConfigurationAttributes pluginExtendedClassLoaderConfigurationAttributes =
        new PluginExtendedClassLoaderConfigurationAttributes(pluginExtendedDeploymentProperties, applicationDescriptor);
    pluginExtendedClassLoaderConfigurationAttributes
        .put(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.class.getName(),
             bundleDependency.getDescriptor());

    pluginExtendedClassLoaderConfigurationAttributes.put(EXPORTED_PACKAGES, ImmutableList.of("org.mule.tests.simple.plugin.api"));
    pluginExtendedClassLoaderConfigurationAttributes.put(EXPORTED_RESOURCES, ImmutableList.of("simple-plugin.properties"));

    ClassLoaderConfiguration pluginClassLoaderConfiguration =
        buildPluginClassLoaderConfiguration(toFile(bundleDependency.getBundleUri().toURL()),
                                            pluginExtendedClassLoaderConfigurationAttributes);
    assertThat(pluginClassLoaderConfiguration.getLocalPackages(),
               everyItem(not(in(newArrayList("org.mule.tests.simple.plugin.api")))));
    assertThat(pluginClassLoaderConfiguration.getLocalPackages(), contains("org.mule.tests.simple.plugin.internal"));

    assertThat(pluginClassLoaderConfiguration.getLocalResources(),
               everyItem(not(in(newArrayList("simple-plugin.properties")))));
    assertThat(pluginClassLoaderConfiguration.getLocalResources(), contains("META-INF/simple-plugin/internal.txt"));
  }

  @Test
  public void patchedApplicationLoadsUpdatedConnector() throws InvalidDescriptorLoaderException {
    testPatchedDependency(PATCHED_PLUGIN_APP, 3, "mule-objectstore-connector", "1.1.0");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJar() throws InvalidDescriptorLoaderException {
    testPatchedDependency(PATCHED_JAR_APP, 2, "commons-cli", "1.4");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJarAndPlugin() throws InvalidDescriptorLoaderException {
    testPatchedDependency(PATCHED_JAR_AND_PLUGIN_APP, 2, "commons-cli", "1.4");
    testPatchedDependency(PATCHED_JAR_AND_PLUGIN_APP, 2, "mule-objectstore-connector", "1.1.0");
  }

  @Test
  public void patchedApplicationWithWhitespaces() throws InvalidDescriptorLoaderException, IOException {
    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(
                                                                                      new File(toFile(getClass().getClassLoader()
                                                                                          .getResource(Paths.get(APPS_FOLDER)
                                                                                              .toString())),
                                                                                               PATCHED_JAR_APP_WHITESPACES));
    assertThat(classLoaderConfiguration.getUrls().length, is(2));
    // It was not escaping the URL for rootArtifact
    assertThat(classLoaderConfiguration.getUrls()[0], equalTo(
                                                              toFile(getClass().getClassLoader().getResource(Paths
                                                                  .get(APPS_FOLDER, PATCHED_JAR_APP_WHITESPACES).toString()))
                                                                  .toURI()
                                                                  .toURL()));
  }

  /**
   * Validates several versions of the same API definition artifact are considered in the model. Dependencies are as follows:
   *
   * <pre>
   * {@code
   * app
   * |- dep
   * \- api
   *    |- lib
   *    \- trait
   *       \- lib
   * }
   * </pre>
   */
  @Test
  public void applicationWithDuplicatedApiArtifactDependencies() throws Exception {
    org.mule.maven.pom.parser.api.model.BundleDependency regularDependency =
        createBundleDependency("a", "b", "1.0.0-SNAPSHOT", null);
    org.mule.maven.pom.parser.api.model.BundleDependency minorLibBundle =
        createBundleDependency("other.company", "dummy-lib", "1.1.0", "raml-fragment");

    when(mockMavenClient.resolveArtifactDependencies(any(), anyBoolean(), anyBoolean(), any(), any(), any()))
        .thenReturn(asList(regularDependency, API_BUNDLE, LIB_BUNDLE, TRAIT_BUNDLE, minorLibBundle));

    ClassLoaderConfiguration classLoaderConfiguration = buildAndValidateModel(5);

    URL[] urls = classLoaderConfiguration.getUrls();
    assertThat(urls, hasItemInArray(getDependencyUrl(minorLibBundle)));
    assertThat(urls, hasItemInArray(getDependencyUrl(regularDependency)));
  }

  /**
   * Validates that API dependencies are fully analyzed, even when they contain loops among each other. Dependencies are as
   * follows:
   *
   * <pre>
   * {@code
   * app
   * \- api
   *    |- lib
   *    |  \- trait
   *    \- trait
   *       \- lib
   * }
   * </pre>
   */
  @Test
  public void applicationWithLoopedApiArtifactDependencies() throws Exception {
    when(mockMavenClient.resolveArtifactDependencies(any(), anyBoolean(), anyBoolean(), any(), any(), any()))
        .thenReturn(asList(API_BUNDLE, LIB_BUNDLE, TRAIT_BUNDLE));

    buildAndValidateModel(3);
  }

  @Test
  public void urlOrderIsCorrect() throws Exception {
    final String groupId = "test.gid";
    final String version1 = "1.0.0";

    final String transitiveDependency1Id = "transitiveDependency1";
    org.mule.maven.pom.parser.api.model.BundleDependency transitive1 =
        createBundleDependency(groupId, transitiveDependency1Id, version1, null);

    final String transitiveDependency2Id = "transitiveDependency2";
    org.mule.maven.pom.parser.api.model.BundleDependency transitive2 =
        createBundleDependency(groupId, transitiveDependency2Id, version1, null);

    final String depWithTransitive1Id = "depT1";
    org.mule.maven.pom.parser.api.model.BundleDependency dependencyWithTransitive1 =
        createBundleDependency(groupId, depWithTransitive1Id, version1, null, singletonList(transitive1));

    final String depWithTransitive2Id = "depT2";
    org.mule.maven.pom.parser.api.model.BundleDependency dependencyWithTransitive2 =
        createBundleDependency(groupId, depWithTransitive2Id, version1, null, singletonList(transitive2));

    List<org.mule.maven.pom.parser.api.model.BundleDependency> resolvedDependencies =
        asList(dependencyWithTransitive1, transitive1, dependencyWithTransitive2, transitive2);

    when(mockMavenClient.resolveArtifactDependencies(
                                                     any(),
                                                     anyBoolean(),
                                                     anyBoolean(),
                                                     any(),
                                                     any(),
                                                     any()))
        .thenReturn(resolvedDependencies);

    File app = toFile(getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, "no-dependencies").toString()));

    MavenConfiguration mockMavenConfiguration = mock(MavenConfiguration.class, RETURNS_DEEP_STUBS);
    when(mockMavenConfiguration.getLocalMavenRepositoryLocation()).thenReturn(temporaryFolder.newFolder());
    when(mockMavenClient.getMavenConfiguration()).thenReturn(mockMavenConfiguration);

    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(app);
    assertThat(classLoaderConfiguration.getUrls().length, equalTo(5));
    for (int i = 1; i < classLoaderConfiguration.getUrls().length; i++) { // The first one does not count because it's the main
                                                                          // artifact.
      org.mule.maven.pom.parser.api.model.BundleDependency dependency = resolvedDependencies.get(i - 1);
      URL url = getDummyUriFor(dependency.getDescriptor().getGroupId(),
                               dependency.getDescriptor().getArtifactId(),
                               dependency.getDescriptor().getVersion())
          .toURL();
      assertThat(classLoaderConfiguration.getUrls()[i], is(equalTo(url)));
    }
  }

  private ClassLoaderConfiguration buildAndValidateModel(int expectedDependencies) throws Exception {
    File app = toFile(getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, "no-dependencies").toString()));

    MavenConfiguration mockMavenConfiguration = mock(MavenConfiguration.class, RETURNS_DEEP_STUBS);
    when(mockMavenConfiguration.getLocalMavenRepositoryLocation()).thenReturn(temporaryFolder.newFolder());
    when(mockMavenClient.getMavenConfiguration()).thenReturn(mockMavenConfiguration);

    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(app);
    assertThat(classLoaderConfiguration.getDependencies(), hasSize(expectedDependencies));
    URL[] urls = classLoaderConfiguration.getUrls();
    assertThat(urls, hasItemInArray(app.toURI().toURL()));
    assertThat(urls, hasItemInArray(getDependencyUrl(API_BUNDLE)));
    assertThat(urls, hasItemInArray(getDependencyUrl(LIB_BUNDLE)));
    assertThat(urls, hasItemInArray(getDependencyUrl(TRAIT_BUNDLE)));
    return classLoaderConfiguration;
  }

  private static org.mule.maven.pom.parser.api.model.BundleDependency createBundleDependency(String groupId, String artifactId,
                                                                                             String version, String classifier) {
    return createBundleDependency(groupId, artifactId, version, classifier, emptyList());
  }

  private static org.mule.maven.pom.parser.api.model.BundleDependency createBundleDependency(String groupId, String artifactId,
                                                                                             String version, String classifier,
                                                                                             List<org.mule.maven.pom.parser.api.model.BundleDependency> transitiveDependencies) {
    org.mule.maven.pom.parser.api.model.BundleDependency.Builder bundleDependencyBuilder =
        new org.mule.maven.pom.parser.api.model.BundleDependency.Builder();
    bundleDependencyBuilder.setDescriptor(new BundleDescriptor.Builder()
        .setGroupId(groupId)
        .setArtifactId(artifactId)
        .setClassifier(classifier)
        .setBaseVersion(version)
        .setVersion(version)
        .build())
        .setBundleUri(getDummyUriFor(groupId, artifactId, version))
        .setScope(COMPILE);
    transitiveDependencies.forEach(bundleDependencyBuilder::addTransitiveDependency);
    return bundleDependencyBuilder.build();
  }

  private URL getDependencyUrl(org.mule.maven.pom.parser.api.model.BundleDependency dependency) throws Exception {
    BundleDescriptor descriptor = dependency.getDescriptor();
    return getDummyUriFor(descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion()).toURL();
  }

  private static URI getDummyUriFor(String groupId, String artifactId, String version) {
    try {
      return new URI(format("file:/%s/%s/%s", groupId, artifactId, version));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void testPatchedDependency(String application, int totalExpectedDependencies, String patchedArtifactId,
                                     String patchedArtifactVersion)
      throws InvalidDescriptorLoaderException {
    URL patchedAppUrl = getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, application).toString());
    ClassLoaderConfiguration classLoaderConfiguration = buildClassLoaderConfiguration(toFile(patchedAppUrl));
    Set<BundleDependency> dependencies = classLoaderConfiguration.getDependencies();
    assertThat(dependencies, hasSize(totalExpectedDependencies));
    List<BundleDependency> connectorsFound = dependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(patchedArtifactId))
        .collect(toList());
    assertThat(connectorsFound, hasSize(1));
    assertThat(connectorsFound.get(0).getDescriptor().getVersion(), is(patchedArtifactVersion));
  }

  private ClassLoaderConfiguration buildClassLoaderConfiguration(File rootApplication)
      throws InvalidDescriptorLoaderException {
    return buildClassLoaderConfiguration(rootApplication, () -> {
      final JarExplorer jarExplorer = mock(JarExplorer.class);
      when(jarExplorer.explore(any(URI.class))).thenReturn(new JarInfo(emptySet(), emptySet(), emptyList()));
      return jarExplorer;
    }, emptyMap());
  }

  private ClassLoaderConfiguration buildClassLoaderConfiguration(File rootApplication, Map<String, Object> attributes,
                                                                 boolean useJarExplorer)
      throws InvalidDescriptorLoaderException {
    return buildClassLoaderConfiguration(rootApplication, () -> {
      if (useJarExplorer) {
        return new FileJarExplorer();
      }
      throw new AssertionError("JarExplorer should not be used");
    }, attributes);
  }

  private ClassLoaderConfiguration buildClassLoaderConfiguration(File rootApplication, Supplier<JarExplorer> supplier,
                                                                 Map<String, Object> attributes)
      throws InvalidDescriptorLoaderException {
    DeployableMavenClassLoaderConfigurationLoader deployableMavenClassLoaderConfigurationLoader =
        new DeployableMavenClassLoaderConfigurationLoader(of(mockMavenClient), supplier);

    Map<String, Object> mergedAttributes =
        ImmutableMap.<String, Object>builder()
            .put(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.class.getName(),
                 new org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder()
                     .setGroupId("groupId")
                     .setArtifactId("artifactId")
                     .setVersion("1.0.0")
                     .setType("jar")
                     .setClassifier("mule-application")
                     .build())
            .putAll(attributes)
            .build();
    return deployableMavenClassLoaderConfigurationLoader.load(rootApplication, mergedAttributes, APP);
  }

  public ClassLoaderConfiguration buildPluginClassLoaderConfiguration(File pluginLocation, Map<String, Object> attributes)
      throws InvalidDescriptorLoaderException {
    PluginMavenClassLoaderConfigurationLoader pluginMavenClassLoaderConfigurationLoader =
        new PluginMavenClassLoaderConfigurationLoader(of(mockMavenClient));

    return pluginMavenClassLoaderConfigurationLoader.load(pluginLocation, attributes, PLUGIN);

  }

  protected static File getResourceFile(String resource) throws URISyntaxException {
    return new File(DeployableMavenClassLoaderConfigurationLoaderTestCase.class.getResource(resource).toURI());
  }

}
