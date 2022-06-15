/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.internal.dsl.DslConstants.EE_NAMESPACE;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;

import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModelBuilder;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Generates default values for any non-defined fields in a {@link MuleDeployableModel}.
 */
public abstract class AbstractDefaultValuesMuleDeployableModelGenerator<M extends MuleDeployableModel, B extends AbstractMuleArtifactModelBuilder<B, M>> {

  private static final String CONFIG_FILE_EXTENSION = ".xml";
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
    this.builder = builder;
  }

  public M generate() {
    setBuilderWithRequiredValues();
    setBuilderWithDefaultName();
    setBuilderWithDefaultSecureProperties();
    setBuilderWithDefaultRedeploymentEnabled();

    Map<String, Document> configs = getConfigs();
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
  private void setBuilderWithDefaultConfigsValue(Map<String, Document> configs) {
    Set<String> defaultConfigs = configs.keySet();
    // TODO W-11203142 - add test configs
    doSetBuilderWithDefaultConfigsValue(defaultConfigs);
  }

  protected abstract void doSetBuilderWithDefaultConfigsValue(Set<String> defaultConfigs);

  /**
   * Resolves the configurations to use taking the ones from the {@code originalMuleDeployableModel} if present, or calculates the
   * ones available in the model.
   * 
   * @return a {@link Map} with the configuration file names as {@code keys} and their associated {@link Document}s as
   *         {@code values}.
   */
  private Map<String, Document> getConfigs() {
    if (originalMuleDeployableModel.getConfigs() != null) {
      Map<String, Document> configs = new HashMap<>();
      originalMuleDeployableModel.getConfigs()
          .forEach(configFileName -> configs.put(configFileName, generateDocument(artifactLocation.getAbsoluteFile().toPath()
              .resolve(modelConfigsDirectory).resolve(configFileName))));
      return configs;
    } else {
      return getAvailableConfigs();
    }
  }

  /**
   * Resolves the configurations available in the model.
   * 
   * @return a {@link Map} with the configuration file names as {@code keys} and their associated {@link Document}s as
   *         {@code values}.
   */
  private Map<String, Document> getAvailableConfigs() {
    List<String> candidateConfigsFileNames =
        modelResources.stream().filter(resource -> resource.endsWith(CONFIG_FILE_EXTENSION)).collect(toList());

    Map<String, Document> availableCandidateConfigs = new HashMap<>();

    candidateConfigsFileNames.forEach(candidateConfigFileName -> availableCandidateConfigs
        .put(candidateConfigFileName, generateDocument(artifactLocation.getAbsoluteFile().toPath()
            .resolve(modelConfigsDirectory).resolve(candidateConfigFileName))));

    return availableCandidateConfigs.entrySet().stream().filter(entry -> hasMuleAsRootElement(entry.getValue()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Sets the builder with the default required {@link Product}.
   * 
   * @param availableConfigs configurations available in the project.
   */
  private void setBuilderWithDefaultRequiredProduct(Map<String, Document> availableConfigs) {
    Product requiredProduct = originalMuleDeployableModel.getRequiredProduct();
    if (requiredProduct == null) {
      requiredProduct = Product.MULE;
      if (doesSomeConfigRequireEE(availableConfigs)
          || anyMulePluginInDependenciesRequiresEE()
          || anyProvidedDependencyRequiresEE()) {
        requiredProduct = Product.MULE_EE;
      }
    }

    builder.setRequiredProduct(requiredProduct);
  }

  private boolean doesSomeConfigRequireEE(Map<String, Document> availableConfigs) {
    return availableConfigs.values().stream().filter(this::hasMuleAsRootElement).anyMatch(this::containsEENamespace);
  }

  private boolean hasMuleAsRootElement(Document doc) {
    if (doc != null && doc.getDocumentElement() != null) {
      String rootElementName = doc.getDocumentElement().getTagName();
      return StringUtils.equals(rootElementName, "mule") || StringUtils.equals(rootElementName, "domain:mule-domain");
    }
    return false;
  }

  private Document generateDocument(Path filePath) {
    javax.xml.parsers.DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();

    try {
      return factory.newDocumentBuilder().parse(filePath.toFile());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      return null;
    }
  }

  private boolean containsEENamespace(Document doc) {
    if (doc == null) {
      return false;
    }
    org.w3c.dom.Element root = doc.getDocumentElement();
    if (root.getNamespaceURI() != null && root.getNamespaceURI().contains(EE_NAMESPACE)) {
      return true;
    }
    if (root.getAttributes() != null) {
      NamedNodeMap attributes = root.getAttributes();
      for (int i = 0; i < attributes.getLength(); ++i) {
        Node uri = root.getAttributes().item(i);
        if (uri.getNodeValue() != null && uri.getNodeValue().contains(EE_NAMESPACE)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Creates a document builder factory.
   *
   * @return the factory created
   */
  private DocumentBuilderFactory createSecureDocumentBuilderFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      // Configuration based on
      // https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.md#xpathexpression

      // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all
      // XML entity attacks are prevented
      // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
      String feature = "http://apache.org/xml/features/disallow-doctype-decl";
      factory.setFeature(feature, true);

      // If you can't completely disable DTDs, then at least do the following:
      // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
      // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
      // JDK7+ - http://xml.org/sax/features/external-general-entities
      feature = "http://xml.org/sax/features/external-general-entities";
      factory.setFeature(feature, false);

      // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
      // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
      // JDK7+ - http://xml.org/sax/features/external-parameter-entities
      feature = "http://xml.org/sax/features/external-parameter-entities";
      factory.setFeature(feature, false);

      // Disable external DTDs as well
      feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
      factory.setFeature(feature, false);

      // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      return factory;
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);// should never happen
    }
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
