/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.join;
import static org.apache.commons.io.FileUtils.write;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.manifest.ExtensionManifestBuilder;
import org.mule.runtime.extension.api.persistence.manifest.ExtensionManifestXmlSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;

@SmallTest
public class PluginResourcesResolverTestCase extends AbstractMuleTestCase {

  private static final String ARTIFACT_EXPORT_RESOURCES_KEY = "artifact.export.resources";
  private static final String ARTIFACT_EXPORT_CLASS_PACKAGES_KEY = "artifact.export.classPackages";

  private static final String ORG_MULE_TEST_RUNNER = "org.mule.test.runner";
  private static final String ORG_MULE_TEST_RUNNER_API = "org.mule.test.runner.api";
  private static final String META_INF_RESOURCE_PROPERTIES = "/META-INF/resource.properties";
  private static final String META_INF_ANOTHER_RESOURCE_PROPERTIES = "/META-INF/anotherResource.properties";

  private static final String JUNIT_MOCK_EXTENSION_MANIFEST_DESCRIPTION = "JUnit Mock Extension Manifest";
  private static final String VERSION = "4.0";
  private static final String SNAPSHOT_VERSION = "4.0-SNAPSHOT";
  private static final String PLUGIN_PROPERTIES = "plugin.properties";

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ExtensionManager extensionManager;

  @Before
  public void before() throws Exception {
    extensionManager = mock(ExtensionManager.class);
  }

  @Test
  public void resolvePluginResourcesForMulePluginWithoutPluginPropertiesDescriptor() {
    PluginUrlClassification mulePluginClassification = newPluginUrlClassification(Collections.<URL>emptyList());
    PluginResourcesResolver resolver = new PluginResourcesResolver(extensionManager);
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(Matchers.contains(PLUGIN_PROPERTIES + " couldn't be found for plugin"));
    resolver.resolvePluginResourcesFor(mulePluginClassification);
  }

  @Test
  public void resolvePluginResourcesForMulePlugin() throws Exception {
    Properties pluginProperties = new Properties();
    pluginProperties.setProperty(ARTIFACT_EXPORT_RESOURCES_KEY, join(",", newArrayList(META_INF_RESOURCE_PROPERTIES,
                                                                                       META_INF_ANOTHER_RESOURCE_PROPERTIES)));
    pluginProperties.setProperty(ARTIFACT_EXPORT_CLASS_PACKAGES_KEY,
                                 join(",", newArrayList(ORG_MULE_TEST_RUNNER, ORG_MULE_TEST_RUNNER_API)));

    File pluginPropertiesFile = temporaryFolder.newFile(PLUGIN_PROPERTIES);
    pluginProperties.store(new FileWriter(pluginPropertiesFile), "Writing " + PLUGIN_PROPERTIES);
    URL classPathUrl = temporaryFolder.getRoot().toURI().toURL();
    List<URL> urls = newArrayList(classPathUrl);
    PluginUrlClassification mulePluginClassification = newPluginUrlClassification(urls);
    PluginResourcesResolver resolver = new PluginResourcesResolver(extensionManager);

    PluginUrlClassification result = resolver.resolvePluginResourcesFor(mulePluginClassification);
    verify(extensionManager, never()).parseExtensionManifestXml(anyObject());

    assertResolvedResources(mulePluginClassification, result);
  }

  @Test
  public void resolvePluginResourcesForMuleExtensionPlugin() throws Exception {
    ExtensionManifestBuilder builder = new ExtensionManifestBuilder()
        .setName(MULE_PLUGIN_CLASSIFIER)
        .setDescription(JUNIT_MOCK_EXTENSION_MANIFEST_DESCRIPTION)
        .setMinMuleVersion(new MuleVersion(VERSION))
        .setVersion(SNAPSHOT_VERSION)
        .addExportedPackages(newArrayList(ORG_MULE_TEST_RUNNER, ORG_MULE_TEST_RUNNER_API))
        .addExportedResources(newArrayList(META_INF_RESOURCE_PROPERTIES, META_INF_ANOTHER_RESOURCE_PROPERTIES));
    builder.withDescriber().setId(MULE_PLUGIN_CLASSIFIER);

    ExtensionManifest extensionManifest = builder.build();
    String manifestXml = new ExtensionManifestXmlSerializer().serialize(extensionManifest);
    File manifestFile = new File(temporaryFolder.getRoot(), "META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
    write(manifestFile, manifestXml);

    URL classPathUrl = manifestFile.getParentFile().getParentFile().toURI().toURL();
    List<URL> urls = newArrayList(classPathUrl);
    PluginUrlClassification extensionPluginClassification = newPluginUrlClassification(urls);
    PluginResourcesResolver resolver = new PluginResourcesResolver(extensionManager);
    URL manifestUrl = manifestFile.toURI().toURL();
    when(extensionManager.parseExtensionManifestXml(manifestUrl)).thenReturn(extensionManifest);

    PluginUrlClassification result = resolver.resolvePluginResourcesFor(extensionPluginClassification);
    verify(extensionManager).parseExtensionManifestXml(manifestUrl);

    assertResolvedResources(extensionPluginClassification, result);
  }

  private PluginUrlClassification newPluginUrlClassification(List<URL> urls) {
    return new PluginUrlClassification(MULE_PLUGIN_CLASSIFIER, urls, Collections.<Class>emptyList(),
                                       Collections.<String>emptyList());
  }

  private void assertResolvedResources(PluginUrlClassification original, PluginUrlClassification resolved) {
    assertThat(resolved.getName(), equalTo(original.getName()));

    assertThat(resolved.getUrls(), hasSize(1));
    assertThat(resolved.getUrls(), equalTo(original.getUrls()));

    assertThat(resolved.getExportClasses(), emptyCollectionOf(Class.class));

    assertThat(resolved.getExportedPackages(), hasSize(2));
    assertThat(resolved.getExportedPackages(), contains(ORG_MULE_TEST_RUNNER, ORG_MULE_TEST_RUNNER_API));

    assertThat(resolved.getExportedResources(), hasSize(2));
    assertThat(resolved.getExportedResources(), contains(META_INF_RESOURCE_PROPERTIES, META_INF_ANOTHER_RESOURCE_PROPERTIES));

  }
}
