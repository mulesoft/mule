/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public abstract class AbstractDefaultValuesMuleDeployableModelGeneratorTestCase<M extends MuleDeployableModel>
    extends AbstractMuleTestCase {

  protected static final String DEFAULT_CONFIGS_DIRECTORY = "src/main/mule";

  @Rule
  public ExpectedException expected = none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File muleFolder;

  @Before
  public void before() throws IOException {
    temporaryFolder.create();
    muleFolder = createMuleFolder();
  }

  private File createMuleFolder() {
    final File file = new File(temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY);
    assertThat(file.mkdirs(), is(true));
    return file;
  }

  @Test
  public void emptyModel() {
    M originalModel = getModel("{\"minMuleVersion\": \"4.4.0\"}");

    M model = completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                            emptyList(), emptyList(), emptyList(), emptyList());
    assertThat(model.getMinMuleVersion(), is(originalModel.getMinMuleVersion()));
    assertThat(model.getName(), notNullValue());
    assertThat(model.getName(),
               is(getDescriptor().getGroupId() + ":" + getDescriptor().getArtifactId() + ":" + getDescriptor().getVersion()));
    assertThat(model.getConfigs(), notNullValue());
    assertThat(model.getConfigs(), hasSize(0));
    assertThat(model.getSecureProperties(), notNullValue());
    assertThat(model.getSecureProperties(), hasSize(0));
    assertThat(model.getClassLoaderModelLoaderDescriptor(), notNullValue());
    assertThat(model.getClassLoaderModelLoaderDescriptor().getId(), is("mule"));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_PACKAGES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_PACKAGES),
               hasSize(0));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_RESOURCES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_RESOURCES),
               hasSize(0));
    assertThat(model.isRedeploymentEnabled(), is(true));
    assertThat(model.getRequiredProduct(), is(Product.MULE));
    assertThat(model.getBundleDescriptorLoader(), notNullValue());
    assertThat(model.getBundleDescriptorLoader().getId(), is("mule"));
    assertThat(model.getBundleDescriptorLoader().getAttributes(), aMapWithSize(0));
  }

  @Test
  public void originalModelMustHaveRequiredFields() {
    M originalModel = getModel("{}");

    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("minMuleVersion cannot be null");
    completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                  emptyList(),
                  emptyList(), emptyList(), emptyList());
  }

  @Test
  public void configsDirectoryMustExistInArtifactLocation() {
    M originalModel = getModel("{\"minMuleVersion\": \"4.4.0\"}");

    expected.expect(IllegalArgumentException.class);
    expected.expectMessage(format("Configurations directory 'non-existing-dir' doesn't exist in project location '%s'.",
                                  temporaryFolder.getRoot()));
    completeModel(originalModel, temporaryFolder.getRoot(), "non-existing-dir",
                  emptyList(),
                  emptyList(), emptyList(), emptyList());
  }

  @Test
  public void modelWithResourcesAndMuleConfigs() throws IOException {
    M originalModel = getModel("{\"minMuleVersion\": \"4.4.0\"}");
    List<String> nonConfigResources = singletonList("not-a-config.xml");
    List<String> configs = singletonList("config.xml");
    addResources(nonConfigResources, configs);

    M model =
        completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                      emptyList(), emptyList(), emptyList(),
                      getResources(nonConfigResources, configs));
    assertThat(model.getConfigs(), notNullValue());
    assertThat(model.getConfigs(), hasSize(1));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_RESOURCES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_RESOURCES),
               hasSize(2));
    assertThat(model.getRequiredProduct(), is(Product.MULE));
  }

  @Test
  public void modelWithResourcesAndMuleEeConfigs() throws IOException {
    M originalModel = getModel("{\"minMuleVersion\": \"4.4.0\"}");
    String nonConfigResource = "not-a-config.xml";
    List<String> nonConfigResources = singletonList(nonConfigResource);
    String config = "config.xml";
    List<String> configs = singletonList(config);
    addResources(nonConfigResources, emptyList(), configs);

    M model =
        completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                      emptyList(), emptyList(), emptyList(),
                      getResources(nonConfigResources, configs));
    assertThat(model.getConfigs(), notNullValue());
    assertThat(model.getConfigs(), hasSize(1));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_RESOURCES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_RESOURCES),
               containsInAnyOrder(nonConfigResource, config));
    assertThat(model.getRequiredProduct(), is(Product.MULE_EE));
  }

  @Test
  public void originalModelDefaultValuesArePreserved() throws IOException {
    String name = "original-name";
    List<String> secureProperties = singletonList("original-secure-property");
    boolean redeploymentEnabled = false;
    String originalConfig = "original-config.xml";
    Set<String> configs = singleton(originalConfig);
    Product requiredProduct = Product.MULE_EE;
    String logConfigFile = "original-log-config-file.log";
    String originalBundleDescriptorLoaderKey = "original-key";
    String originalBundleDescriptorLoaderValue = "original-value";

    String originalClassLoaderModelDescriptorLoaderId = "original-class-loader-model-descriptor-loader-id";
    Map<String, Object> classLoaderModelDescriptorLoaderAttributes = new HashMap<>();
    List<String> originalExportedPackages = singletonList("original-exported-package");
    List<String> originalExportedResources = singletonList("original-exported-resource");
    classLoaderModelDescriptorLoaderAttributes.put(EXPORTED_PACKAGES, originalExportedPackages);
    classLoaderModelDescriptorLoaderAttributes.put(EXPORTED_RESOURCES, originalExportedResources);

    M originalModel = getModel("4.4.0", name, requiredProduct,
                               new MuleArtifactLoaderDescriptor(originalClassLoaderModelDescriptorLoaderId,
                                                                classLoaderModelDescriptorLoaderAttributes),
                               new MuleArtifactLoaderDescriptor("", singletonMap(originalBundleDescriptorLoaderKey,
                                                                                 originalBundleDescriptorLoaderValue)),
                               secureProperties, redeploymentEnabled, configs, logConfigFile);

    List<String> nonConfigResources = singletonList("not-a-config.xml");
    List<String> newConfigs = singletonList("config.xml");
    addResources(nonConfigResources, emptyList(), newConfigs);

    M model =
        completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                      emptyList(), emptyList(), emptyList(),
                      getResources(nonConfigResources, newConfigs));
    assertThat(model.getConfigs(), notNullValue());
    assertThat(model.getConfigs(), contains(originalConfig));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_PACKAGES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_PACKAGES),
               is(originalExportedPackages));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_RESOURCES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_RESOURCES),
               is(originalExportedResources));
    assertThat(model.getRequiredProduct(), is(Product.MULE_EE));
    assertThat(model.getBundleDescriptorLoader().getId(), is("mule"));
    assertThat(model.getBundleDescriptorLoader().getAttributes(), aMapWithSize(1));
    assertThat(model.getBundleDescriptorLoader().getAttributes(),
               hasEntry(is(originalBundleDescriptorLoaderKey), is(originalBundleDescriptorLoaderValue)));
    assertThat((String) model.getBundleDescriptorLoader().getAttributes().get(originalBundleDescriptorLoaderKey),
               is(originalBundleDescriptorLoaderValue));
  }

  @Test
  public void availableExportedPackagesNotExportedIfNotPresentInOriginalModelButAvailableResourcesAre() throws IOException {
    M originalModel =
        getModel("{\"minMuleVersion\": \"4.4.0\", \"classLoaderModelLoaderDescriptor\":{}}");

    String nonConfigResource = "not-a-config.xml";
    List<String> nonConfigResources = singletonList(nonConfigResource);
    String config = "config.xml";
    List<String> configs = singletonList(config);
    addResources(nonConfigResources, configs);

    M model =
        completeModel(originalModel, temporaryFolder.getRoot(), DEFAULT_CONFIGS_DIRECTORY,
                      emptyList(), emptyList(), singletonList("some-package"),
                      getResources(nonConfigResources, configs));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(), aMapWithSize(1));
    assertThat(model.getClassLoaderModelLoaderDescriptor().getAttributes(),
               hasEntry(is(EXPORTED_RESOURCES), notNullValue()));
    assertThat((Collection<String>) model.getClassLoaderModelLoaderDescriptor().getAttributes().get(EXPORTED_RESOURCES),
               containsInAnyOrder(nonConfigResource, config));
  }

  protected M getModel(String jsonString) {
    return getModelDeserializer()
        .deserialize(jsonString);
  }

  protected abstract M completeModel(M originalModel,
                                     File artifactLocation,
                                     String modelConfigsDirectory,
                                     List<BundleDependency> modelDependencies,
                                     List<BundleDependency> modelMuleRuntimeDependencies,
                                     List<String> modelPackages,
                                     List<String> modelResources);

  protected abstract M getModel(String minMuleVersion, String name, Product requiredProduct,
                                MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                MuleArtifactLoaderDescriptor bundleDescriptorLoaderDescriptor, List<String> secureProperties,
                                boolean redeploymentEnabled, Set<String> configs, String logConfigFile);

  protected abstract BundleDescriptor getDescriptor();

  private List<String> getResources(List<String> nonConfigResources, List<String> configs) {
    List<String> resources = new ArrayList<>();
    resources.addAll(nonConfigResources);
    resources.addAll(configs);

    return resources;
  }

  private void addResources(List<String> resources, List<String> muleConfigs) throws IOException {
    addResources(resources, muleConfigs, emptyList());
  }

  private void addResources(List<String> resources, List<String> muleConfigs, List<String> muleEeConfigs) throws IOException {
    for (String resource : resources) {
      creteResource(resource);
    }

    for (String config : muleConfigs) {
      creteConfig(config);
    }

    for (String config : muleEeConfigs) {
      creteEeConfig(config);
    }
  }

  private void creteResource(String resource) throws IOException {
    createFile(muleFolder, resource, "");
  }

  private void creteConfig(String config) throws IOException {
    createFile(muleFolder, config, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><mule></mule>");
  }

  private void creteEeConfig(String config) throws IOException {
    createFile(muleFolder, config,
               "<?xml version=\"1.0\" encoding=\"UTF-8\"?><mule xmlns:ee=\"http://www.mulesoft.org/schema/mule/ee/core\"></mule>");
  }

  private void createFile(File folder, String name, String content) throws IOException {
    File tempInputFile = new File(folder, name);
    tempInputFile.createNewFile();
    tempInputFile.deleteOnExit();
    writeByteArrayToFile(tempInputFile, content.getBytes(StandardCharsets.UTF_8));
  }

  protected abstract AbstractMuleArtifactModelJsonSerializer<M> getModelDeserializer();

}
