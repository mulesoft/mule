/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.infrastructure;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.VERSION;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
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
import java.util.List;
import java.util.Map;
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

  private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
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
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, annotatedClass.getName());
    params.put(VERSION, getProductVersion());
    DslResolvingContext dslResolvingContext = getDefault(emptySet());
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
      String schema = schemaGenerator.generate(model, DslResolvingContext.getDefault(extensionManager.getExtensions()));
      File xsd = FileUtils.newFile(generatedResourcesDirectory, xsdFileName);
      FileUtils.copyStreamToFile(new ByteArrayInputStream(schema.getBytes()), xsd);
    } catch (IOException e) {
      throw new RuntimeException(format("Error generating test xsd resource [%s]: " + e.getMessage(), xsdFileName, e));
    }
  }

  public List<GeneratedResource> generateDslResources(File generatedResourcesDirectory, ExtensionModel forExtensionModel) {
    DslResolvingContext context =
        extensionManager.getExtensions().stream().anyMatch(e -> !e.getImportedTypes().isEmpty())
            ? DslResolvingContext.getDefault(extensionManager.getExtensions())
            : new NullDslResolvingContext();

    ExtensionsTestDslResourcesGenerator dslResourceGenerator =
        new ExtensionsTestDslResourcesGenerator(getDslResourceFactories(), generatedResourcesDirectory, context);

    extensionManager.getExtensions().stream()
        .filter(runtimeExtensionModel -> forExtensionModel != null ? runtimeExtensionModel.equals(forExtensionModel) : true)
        .forEach(dslResourceGenerator::generateFor);

    return dslResourceGenerator.dumpAll();
  }

  private List<GeneratedResourceFactory> getResourceFactories() {
    return copyOf(serviceRegistry.lookupProviders(GeneratedResourceFactory.class, currentThread().getContextClassLoader()));
  }

  private ExtensionSchemaGenerator getSchemaGenerator() {
    return copyOf(serviceRegistry.lookupProviders(ExtensionSchemaGenerator.class, currentThread().getContextClassLoader()))
        .get(0);
  }

  private List<DslResourceFactory> getDslResourceFactories() {
    return copyOf(serviceRegistry.lookupProviders(DslResourceFactory.class, currentThread().getContextClassLoader()));
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
