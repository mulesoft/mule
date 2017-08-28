/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel.MulePluginModelBuilder;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;

@SmallTest
public class PluginResourcesResolverTestCase extends AbstractMuleTestCase {

  private static final String ORG_MULE_TEST_RUNNER = "org.mule.test.runner";
  private static final String ORG_MULE_TEST_RUNNER_API = "org.mule.test.runner.api";
  private static final String META_INF_RESOURCE_PROPERTIES = "/META-INF/resource.properties";
  private static final String META_INF_ANOTHER_RESOURCE_PROPERTIES = "/META-INF/anotherResource.properties";

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void resolvePluginResourcesForMulePluginWithoutPluginPropertiesDescriptor() {
    PluginUrlClassification mulePluginClassification = newPluginUrlClassification(Collections.<URL>emptyList());
    PluginResourcesResolver resolver = new PluginResourcesResolver();
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(Matchers.contains(MULE_ARTIFACT_JSON_DESCRIPTOR + " couldn't be found for plugin"));
    resolver.resolvePluginResourcesFor(mulePluginClassification);
  }

  @Test
  public void resolvePluginResourcesForMulePlugin() throws Exception {
    MulePluginModelBuilder builder = new MulePluginModelBuilder();
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId("mule")
        .addProperty(EXPORTED_PACKAGES, newArrayList(ORG_MULE_TEST_RUNNER, ORG_MULE_TEST_RUNNER_API))
        .addProperty(EXPORTED_RESOURCES, newArrayList(META_INF_RESOURCE_PROPERTIES, META_INF_ANOTHER_RESOURCE_PROPERTIES))
        .build());
    builder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    MulePluginModel mulePluginModel = builder
        .setName("samplePlugin")
        .setMinMuleVersion("4.0.0")
        .build();
    String mulePluginModelJson = new MulePluginModelJsonSerializer().serialize(mulePluginModel);

    File pluginPropertiesFile =
        new File(new File(new File(temporaryFolder.getRoot(), "META-INF"), "mule-artifact"), MULE_ARTIFACT_JSON_DESCRIPTOR);
    writeStringToFile(pluginPropertiesFile, mulePluginModelJson);
    URL classPathUrl = temporaryFolder.getRoot().toURI().toURL();
    List<URL> urls = newArrayList(classPathUrl);
    PluginUrlClassification mulePluginClassification = newPluginUrlClassification(urls);
    PluginResourcesResolver resolver = new PluginResourcesResolver();

    PluginUrlClassification result = resolver.resolvePluginResourcesFor(mulePluginClassification);

    assertResolvedResources(mulePluginClassification, result);
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
