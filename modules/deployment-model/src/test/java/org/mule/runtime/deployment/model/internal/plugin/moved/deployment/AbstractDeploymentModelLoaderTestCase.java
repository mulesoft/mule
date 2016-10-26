/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.deployment.model.api.plugin.moved.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.moved.Plugin;
import org.mule.runtime.deployment.model.api.plugin.moved.PluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.moved.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.moved.dependency.Scope;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedDeploymentModelException;
import org.mule.runtime.deployment.model.internal.plugin.moved.dependency.DefaultArtifactDependency;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractDeploymentModelLoaderTestCase {

  protected abstract Plugin getPlugin(String pluginParentFolder, String pluginFolder) throws MalformedPluginException;

  protected abstract String getRuntimeClasses();

  @Parameterized.Parameter(value = 0)
  public String pluginParentFolder;
  /**
   * as plugin.properties do NOT have the plugin's name, we need to know whether we are testing against a properties or
   * a json descriptor file to generate a proper plugin's expected name in {@link #assertMinimalRequirementsPlugin(PluginDescriptor, String)}
   */
  @Parameterized.Parameter(value = 1)
  public boolean hasJsonDescriptor;

  @Parameterized.Parameters(name = "{index}:{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList("plugin-successful-structures",
                         "plugin-properties-successful-structures")
        .stream()
        .map(ele -> new Object[] {ele, "plugin-successful-structures".equals(ele)})
        .collect(Collectors.toList());
  }

  @Test
  public void testSuccessfulDeploymentModelClasses()
      throws MalformedPluginException, MalformedDeploymentModelException {
    String pluginFolder = "plugin-classes";
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(deploymentModel);
    assertDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependencies()
      throws MalformedPluginException, MalformedDeploymentModelException {
    String pluginFolder = "plugin-dependencies";
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(deploymentModel);
    assertDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedPackages()
      throws MalformedPluginException, MalformedDeploymentModelException {
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel("plugin-dependencies-no-exportedPackages");

    assertExportedResources(deploymentModel);
    assertThat(deploymentModel.getExportedPackages().size(), is(0));
    assertDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedPackagesAndNoExportedResources()
      throws MalformedPluginException, MalformedDeploymentModelException {
    DeploymentModel deploymentModel =
        getSuccessfulDeploymentModel("plugin-dependencies-no-exportedPackages-and-no-exportedResources");

    assertThat(deploymentModel.getExportedPackages().size(), is(0));
    assertThat(deploymentModel.getExportedResources().size(), is(0));
    assertDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedResources()
      throws MalformedPluginException, MalformedDeploymentModelException {
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel("plugin-dependencies-no-exportedResources");

    assertExportedPackages(deploymentModel);
    assertThat(deploymentModel.getExportedResources().size(), is(0));
    assertDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelEmpty() throws MalformedPluginException, MalformedDeploymentModelException {
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel("plugin-empty");

    assertExportedPackagesAndResources(deploymentModel);
    assertNoDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelLibs() throws MalformedPluginException, MalformedDeploymentModelException {
    String pluginFolder = "plugin-libs";
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(deploymentModel);
    assertDependencies(deploymentModel);
  }

  @Test
  public void testSuccessfulDeploymentModelLibsAndClasses()
      throws MalformedPluginException, MalformedDeploymentModelException {
    String pluginFolder = "plugin-libs-and-classes";
    DeploymentModel deploymentModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(deploymentModel);
    assertDependencies(deploymentModel);
  }

  private void assertClasses(DeploymentModel deploymentModel) {
    assertThat(deploymentModel.getRuntimeClasses().isPresent(), is(true));
    assertThat(deploymentModel.getRuntimeClasses().get().getFile(), endsWith(getRuntimeClasses()));
  }

  private void assertExportedPackagesAndResources(DeploymentModel deploymentModel) {
    assertExportedPackages(deploymentModel);
    assertExportedResources(deploymentModel);
  }

  private void assertExportedPackages(DeploymentModel deploymentModel) {
    assertThat(deploymentModel.getExportedPackages(),
               containsInAnyOrder("org.mule.extension.api", "org.mule.extension.api.exception"));
  }

  private void assertExportedResources(DeploymentModel deploymentModel) {
    assertThat(deploymentModel.getExportedResources(), containsInAnyOrder("/META-INF/some.file", "/META-INF/other.file"));
  }

  private void assertNoDependencies(DeploymentModel deploymentModel) {
    assertThat(deploymentModel.getDependencies(), empty());
  }

  private void assertDependencies(DeploymentModel deploymentModel) {
    if (hasJsonDescriptor) {
      assertThat(deploymentModel.getDependencies().size(), is(4));
      ArtifactDependency[] artifactDependencies = {
          new DefaultArtifactDependency("org.mule.modules", "mule-module-sockets", "4.0.0", "jar", "mule-plugin", Scope.PROVIDED),
          new DefaultArtifactDependency("org.mule", "mule-core", "4.0.0", "jar", null, Scope.PROVIDED),
          new DefaultArtifactDependency("com.google.guava", "guava", null, "jar", null, null),
          new DefaultArtifactDependency("junit", "junit", "4.11", "jar", null, Scope.TEST)
      };
      assertThat(deploymentModel.getDependencies(), containsInAnyOrder(artifactDependencies));
    } else {
      //TODO MULE-10785 until we figure it out how to put groupid, artifactid, version, type and classifier in the plugin.properties dependencies won't be tested
      assertThat(deploymentModel.getDependencies().size(), is(0));
    }
  }

  private DeploymentModel getSuccessfulDeploymentModel(String pluginFolder)
      throws MalformedPluginException, MalformedDeploymentModelException {
    Plugin plugin = getPlugin(pluginParentFolder, pluginFolder);
    DeploymentModel deploymentModel = DeploymentModelLoader.from(plugin);
    assertClasses(deploymentModel);
    return deploymentModel;
  }
}
