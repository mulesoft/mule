/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.infrastructure;

import static org.mule.runtime.core.api.util.FileUtils.stringToFile;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of an {@link ResourcesGenerator} that writes the generated resources to the specified target directory but also
 * exposes the content to be shared for testing purposes.
 */
class ExtensionsTestLoaderResourcesGenerator implements ResourcesGenerator {

  private final File targetDirectory;
  private final Map<String, StringBuilder> contents = new HashMap<>();
  private final List<GeneratedResourceFactory> resourceFactories;

  ExtensionsTestLoaderResourcesGenerator(Collection<GeneratedResourceFactory> resourceFactories,
                                         File targetDirectory) {
    this.targetDirectory = targetDirectory;
    this.resourceFactories = ImmutableList.copyOf(resourceFactories);
  }

  protected void write(GeneratedResource resource) {
    String resourceKey = Paths.get(targetDirectory.getPath(), resource.getPath()).toString();
    StringBuilder builder = contents.get(resourceKey);
    if (builder == null) {
      builder = new StringBuilder();
      contents.put(resourceKey, builder);
    }

    if (builder.length() > 0) {
      builder.append("\n");
    }

    builder.append(new String(resource.getContent()));
  }

  List<GeneratedResource> dumpAll() {
    List<GeneratedResource> allResources =
        contents.entrySet().stream()
            .map(entry -> new GeneratedResource(false, entry.getKey(), entry.getValue().toString().getBytes()))
            .toList();

    allResources.forEach(resource -> {
      File targetFile = new File(resource.getPath());
      try {
        stringToFile(targetFile.getAbsolutePath(), new String(resource.getContent()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    return allResources;
  }

  @Override
  public List<GeneratedResource> generateFor(ExtensionModel extensionModel) {
    List<GeneratedResource> resources = resourceFactories.stream()
        .map(factory -> factory.generateResource(extensionModel))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    resources.forEach(this::write);
    return resources;
  }
}
