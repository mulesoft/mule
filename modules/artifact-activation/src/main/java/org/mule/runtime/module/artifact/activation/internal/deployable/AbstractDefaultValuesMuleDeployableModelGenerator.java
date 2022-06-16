/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;

import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModelBuilder;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.internal.descriptor.ConfigurationsResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.ConfigurationsResolver.DeployableConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;

/**
 * Generates default values for any non-defined fields in a {@link MuleDeployableModel}.
 */
public abstract class AbstractDefaultValuesMuleDeployableModelGenerator<M extends MuleDeployableModel, B extends AbstractMuleArtifactModelBuilder<B, M>> {

  private static final String MULE_ID = "mule";
  private static final MulePluginModelJsonSerializer serializer = new MulePluginModelJsonSerializer();

  private final M originalMuleDeployableModel;
  private final File artifactLocation;
  private final String modelConfigsDirectory;
  private final BundleDescriptor modelBundleDescriptor;
  private final List<BundleDependency> modelDependencies;
  private final List<BundleDependency> modelMuleRuntimeDependencies;
  private final List<String> modelPackages;
  private final List<String> modelResources;
  private final ConfigurationsResolver configurationsResolver;
  private final B builder;

  /**
   * Creates a new instance with the provided parameters.
   *
   * @param originalMuleDeployableModel  mule deployable model to be completed with default values.
   * @param artifactLocation             the folder containing the project files.
   * @param modelConfigsDirectory        directory containing configuration files.
   * @param modelBundleDescriptor        contains the GAV of the modeled project.
   * @param modelDependencies            dependencies of the modeled project.
   * @param modelMuleRuntimeDependencies dependencies of the modeled project that will be provided by the environment.
   * @param modelPackages                available packages containing java classes in the modeled project
   * @param modelResources               available resources in the modeled project.
   * @param builder                      builder for the new mule deployable model based on the original.
   *
   * @throws IllegalArgumentException if the {@code modelConfigsDirectory} doesn't exist within the {@code artifactLocation}.
   */
  public AbstractDefaultValuesMuleDeployableModelGenerator(M originalMuleDeployableModel,
                                                           File artifactLocation,
                                                           String modelConfigsDirectory,
                                                           BundleDescriptor modelBundleDescriptor,
                                                           List<BundleDependency> modelDependencies,
                                                           List<BundleDependency> modelMuleRuntimeDependencies,
                                                           List<String> modelPackages,
                                                           List<String> modelResources,
                                                           ConfigurationsResolver configurationsResolver,
                                                           B builder) {
    if (!exists(artifactLocation.toPath().resolve(modelConfigsDirectory))) {
      throw new IllegalArgumentException(format("Configurations directory '%s' doesn't exist in project location '%s'.",
                                                modelConfigsDirectory, artifactLocation));
    }

    this.originalMuleDeployableModel = originalMuleDeployableModel;
    this.artifactLocation = artifactLocation;
    this.modelConfigsDirectory = modelConfigsDirectory;
    this.modelBundleDescriptor = modelBundleDescriptor;
    this.modelDependencies = modelDependencies;
    this.modelMuleRuntimeDependencies = modelMuleRuntimeDependencies;
    this.modelPackages = modelPackages;
    this.modelResources = modelResources;
    this.configurationsResolver = configurationsResolver;
    this.builder = builder;
  }

  public M generate() {
    setBuilderWithRequiredValues();
    setBuilderWithDefaultName();
    setBuilderWithDefaultSecureProperties();
    setBuilderWithDefaultRedeploymentEnabled();

    List<DeployableConfiguration> configs = getConfigs();
    setBuilderWithDefaultConfigsValue(configs);
    setBuilderWithDefaultRequiredProduct(configs);

    setBuilderWithDefaultExportedPackagesAndResourcesValue();
    setBuilderWithIncludeTestDependencies();
    setBuilderWithDefaultBundleDescriptorLoaderValue();

    if (originalMuleDeployableModel.getLogConfigFile() != null) {
      doSetBuilderWithConfigFile(originalMuleDeployableModel.getLogConfigFile());
    }

    doSpecificConfiguration();

    return builder.build();
  }

  protected abstract void doSetBuilderWithConfigFile(String logConfigFile);

  /**
   * Generates the values that correspond only to the specific mule model type.
   */
  protected void doSpecificConfiguration() {
    // Do nothing
  }

  protected M getOriginalMuleDeployableModel() {
    return originalMuleDeployableModel;
  }

  protected B getBuilder() {
    return builder;
  }

  /**
   * Sets the builder with all the required values, i.e. those that must always be present in a mule model.
   */
  private void setBuilderWithRequiredValues() {
    builder.setMinMuleVersion(originalMuleDeployableModel.getMinMuleVersion());
  }

  /**
   * Sets the name as {@code groupId:artifactId:version} if not set in the {@code originalMuleDeployableModel}.
   */
  private void setBuilderWithDefaultName() {
    String name = originalMuleDeployableModel.getName();
    if (isEmpty(name)) {
      BundleDescriptor applicationDescriptor = modelBundleDescriptor;
      name = applicationDescriptor.getGroupId() + ":" + applicationDescriptor.getArtifactId() + ":"
          + applicationDescriptor.getVersion();
    }

    builder.setName(name);
  }

  /**
   * Sets the builder with an empty list of secure properties if not set in {@code originalMuleDeployableModel}.
   */
  private void setBuilderWithDefaultSecureProperties() {
    doSetBuilderWithDefaultSecureProperties(originalMuleDeployableModel.getSecureProperties() == null ? emptyList()
        : originalMuleDeployableModel.getSecureProperties());
  }

  protected abstract void doSetBuilderWithDefaultSecureProperties(List<String> secureProperties);

  /**
   * Updates the {@code redeploymentEnabled} field of the builder based on the {@code originalMuleDeployableModel}.
   */
  private void setBuilderWithDefaultRedeploymentEnabled() {
    doSetBuilderWithDefaultRedeploymentEnabled(originalMuleDeployableModel.isRedeploymentEnabled());
  }

  protected abstract void doSetBuilderWithDefaultRedeploymentEnabled(boolean redeploymentEnabled);

  /**
   * Sets the builder with the configurations obtained from the model and the {@code originalMuleDeployableModel}, and the test
   * configs.
   *
   * @param configs configurations to set in the builder.
   */
  private void setBuilderWithDefaultConfigsValue(List<DeployableConfiguration> configs) {
    Set<String> defaultConfigs = configs.stream().map(DeployableConfiguration::getName).collect(toSet());
    // TODO W-11203142 - add test configs
    doSetBuilderWithDefaultConfigsValue(defaultConfigs);
  }

  protected abstract void doSetBuilderWithDefaultConfigsValue(Set<String> defaultConfigs);

  /**
   * Resolves the configurations to use taking the ones from the {@code originalMuleDeployableModel} if present, or calculates the
   * ones available in the model.
   * 
   * @return a {@link List} with the project configurations.
   */
  private List<DeployableConfiguration> getConfigs() {
    if (originalMuleDeployableModel.getConfigs() != null) {
      return configurationsResolver
          .resolve(resolveCandidateConfigsPaths(new ArrayList<>(originalMuleDeployableModel.getConfigs())));
    } else {
      return configurationsResolver.resolve(resolveCandidateConfigsPaths(modelResources));
    }
  }

  private List<File> resolveCandidateConfigsPaths(List<String> candidateConfigsFileNames) {
    return candidateConfigsFileNames.stream().map(candidateConfigFileName -> artifactLocation.getAbsoluteFile().toPath()
        .resolve(modelConfigsDirectory).resolve(candidateConfigFileName).toFile()).collect(toList());
  }

  /**
   * Sets the builder with the default required {@link Product}.
   *
   * @param configs project configurations.
   */
  private void setBuilderWithDefaultRequiredProduct(List<DeployableConfiguration> configs) {
    Product requiredProduct = originalMuleDeployableModel.getRequiredProduct();
    if (requiredProduct == null) {
      requiredProduct = Product.MULE;
      if (doesSomeConfigRequireEE(configs)
          || anyMulePluginInDependenciesRequiresEE()
          || anyProvidedDependencyRequiresEE()) {
        requiredProduct = Product.MULE_EE;
      }
    }

    builder.setRequiredProduct(requiredProduct);
  }

  private boolean doesSomeConfigRequireEE(List<DeployableConfiguration> configs) {
    return configs.stream().anyMatch(config -> config.getRequiredProduct().equals(Product.MULE_EE));
  }

  private boolean anyMulePluginInDependenciesRequiresEE() {
    return modelDependencies.stream().filter(dep -> dep.getDescriptor().isPlugin() && dep.getScope().equals(COMPILE))
        .map(org.mule.runtime.module.artifact.api.descriptor.BundleDependency::getBundleUri).anyMatch(this::mulePluginRequiresEE);
  }

  private boolean mulePluginRequiresEE(URI uri) {
    try {
      JarFile pluginJar = new JarFile(uri.getPath());
      JarEntry muleArtifactDescriptor = pluginJar.getJarEntry("META-INF/mule-artifact/mule-artifact.json");
      InputStream is = pluginJar.getInputStream(muleArtifactDescriptor);
      String muleArtifactJson = IOUtils.toString(is, StandardCharsets.UTF_8);
      MulePluginModel mulePluginApplicationModel = serializer.deserialize(muleArtifactJson);
      Product requiredProduct = mulePluginApplicationModel.getRequiredProduct();
      return requiredProduct != null && requiredProduct.equals(Product.MULE_EE);
    } catch (IOException e) {
      return false;
    }
  }

  private boolean anyProvidedDependencyRequiresEE() {
    return modelMuleRuntimeDependencies.stream()
        .anyMatch(coordinates -> coordinates.getDescriptor().getGroupId().startsWith("com.mulesoft.mule"));
  }

  /**
   * Sets the {@code exportedPackages} and {@code exportedResources} properties in the class loader model loader descriptor of the
   * builder. The {@code exportedResources} are updated with the test resources even if the field was already present in the
   * {@code originalMuleDeployableModel}.
   */
  private void setBuilderWithDefaultExportedPackagesAndResourcesValue() {
    MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor;
    if (originalMuleDeployableModel.getClassLoaderModelLoaderDescriptor() != null) {
      Map<String, Object> originalAttributes = originalMuleDeployableModel.getClassLoaderModelLoaderDescriptor().getAttributes();
      List<String> exportedResources = new ArrayList<>();

      if (originalAttributes != null && originalAttributes.get(EXPORTED_RESOURCES) != null) {
        exportedResources.addAll((Collection<String>) originalAttributes.get(EXPORTED_RESOURCES));
      } else {
        exportedResources.addAll(modelResources);
      }
      // TODO W-11203142 - include test resources

      Map<String, Object> attributesCopy =
          getUpdatedAttributes(originalMuleDeployableModel.getClassLoaderModelLoaderDescriptor(), EXPORTED_RESOURCES,
                               new ArrayList<>(exportedResources));

      classLoaderModelLoaderDescriptor =
          new MuleArtifactLoaderDescriptor(originalMuleDeployableModel.getClassLoaderModelLoaderDescriptor().getId(),
                                           attributesCopy);
    } else {
      classLoaderModelLoaderDescriptor = new MuleArtifactLoaderDescriptorBuilder()
          .setId(MULE_ID)
          .addProperty(EXPORTED_PACKAGES, modelPackages)
          .addProperty(EXPORTED_RESOURCES, modelResources)
          .build();
    }

    builder.withClassLoaderModelDescriptorLoader(classLoaderModelLoaderDescriptor);
  }

  /**
   * Updates the builder's class loader model loader descriptor to include test dependencies if necessary.
   */
  private void setBuilderWithIncludeTestDependencies() {
    // TODO W-11203142 - include test dependencies if necessary
  }

  private Map<String, Object> getUpdatedAttributes(MuleArtifactLoaderDescriptor descriptorLoader, String attribute,
                                                   Object value) {
    Map<String, Object> originalAttributes = descriptorLoader.getAttributes();
    Map<String, Object> attributesCopy = new HashMap<>();
    if (originalAttributes != null) {
      attributesCopy.putAll(originalAttributes);
    }
    attributesCopy.put(attribute, value);
    return attributesCopy;
  }

  /**
   * Sets the builder {@code bundleDescriptorLoader} field with default value based on the {@code originalMuleDeployableModel}.
   */
  private void setBuilderWithDefaultBundleDescriptorLoaderValue() {
    MuleArtifactLoaderDescriptor bundleDescriptorLoader = originalMuleDeployableModel.getBundleDescriptorLoader();
    builder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_ID,
                                                                        bundleDescriptorLoader == null
                                                                            || bundleDescriptorLoader.getAttributes() == null
                                                                                ? new HashMap()
                                                                                : bundleDescriptorLoader.getAttributes()));
  }

}
