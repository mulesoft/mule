/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.infrastructure;

import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.resources.AbstractResourcesGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of an {@link AbstractResourcesGenerator} that writes the generated resources to the specified target directory
 * but also exposes the content to be shared for testing purposes.
 */
class ExtensionsTestLoaderResourcesGenerator extends AbstractResourcesGenerator {

  private final File targetDirectory;
  private final Map<String, StringBuilder> contents = new HashMap<>();

  ExtensionsTestLoaderResourcesGenerator(Collection<GeneratedResourceFactory> resourceFactories,
                                         File targetDirectory) {
    super(resourceFactories);
    this.targetDirectory = targetDirectory;
  }

  @Override
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
        contents.entrySet().stream().map(entry -> new GeneratedResource(entry.getKey(), entry.getValue().toString().getBytes()))
            .collect(toImmutableList());

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
}
