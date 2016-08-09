/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Arrays.stream;
import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.resources.AbstractResourcesGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

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
  private final File generatedResourcesDirectory;

  /**
   * Creates a {@link ExtensionsTestInfrastructureDiscoverer} that will use the extensionManager passed here in order to register
   * the extensions, resources for the extensions will be created in the generatedResourcesDirectory.
   *
   * @param extensionManagerAdapter {@link ExtensionManagerAdapter} to be used for registering the extensions
   * @param generatedResourcesDirectory the {@link File} where the resources for the extensions would be created
   * @throws {@link RuntimeException} if there was an error while creating the MANIFEST.MF file
   */
  public ExtensionsTestInfrastructureDiscoverer(ExtensionManagerAdapter extensionManagerAdapter,
                                                File generatedResourcesDirectory) {
    try {
      this.extensionManager = extensionManagerAdapter;
      this.generatedResourcesDirectory = generatedResourcesDirectory;
      createManifestFileIfNecessary(generatedResourcesDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Error creating discoverer", e);
    }
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
  public List<GeneratedResource> discoverExtensions(Describer[] describers, Class<?>[] annotatedClasses) {
    if (isEmpty(describers) && !isEmpty(annotatedClasses)) {
      describers = stream(annotatedClasses)
          .map(annotatedClass -> new AnnotationsBasedDescriber(annotatedClass, new StaticVersionResolver(getProductVersion())))
          .collect(Collectors.toList()).toArray(new Describer[annotatedClasses.length]);
    }
    if (isEmpty(describers)) {
      throw new IllegalStateException("No extension found");
    }
    loadExtensionsFromDescribers(extensionManager, describers);

    ExtensionsTestInfrastructureResourcesGenerator generator =
        new ExtensionsTestInfrastructureResourcesGenerator(getResourceFactories(), generatedResourcesDirectory);
    extensionManager.getExtensions().forEach(generator::generateFor);
    return generator.dumpAll();
  }

  private List<GeneratedResourceFactory> getResourceFactories() {
    return copyOf(serviceRegistry.lookupProviders(GeneratedResourceFactory.class));
  }

  private void loadExtensionsFromDescribers(ExtensionManagerAdapter extensionManager, Describer[] describers) {
    for (Describer describer : describers) {
      final DescribingContext context = new DefaultDescribingContext(getClass().getClassLoader());
      extensionManager.registerExtension(extensionFactory.createFrom(describer.describe(context), context));
    }
  }

  /**
   * Implementation of an {@link AbstractResourcesGenerator} that writes the generated resources to the specified target directory
   * but also exposes the content to be shared for testing purposes.
   */
  private static class ExtensionsTestInfrastructureResourcesGenerator extends AbstractResourcesGenerator {

    private final File targetDirectory;
    private final Map<String, StringBuilder> contents = new HashMap<>();

    private ExtensionsTestInfrastructureResourcesGenerator(Collection<GeneratedResourceFactory> resourceFactories,
                                                           File targetDirectory) {
      super(resourceFactories);
      this.targetDirectory = targetDirectory;
    }

    @Override
    protected void write(GeneratedResource resource) {
      StringBuilder builder = contents.get(resource.getPath());
      if (builder == null) {
        builder = new StringBuilder();
        contents.put(resource.getPath(), builder);
      }

      if (builder.length() > 0) {
        builder.append("\n");
      }

      builder.append(new String(resource.getContent()));
    }

    List<GeneratedResource> dumpAll() {
      List<GeneratedResource> allResources =
          contents.entrySet().stream().map(entry -> new GeneratedResource(entry.getKey(), entry.getValue().toString().getBytes()))
              .collect(new ImmutableListCollector<>());

      allResources.forEach(resource -> {
        File targetFile = new File(targetDirectory, resource.getPath());
        try {
          FileUtils.write(targetFile, new String(resource.getContent()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      return allResources;
    }
  }

  private File createManifestFileIfNecessary(File targetDirectory) throws IOException {
    return createManifestFileIfNecessary(targetDirectory, MuleManifest.getManifest());
  }

  private File createManifestFileIfNecessary(File targetDirectory, Manifest sourceManifest) throws IOException {
    File manifestFile = new File(targetDirectory.getPath(), "MANIFEST.MF");
    if (!manifestFile.exists()) {
      Manifest manifest = new Manifest(sourceManifest);
      try (FileOutputStream fileOutputStream = new FileOutputStream(manifestFile)) {
        manifest.write(fileOutputStream);
      }
    }
    return manifestFile;
  }

}
