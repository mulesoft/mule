/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.infrastructure;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslResolvingContext;
import org.mule.runtime.extension.xml.dsl.api.resources.spi.DslResourceFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.introspection.version.VersionResolver;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.jar.Manifest;

/**
 * Discovers and registers the extensions to a {@link org.mule.runtime.extension.api.ExtensionManager}.
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
  private final ExtensionFactory extensionFactory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
  private final ExtensionManagerAdapter extensionManager;

  /**
   * Creates a {@link ExtensionsTestInfrastructureDiscoverer} that will use the extensionManager passed here in order to register
   * the extensions, resources for the extensions will be created in the generatedResourcesDirectory.
   *
   * @param extensionManagerAdapter {@link ExtensionManagerAdapter} to be used for registering the extensions
   * @throws {@link RuntimeException} if there was an error while creating the MANIFEST.MF file
   */
  public ExtensionsTestInfrastructureDiscoverer(ExtensionManagerAdapter extensionManagerAdapter) {
    this.extensionManager = extensionManagerAdapter;
  }

  /**
   * It will register the extensions described or annotated and it will generate their resources. If no describers are defined the
   * annotatedClasses would be used to generate the describers.
   *
   * @param describers if empty it will use annotatedClasses param to build the describers
   * @param annotatedClasses used to build the describers
   * @throws IllegalStateException if no extensions can be described
   * @return a {@link List} of the resources generated for the given describers or annotated classes
   */
  public void discoverExtensions(Describer[] describers, Class<?>[] annotatedClasses) {
    if (isEmpty(describers) && !isEmpty(annotatedClasses)) {
      describers = stream(annotatedClasses)
          .map(annotatedClass -> new AnnotationsBasedDescriber(annotatedClass, new StaticVersionResolver(getProductVersion())))
          .collect(toList()).toArray(new Describer[annotatedClasses.length]);
    }
    if (isEmpty(describers)) {
      throw new IllegalStateException("No extension found");
    }
    loadExtensionsFromDescribers(extensionManager, describers);
  }

  /**
   * It will register the extensions described or annotated and it will generate their resources. If no describers are defined the
   * annotatedClasses would be used to generate the describers.
   *
   * @throws IllegalStateException if no extensions can be described
   * @return a {@link List} of the resources generated for the given describers or annotated classes
   */
  public RuntimeExtensionModel discoverExtension(Class<?> annotatedClass, VersionResolver versionResolver) {
    RuntimeExtensionModel model =
        loadExtensionModel(new AnnotationsBasedDescriber(annotatedClass, versionResolver));
    extensionManager.registerExtension(model);

    return model;
  }

  public List<GeneratedResource> generateLoaderResources(RuntimeExtensionModel extensionModel,
                                                         File generatedResourcesDirectory) {
    createManifestFileIfNecessary(generatedResourcesDirectory);
    ExtensionsTestLoaderResourcesGenerator generator =
        new ExtensionsTestLoaderResourcesGenerator(getResourceFactories(), generatedResourcesDirectory);

    generator.generateFor(extensionModel);
    return generator.dumpAll();
  }

  public List<GeneratedResource> generateDslResources(File generatedResourcesDirectory) {
    DslResolvingContext context =
        extensionManager.getExtensions().stream().anyMatch(e -> e.getModelProperty(ImportedTypesModelProperty.class).isPresent())
            ? name -> extensionManager.getExtension(name).map(e -> e)
            : name -> Optional.empty();

    ExtensionsTestDslResourcesGenerator dslResourceGenerator =
        new ExtensionsTestDslResourcesGenerator(getDslResourceFactories(), generatedResourcesDirectory, context);

    extensionManager.getExtensions().forEach(dslResourceGenerator::generateFor);

    return dslResourceGenerator.dumpAll();
  }

  private List<GeneratedResourceFactory> getResourceFactories() {
    return copyOf(serviceRegistry.lookupProviders(GeneratedResourceFactory.class));
  }

  private List<DslResourceFactory> getDslResourceFactories() {
    return copyOf(serviceRegistry.lookupProviders(DslResourceFactory.class));
  }

  private void loadExtensionsFromDescribers(ExtensionManagerAdapter extensionManager, Describer[] describers) {
    for (Describer describer : describers) {
      extensionManager.registerExtension(loadExtensionModel(describer));
    }
  }

  private RuntimeExtensionModel loadExtensionModel(Describer describer) {
    final DescribingContext context = new DefaultDescribingContext(getClass().getClassLoader());
    return extensionFactory.createFrom(describer.describe(context), context);
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
