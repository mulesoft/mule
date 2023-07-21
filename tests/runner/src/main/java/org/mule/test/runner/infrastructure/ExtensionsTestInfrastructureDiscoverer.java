/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.infrastructure;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.extension.internal.spi.ExtensionsApiSpiUtils.loadDslResourceFactories;
import static org.mule.runtime.extension.internal.spi.ExtensionsApiSpiUtils.loadExtensionSchemaGenerators;
import static org.mule.runtime.extension.internal.spi.ExtensionsApiSpiUtils.loadGeneratedResourceFactories;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.DslResourceFactory;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Discovers and registers the extensions to a {@link org.mule.runtime.core.api.extension.ExtensionManager}.
 * <p/>
 * Once extensions are registered, a {@link ResourcesGenerator} is used to automatically generate any backing resources needed
 * (XSD schemas, spring bundles, etc).
 * <p/>
 * In this way, the user experience is greatly simplified when running the test either through an IDE or build tool such as maven
 * or gradle.
 * <p/>
 *
 * @since 4.0
 */
public class ExtensionsTestInfrastructureDiscoverer {

  private final ExtensionManager extensionManager;

  /**
   * Creates a {@link ExtensionsTestInfrastructureDiscoverer} that will use the extensionManager passed here in order to register
   * the extensions, resources for the extensions will be created in the generatedResourcesDirectory.
   *
   * @param extensionManagerAdapter {@link ExtensionManager} to be used for registering the extensions
   * @throws {@link RuntimeException} if there was an error while creating the MANIFEST.MF file
   */
  public ExtensionsTestInfrastructureDiscoverer(ExtensionManager extensionManagerAdapter) {
    this.extensionManager = extensionManagerAdapter;
  }


  /**
   * It will register the extensions described or annotated and it will generate their resources. If no describers are defined the
   * annotatedClasses would be used to generate the describers.
   *
   * @return a {@link List} of the resources generated for the given describers or annotated classes
   * @throws IllegalStateException if no extensions can be described
   */
  public ExtensionModel discoverExtension(Class<?> annotatedClass, ExtensionModelLoader loader) {
    return discoverExtension(annotatedClass, loader, getDefault(singleton(MuleExtensionModelProvider.getExtensionModel())));
  }

  public ExtensionModel discoverExtension(Class<?> annotatedClass, ExtensionModelLoader loader,
                                          DslResolvingContext dslResolvingContext) {
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, annotatedClass.getName());
    params.put(VERSION, getProductVersion());
    ExtensionModel model = loader.loadExtensionModel(annotatedClass.getClassLoader(), dslResolvingContext, params);
    extensionManager.registerExtension(model);
    return model;
  }

  public List<GeneratedResource> generateLoaderResources(ExtensionModel extensionModel, File generatedResourcesDirectory) {
    createManifestFileIfNecessary(generatedResourcesDirectory);
    ExtensionsTestLoaderResourcesGenerator generator =
        new ExtensionsTestLoaderResourcesGenerator(getResourceFactories(), generatedResourcesDirectory);
    generator.generateFor(extensionModel);
    return generator.dumpAll();
  }

  public List<GeneratedResource> generateDslResources(File generatedResourcesDirectory) {
    return generateDslResources(generatedResourcesDirectory, null);
  }

  public void generateSchemaTestResource(ExtensionModel model, File generatedResourcesDirectory) {
    String xsdFileName = model.getXmlDslModel().getXsdFileName();
    try {
      ExtensionSchemaGenerator schemaGenerator = getSchemaGenerator();
      Set<ExtensionModel> models = new HashSet<>(extensionManager.getExtensions());
      models.add(MuleExtensionModelProvider.getExtensionModel());
      String schema = schemaGenerator.generate(model, DslResolvingContext.getDefault(models));
      File xsd = FileUtils.newFile(generatedResourcesDirectory, xsdFileName);
      FileUtils.copyStreamToFile(new ByteArrayInputStream(schema.getBytes()), xsd);
    } catch (IOException e) {
      throw new RuntimeException(format("Error generating test xsd resource [%s]: " + e.getMessage(), xsdFileName, e));
    }
  }

  public List<GeneratedResource> generateDslResources(File generatedResourcesDirectory, ExtensionModel forExtensionModel) {
    DslResolvingContext context;
    if (extensionManager.getExtensions().stream().anyMatch(e -> !e.getImportedTypes().isEmpty())) {
      HashSet<ExtensionModel> models = new HashSet<>(extensionManager.getExtensions());
      models.add(MuleExtensionModelProvider.getExtensionModel());
      context = DslResolvingContext.getDefault(models);
    } else {
      context = new NullDslResolvingContext();
    }

    ExtensionsTestDslResourcesGenerator dslResourceGenerator =
        new ExtensionsTestDslResourcesGenerator(getDslResourceFactories(), generatedResourcesDirectory, context);

    extensionManager.getExtensions().stream()
        .filter(runtimeExtensionModel -> forExtensionModel != null ? runtimeExtensionModel.equals(forExtensionModel) : true)
        .forEach(dslResourceGenerator::generateFor);

    return dslResourceGenerator.dumpAll();
  }

  private List<GeneratedResourceFactory> getResourceFactories() {
    return loadGeneratedResourceFactories().collect(toList());
  }

  private ExtensionSchemaGenerator getSchemaGenerator() {
    return loadExtensionSchemaGenerators().findFirst().get();
  }

  private List<DslResourceFactory> getDslResourceFactories() {
    return loadDslResourceFactories().collect(toList());
  }

  private File createManifestFileIfNecessary(File targetDirectory) {
    return createManifestFileIfNecessary(targetDirectory, MuleManifest.getManifest());
  }

  private File createManifestFileIfNecessary(File targetDirectory, Manifest sourceManifest) {
    try {
      File manifestFile = new File(targetDirectory.getPath(), "MANIFEST.MF");
      if (!manifestFile.exists()) {
        Manifest manifest = new Manifest(sourceManifest);
        try (FileOutputStream fileOutputStream = new FileOutputStream(manifestFile)) {
          manifest.write(fileOutputStream);
        }
      }
      return manifestFile;
    } catch (IOException e) {
      throw new RuntimeException("Error creating discoverer", e);
    }
  }

}
