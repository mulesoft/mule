/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.deployment.model.api.plugin.descriptor.Descriptor;
import org.mule.runtime.deployment.model.api.plugin.loader.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.PluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class SuccessfulAbstractPluginTestCase extends AbstractMuleTestCase {

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

  protected abstract PluginDescriptor getPluginDescriptor(String parentFolder, String pluginFolder)
      throws MalformedPluginException;

  @Test
  public void testSuccessfulDeploymentModelClasses()
      throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-classes";
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin(pluginFolder);

    assertClassloaderDescriptor(pluginDescriptor);
    assertExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelDependencies()
      throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-dependencies";
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin(pluginFolder);

    assertClassloaderDescriptor(pluginDescriptor);
    assertExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedPackages()
      throws MalformedPluginException, MalformedClassloaderModelException {
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin("plugin-dependencies-no-exportedPackages");

    assertClassloaderDescriptorJustResources(pluginDescriptor);
    assertNoExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedPackagesAndNoExportedResources()
      throws MalformedPluginException, MalformedClassloaderModelException {
    PluginDescriptor pluginDescriptor =
        getSuccessfulPlugin("plugin-dependencies-no-exportedPackages-and-no-exportedResources");

    assertClassloaderDescriptorNoResourcesNoPackages(pluginDescriptor);
    assertNoExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelDependenciesNoExportedResources()
      throws MalformedPluginException, MalformedClassloaderModelException {
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin("plugin-dependencies-no-exportedResources");

    assertClassloaderDescriptorJustPackages(pluginDescriptor);
    assertNoExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelEmpty() throws MalformedPluginException, MalformedClassloaderModelException {
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin("plugin-empty");

    assertClassloaderDescriptor(pluginDescriptor);
    assertNoExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelLibs() throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-libs";
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin(pluginFolder);

    assertClassloaderDescriptor(pluginDescriptor);
    assertExtensionModelDescriptor(pluginDescriptor);
  }

  @Test
  public void testSuccessfulDeploymentModelLibsAndClasses()
      throws MalformedPluginException, MalformedClassloaderModelException {
    String pluginFolder = "plugin-libs-and-classes";
    PluginDescriptor pluginDescriptor = getSuccessfulPlugin(pluginFolder);

    assertClassloaderDescriptor(pluginDescriptor);
    assertExtensionModelDescriptor(pluginDescriptor);
  }

  private void assertMinimalRequirementsPlugin(PluginDescriptor pluginDescriptor, String pluginParentFolder) {
    String expectedPluginName = getPluginName(pluginParentFolder);
    Assert.assertThat(pluginDescriptor.getName(), Is.is(expectedPluginName));
    Assert.assertThat(pluginDescriptor.getMinMuleVersion(), Is.is(new MuleVersion("4.0.0")));
  }

  protected String getPluginName(String pluginParentFolder) {
    return hasJsonDescriptor ? "plugin-name" : pluginParentFolder;
  }

  protected void assertClassloaderDescriptor(PluginDescriptor pluginDescriptor) {
    Descriptor classloaderModelDescriptor = pluginDescriptor.getClassloaderModelDescriptor();

    if (hasJsonDescriptor) {
      assertThat(classloaderModelDescriptor.getId(), is("maven"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(2));
      assertThat(classloaderModelDescriptor.getAttributes().get("exportedPackages"), instanceOf(List.class));
      assertThat((List<String>) classloaderModelDescriptor.getAttributes().get("exportedPackages"),
                 containsInAnyOrder("org.mule.extension.api", "org.mule.extension.api.exception"));
      assertThat(classloaderModelDescriptor.getAttributes().get("exportedResources"), instanceOf(List.class));
      assertThat((List<String>) classloaderModelDescriptor.getAttributes().get("exportedResources"),
                 containsInAnyOrder("/META-INF/some.file", "/META-INF/other.file"));
    } else {
      assertThat(classloaderModelDescriptor.getId(), is("pluginproperties"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(0));
    }
  }

  protected void assertClassloaderDescriptorJustResources(PluginDescriptor pluginDescriptor) {
    Descriptor classloaderModelDescriptor = pluginDescriptor.getClassloaderModelDescriptor();

    if (hasJsonDescriptor) {
      assertThat(classloaderModelDescriptor.getId(), is("maven"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(1));
      assertThat(classloaderModelDescriptor.getAttributes().get("exportedResources"), instanceOf(List.class));
      assertThat((List<String>) classloaderModelDescriptor.getAttributes().get("exportedResources"),
                 containsInAnyOrder("/META-INF/some.file", "/META-INF/other.file"));
    } else {
      assertThat(classloaderModelDescriptor.getId(), is("pluginproperties"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(0));
    }
  }

  protected void assertClassloaderDescriptorJustPackages(PluginDescriptor pluginDescriptor) {
    Descriptor classloaderModelDescriptor = pluginDescriptor.getClassloaderModelDescriptor();

    if (hasJsonDescriptor) {
      assertThat(classloaderModelDescriptor.getId(), is("maven"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(1));
      assertThat(classloaderModelDescriptor.getAttributes().get("exportedPackages"), instanceOf(List.class));
      assertThat((List<String>) classloaderModelDescriptor.getAttributes().get("exportedPackages"),
                 containsInAnyOrder("org.mule.extension.api", "org.mule.extension.api.exception"));
    } else {
      assertThat(classloaderModelDescriptor.getId(), is("pluginproperties"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(0));
    }
  }

  protected void assertClassloaderDescriptorNoResourcesNoPackages(PluginDescriptor pluginDescriptor) {
    Descriptor classloaderModelDescriptor = pluginDescriptor.getClassloaderModelDescriptor();

    if (hasJsonDescriptor) {
      assertThat(classloaderModelDescriptor.getId(), is("maven"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(0));
    } else {
      assertThat(classloaderModelDescriptor.getId(), is("pluginproperties"));
      assertThat(classloaderModelDescriptor.getAttributes().size(), is(0));
    }
  }

  protected void assertExtensionModelDescriptor(PluginDescriptor pluginDescriptor) {
    Optional<Descriptor> extensionModelDescriptor = pluginDescriptor.getExtensionModelDescriptor();

    if (hasJsonDescriptor) {
      assertThat(extensionModelDescriptor.isPresent(), is(true));
      assertThat(extensionModelDescriptor.get().getId(), is("annotationsExtensionModelLoader"));
      assertThat(extensionModelDescriptor.get().getAttributes().size(), is(1));
      assertThat(extensionModelDescriptor.get().getAttributes().get("class"),
                 is("org.mule.extension.internal.ExtensionConnector"));
    } else {
      assertThat(extensionModelDescriptor.isPresent(), is(false));
    }
  }

  protected void assertNoExtensionModelDescriptor(PluginDescriptor pluginDescriptor) {
    assertThat(pluginDescriptor.getExtensionModelDescriptor().isPresent(), is(false));
  }

  private PluginDescriptor getSuccessfulPlugin(String pluginFolder) throws MalformedPluginException {
    PluginDescriptor pluginDescriptor = getPluginDescriptor(pluginParentFolder, pluginFolder);
    assertMinimalRequirementsPlugin(pluginDescriptor, pluginFolder);
    return pluginDescriptor;
  }
}
