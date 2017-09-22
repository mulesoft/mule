/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import org.mule.maven.client.api.LocalRepositorySupplierFactory;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DeployableMavenClassLoaderModelLoaderTestCase {

  private static final String APPS_FOLDER = "apps";
  private static final String PATCHED_PLUGIN_APP = "patched-plugin-app";
  private static final String PATCHED_JAR_APP = "patched-jar-app";
  private static final String PATCHED_JAR_AND_PLUGIN_APP = "patched-jar-and-plugin-app";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MavenClient mockMavenClient = mock(MavenClient.class);
  private LocalRepositorySupplierFactory mockLocalRepository = mock(LocalRepositorySupplierFactory.class);

  @Test
  public void patchedApplicationLoadsUpdatedConnector() throws InvalidDescriptorLoaderException {
    testPatchedDependency(PATCHED_PLUGIN_APP, 3, "mule-objectstore-connector", "1.0.1");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJar() throws InvalidDescriptorLoaderException {
    testPatchedDependency(PATCHED_JAR_APP, 2, "commons-cli", "1.4");
  }

  @Test
  public void patchedApplicationLoadsUpdatedJarAndPlugin() throws InvalidDescriptorLoaderException {
    testPatchedDependency(PATCHED_JAR_AND_PLUGIN_APP, 3, "commons-cli", "1.4");
    testPatchedDependency(PATCHED_JAR_AND_PLUGIN_APP, 3, "mule-sockets-connector", "1.5.8");
  }

  private void testPatchedDependency(String application, int totalExpectedDependencies, String patchedArtifactId,
                                     String patchedArtifactVersion)
      throws InvalidDescriptorLoaderException {
    DeployableMavenClassLoaderModelLoader deployableMavenClassLoaderModelLoader =
        new DeployableMavenClassLoaderModelLoader(mockMavenClient, mockLocalRepository);

    URL patchedAppUrl = getClass().getClassLoader().getResource(Paths.get(APPS_FOLDER, application).toString());
    ClassLoaderModel classLoaderModel =
        deployableMavenClassLoaderModelLoader.load(FileUtils.toFile(patchedAppUrl), emptyMap(), APP);
    Set<BundleDependency> dependencies = classLoaderModel.getDependencies();
    assertThat(dependencies, hasSize(totalExpectedDependencies));
    List<BundleDependency> connectorsFound = dependencies.stream()
        .filter(bundleDependency -> bundleDependency.getDescriptor().getArtifactId().equals(patchedArtifactId))
        .collect(Collectors.toList());
    assertThat(connectorsFound, hasSize(1));
    assertThat(connectorsFound.get(0).getDescriptor().getVersion(), is(patchedArtifactVersion));
  }

}
