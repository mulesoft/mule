/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.maven.client.api.model.BundleScope.COMPILE;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

import io.qameta.allure.Description;

public class DeployableMavenClassLoaderModelLoaderTestCase {

  private static final String APPS_FOLDER = "apps";
  private static final String PATCHED_PLUGIN_APP = "patched-plugin-app";
  private static final String PATCHED_JAR_APP_WHITESPACES = "patched jar app";
  private static final String PATCHED_JAR_APP = "patched-jar-app";
  private static final String PATCHED_JAR_AND_PLUGIN_APP = "patched-jar-and-plugin-app";
  private static final org.mule.maven.client.api.model.BundleDependency API_BUNDLE =
      createBundleDependency("some.company", "dummy-api", "1.0.0", "raml");
  private static final org.mule.maven.client.api.model.BundleDependency LIB_BUNDLE =
      createBundleDependency("other.company", "dummy-lib", "1.2.0", "raml-fragment");
  private static final org.mule.maven.client.api.model.BundleDependency TRAIT_BUNDLE =
      createBundleDependency("some.company", "dummy-trait", "1.0.3", "raml-fragment");
  private static final String POM_FORMAT = "%s-%s.pom";
  private final List<org.mule.maven.client.api.model.BundleDependency> BASE_DEPENDENCIES =
      asList(API_BUNDLE, LIB_BUNDLE, TRAIT_BUNDLE);

  private final MavenClient mockMavenClient = mock(MavenClient.class, RETURNS_DEEP_STUBS);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  @Description("Heavyweight packaged apps will deploy ok with shared libraries information in classloader-model.json")
  public void sharedLibrariesAreReadFromModel() throws Exception {
    URL patchedAppUrl = getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, "shared-libraries-in-model").toString());
    ClassLoaderModel classLoaderModel = buildClassLoaderModel(toFile(patchedAppUrl));
    assertThat(classLoaderModel.getExportedResources(), is(not(empty())));
  }

  @Test
  public void patchedApplicationLoadsUpdatedConnector() throws InvalidDescriptorLoaderException, IOException {
    testPatchedDependency(PATCHED_PLUGIN_APP, 3, "mule-objectstore-connector", "1.0.1");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJar() throws InvalidDescriptorLoaderException, IOException {
    testPatchedDependency(PATCHED_JAR_APP, 2, "commons-cli", "1.4");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJarAndPlugin() throws InvalidDescriptorLoaderException, IOException {
    testPatchedDependency(PATCHED_JAR_AND_PLUGIN_APP, 3, "commons-cli", "1.4");
    testPatchedDependency(PATCHED_JAR_AND_PLUGIN_APP, 3, "mule-sockets-connector", "1.5.8");
  }

  @Test
  public void patchedApplicationWithWhitespaces() throws InvalidDescriptorLoaderException, IOException {
    ClassLoaderModel classLoaderModel = buildClassLoaderModel(
                                                              new File(toFile(getClass().getClassLoader()
                                                                  .getResource(Paths.get(APPS_FOLDER).toString())),
                                                                       PATCHED_JAR_APP_WHITESPACES));
    assertThat(classLoaderModel.getUrls().length, is(2));
    // It was not escaping the URL for rootArtifact
    assertThat(classLoaderModel.getUrls()[0], equalTo(
                                                      toFile(getClass().getClassLoader().getResource(Paths
                                                          .get(APPS_FOLDER, PATCHED_JAR_APP_WHITESPACES).toString())).toURI()
                                                              .toURL()));
  }

  /**
   * Validates several versions of the same API definition artifact are considered in the model. Dependencies are as follows:
   *
   * app |- dep |- api |- lib \- trait \- lib'
   */
  @Test
  public void applicationWithDuplicatedApiArtifactDependencies() throws Exception {
    org.mule.maven.client.api.model.BundleDependency regularDependency = createBundleDependency("a", "b", "1.0.0-SNAPSHOT", null);
    org.mule.maven.client.api.model.BundleDependency minorLibBundle =
        createBundleDependency("other.company", "dummy-lib", "1.1.0", "raml-fragment");

    when(mockMavenClient.resolveArtifactDependencies(any(), anyBoolean(), anyBoolean(), any(), any(), any()))
        .thenReturn(asList(regularDependency, API_BUNDLE, LIB_BUNDLE, TRAIT_BUNDLE, minorLibBundle));

    ClassLoaderModel classLoaderModel = buildAndValidateModel(5);

    URL[] urls = classLoaderModel.getUrls();
    assertThat(urls, hasItemInArray(getDependencyUrl(minorLibBundle)));
    assertThat(urls, hasItemInArray(getDependencyUrl(regularDependency)));
  }

  /**
   * Validates that API dependencies are fully analyzed, even when they contain loops among each other. Dependencies are as
   * follows:
   *
   * app \- api |- lib | |- trait \- trait \- lib
   */
  @Test
  public void applicationWithLoopedApiArtifactDependencies() throws Exception {
    when(mockMavenClient.resolveArtifactDependencies(any(), anyBoolean(), anyBoolean(), any(), any(), any()))
        .thenReturn(asList(API_BUNDLE, LIB_BUNDLE, TRAIT_BUNDLE));

    buildAndValidateModel(3);
  }

  private ClassLoaderModel buildAndValidateModel(int expectedDependencies) throws Exception {
    File app = toFile(getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, "no-dependencies").toString()));

    MavenConfiguration mockMavenConfiguration = mock(MavenConfiguration.class, RETURNS_DEEP_STUBS);
    when(mockMavenConfiguration.getLocalMavenRepositoryLocation()).thenReturn(temporaryFolder.newFolder());
    when(mockMavenClient.getMavenConfiguration()).thenReturn(mockMavenConfiguration);

    ClassLoaderModel classLoaderModel = buildClassLoaderModel(app);
    assertThat(classLoaderModel.getDependencies(), hasSize(expectedDependencies));
    URL[] urls = classLoaderModel.getUrls();
    assertThat(urls, hasItemInArray(app.toURI().toURL()));
    assertThat(urls, hasItemInArray(getDependencyUrl(API_BUNDLE)));
    assertThat(urls, hasItemInArray(getDependencyUrl(LIB_BUNDLE)));
    assertThat(urls, hasItemInArray(getDependencyUrl(TRAIT_BUNDLE)));
    return classLoaderModel;
  }

  private static org.mule.maven.client.api.model.BundleDependency createBundleDependency(String groupId, String artifactId,
                                                                                         String version, String classifier) {
    return new org.mule.maven.client.api.model.BundleDependency.Builder()
        .setDescriptor(new BundleDescriptor.Builder()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setClassifier(classifier)
            .setBaseVersion(version)
            .setVersion(version)
            .build())
        .setBundleUri(getDummyUriFor(groupId, artifactId, version))
        .setScope(COMPILE)
        .build();
  }

  private URL getDependencyUrl(org.mule.maven.client.api.model.BundleDependency dependency) throws Exception {
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
      throws InvalidDescriptorLoaderException, IOException {
    URL patchedAppUrl = getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, application).toString());
    ClassLoaderModel classLoaderModel = buildClassLoaderModel(toFile(patchedAppUrl));
    Set<BundleDependency> dependencies = classLoaderModel.getDependencies();
    assertThat(dependencies, hasSize(totalExpectedDependencies));
    List<BundleDependency> connectorsFound = dependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(patchedArtifactId))
        .collect(toList());
    assertThat(connectorsFound, hasSize(1));
    assertThat(connectorsFound.get(0).getDescriptor().getVersion(), is(patchedArtifactVersion));
  }

  private ClassLoaderModel buildClassLoaderModel(File rootApplication)
      throws InvalidDescriptorLoaderException {
    DeployableMavenClassLoaderModelLoader deployableMavenClassLoaderModelLoader =
        new DeployableMavenClassLoaderModelLoader(mockMavenClient, () -> {
          final JarExplorer jarExplorer = mock(JarExplorer.class);
          when(jarExplorer.explore(any(URI.class))).thenReturn(new JarInfo(emptySet(), emptySet(), emptyList()));
          return jarExplorer;
        });

    Map<String, Object> attributes =
        ImmutableMap.of(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.class.getName(),
                        new org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder()
                            .setGroupId("groupId")
                            .setArtifactId("artifactId")
                            .setVersion("1.0.0")
                            .setType("jar")
                            .setClassifier("mule-application")
                            .build());
    return deployableMavenClassLoaderModelLoader.load(rootApplication, attributes, APP);
  }
}
