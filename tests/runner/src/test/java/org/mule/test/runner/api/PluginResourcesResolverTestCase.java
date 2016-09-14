/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

public class PluginResourcesResolverTestCase extends AbstractMuleTestCase {

  private static final String MULE_PLUGIN = "mule-plugin";
  private static final String PLUGIN_RESOURCES_RESOLVER = "plugin-resources-resolver/";
  private static final String MULE_PLUGIN_RESOURCES_BASE_FOLDER = PLUGIN_RESOURCES_RESOLVER + "mule-plugin/";
  private static final String MULE_EXTENSION_PLUGIN_RESOURCES_BASE_FOLDER = PLUGIN_RESOURCES_RESOLVER + "mule-extension/";
  private static final String ORG_MULE_TEST_RUNNER = "org.mule.test.runner";
  private static final String ORG_MULE_TEST_RUNNER_API = "org.mule.test.runner.api";
  private static final String META_INF_RESOURCE_PROPERTIES = "/META-INF/resource.properties";
  private static final String META_INF_ANOTHER_RESOURCE_PROPERTIES = "/META-INF/anotherResource.properties";

  private ExtensionManagerAdapter extensionManager;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() throws Exception {
    extensionManager = mock(ExtensionManagerAdapter.class);
  }

  @Test
  public void resolvePluginResourcesForMulePluginWithoutPluginPropertiesDescriptor() {
    PluginUrlClassification mulePluginClassification = newPluginUrlClassification(Collections.<URL>emptyList());
    PluginResourcesResolver resolver = new PluginResourcesResolver(extensionManager);
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(Matchers.contains("plugin.properties couldn't be found for plugin"));
    resolver.resolvePluginResourcesFor(mulePluginClassification);
  }

  @Test
  public void resolvePluginResourcesForMulePlugin() throws Exception {
    List<URL> urls = buildUrl(MULE_PLUGIN_RESOURCES_BASE_FOLDER);
    PluginUrlClassification mulePluginClassification = newPluginUrlClassification(urls);
    PluginResourcesResolver resolver = new PluginResourcesResolver(extensionManager);

    PluginUrlClassification result = resolver.resolvePluginResourcesFor(mulePluginClassification);
    verify(extensionManager, never()).parseExtensionManifestXml(anyObject());

    assertResolvedResources(mulePluginClassification, result);
  }

  @Test
  public void resolvePluginResourcesForMuleExtensionPlugin() throws Exception {
    List<URL> urls = buildUrl(MULE_EXTENSION_PLUGIN_RESOURCES_BASE_FOLDER);
    PluginUrlClassification extensionPluginClassification = newPluginUrlClassification(urls);
    PluginResourcesResolver resolver = new PluginResourcesResolver(extensionManager);
    ExtensionManifest extensionManifest = mock(ExtensionManifest.class);
    when(extensionManifest.getExportedPackages()).thenReturn(newArrayList(ORG_MULE_TEST_RUNNER, ORG_MULE_TEST_RUNNER_API));
    when(extensionManifest.getExportedResources()).thenReturn(newArrayList(META_INF_RESOURCE_PROPERTIES,
                                                                           META_INF_ANOTHER_RESOURCE_PROPERTIES));
    final URL manifestUrl =
        new File(urls.get(0).getFile(), "META-INF/" + ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME).toURI().toURL();
    when(extensionManager.parseExtensionManifestXml(manifestUrl)).thenReturn(extensionManifest);

    PluginUrlClassification result = resolver.resolvePluginResourcesFor(extensionPluginClassification);
    verify(extensionManager).parseExtensionManifestXml(manifestUrl);
    verify(extensionManifest).getExportedResources();
    verify(extensionManifest).getExportedPackages();

    assertResolvedResources(extensionPluginClassification, result);
  }

  private PluginUrlClassification newPluginUrlClassification(List<URL> urls) {
    return new PluginUrlClassification(MULE_PLUGIN, urls, Collections.<Class>emptyList(), Collections.<String>emptyList());
  }

  private List<URL> buildUrl(String resourceBaseFolder) throws Exception {
    final URL resource = getClass().getClassLoader().getResource(resourceBaseFolder);
    assertThat(resource, notNullValue());

    return newArrayList(resource.toURI().toURL());
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
