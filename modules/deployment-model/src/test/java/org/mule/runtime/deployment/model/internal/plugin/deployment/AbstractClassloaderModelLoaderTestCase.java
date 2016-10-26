/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.deployment.model.api.plugin.loader.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.loader.Plugin;
import org.mule.runtime.deployment.model.api.plugin.PluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.ClassloaderModel;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;
import org.mule.runtime.deployment.model.internal.plugin.classloadermodel.ClassloaderModelLoader;
import org.mule.runtime.deployment.model.internal.plugin.dependency.DefaultArtifactDependency;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractClassloaderModelLoaderTestCase extends AbstractMuleTestCase {

  protected abstract Plugin getPlugin(String pluginParentFolder, String pluginFolder) throws MalformedPluginException;

  protected abstract String getRuntimeClasses();

  @Parameterized.Parameter(value = 0)
  public String pluginParentFolder;
  /**
   * as plugin.properties do NOT have the plugin's name, we need to know whether we are testing against a properties or
   * a json classloadermodel file to generate a proper plugin's expected name in {@link #assertMinimalRequirementsPlugin(PluginDescriptor, String)}
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
      throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-classes";
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(classloaderModel);
    assertDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependencies()
      throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-dependencies";
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(classloaderModel);
    assertDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedPackages()
      throws MalformedPluginException, MalformedClassloaderModelException {
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel("plugin-dependencies-no-exportedPackages");

    assertExportedResources(classloaderModel);
    assertThat(classloaderModel.getExportedPackages().size(), is(0));
    assertDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedPackagesAndNoExportedResources()
      throws MalformedPluginException, MalformedClassloaderModelException {
    ClassloaderModel classloaderModel =
        getSuccessfulDeploymentModel("plugin-dependencies-no-exportedPackages-and-no-exportedResources");

    assertThat(classloaderModel.getExportedPackages().size(), is(0));
    assertThat(classloaderModel.getExportedResources().size(), is(0));
    assertDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedResources()
      throws MalformedPluginException, MalformedClassloaderModelException {
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel("plugin-dependencies-no-exportedResources");

    assertExportedPackages(classloaderModel);
    assertThat(classloaderModel.getExportedResources().size(), is(0));
    assertDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelEmpty() throws MalformedPluginException, MalformedClassloaderModelException {
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel("plugin-empty");

    assertExportedPackagesAndResources(classloaderModel);
    assertNoDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelLibs() throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-libs";
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(classloaderModel);
    assertDependencies(classloaderModel);
  }

  @Test
  public void testSuccessfulDeploymentModelLibsAndClasses()
      throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-libs-and-classes";
    ClassloaderModel classloaderModel = getSuccessfulDeploymentModel(pluginFolder);

    assertExportedPackagesAndResources(classloaderModel);
    assertDependencies(classloaderModel);
  }

  private void assertClasses(ClassloaderModel classloaderModel) {
    assertThat(classloaderModel.getRuntimeClasses().isPresent(), is(true));
    assertThat(classloaderModel.getRuntimeClasses().get().getFile(), endsWith(getRuntimeClasses()));
  }

  private void assertExportedPackagesAndResources(ClassloaderModel classloaderModel) {
    assertExportedPackages(classloaderModel);
    assertExportedResources(classloaderModel);
  }

  private void assertExportedPackages(ClassloaderModel classloaderModel) {
    assertThat(classloaderModel.getExportedPackages(),
               containsInAnyOrder("org.mule.extension.api", "org.mule.extension.api.exception"));
  }

  private void assertExportedResources(ClassloaderModel classloaderModel) {
    assertThat(classloaderModel.getExportedResources(), containsInAnyOrder("/META-INF/some.file", "/META-INF/other.file"));
  }

  private void assertNoDependencies(ClassloaderModel classloaderModel) {
    assertThat(classloaderModel.getDependencies(), empty());
  }

  private void assertDependencies(ClassloaderModel classloaderModel) {
    if (hasJsonDescriptor) {
      assertThat(classloaderModel.getDependencies().size(), is(1));
      ArtifactDependency[] artifactDependencies = {
          new DefaultArtifactDependency("org.mule.modules", "mule-module-sockets", "4.0.0", "jar", "mule-plugin")
      };
      assertThat(classloaderModel.getDependencies(), containsInAnyOrder(artifactDependencies));
    } else {
      //TODO MULE-10440 until we figure it out how to put groupid, artifactid, version, type and classifier in the plugin.properties dependencies won't be tested
      assertThat(classloaderModel.getDependencies().size(), is(0));
    }
  }

  private ClassloaderModel getSuccessfulDeploymentModel(String pluginFolder)
      throws MalformedPluginException, MalformedClassloaderModelException {
    Plugin plugin = getPlugin(pluginParentFolder, pluginFolder);
    ClassloaderModel classloaderModel = ClassloaderModelLoader.from(plugin);
    assertClasses(classloaderModel);
    return classloaderModel;
  }
}
