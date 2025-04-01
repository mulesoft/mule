/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;

import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of {@link ResourcesGenerator} that writes files using a {@link javax.annotation.processing.Filer} obtained
 * through a annotation {@link javax.annotation.processing.Processor} context
 *
 * @since 3.7.0
 */
public final class AnnotationProcessorResourceGenerator implements ResourcesGenerator {

  private final ProcessingEnvironment processingEnv;
  private final List<GeneratedResourceFactory> resourceFactories;

  /**
   * Creates a new instance
   *
   * @param resourceFactories the {@link GeneratedResourceFactory} instances used to generated resources
   * @param processingEnv     the current {@link ProcessingEnvironment}
   */
  public AnnotationProcessorResourceGenerator(List<GeneratedResourceFactory> resourceFactories,
                                              ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.resourceFactories = ImmutableList.copyOf(enrichFactories(resourceFactories, processingEnv));
  }

  protected void write(GeneratedResource resource) {
    FileObject file;
    try {
      file = processingEnv.getFiler().createResource(resource.isAvailableInArtifact() ? CLASS_OUTPUT : SOURCE_OUTPUT,
                                                     EMPTY, resource.getPath());
    } catch (IOException e) {
      throw wrapException(e, resource);
    }

    try (OutputStream out = file.openOutputStream()) {
      out.write(resource.getContent());
      out.flush();
    } catch (IOException e) {
      throw wrapException(e, resource);
    }
  }

  private RuntimeException wrapException(Exception e, GeneratedResource resource) {
    return new RuntimeException(String.format("Could not write generated resource '%s'", resource.getPath()), e);
  }

  private static Collection<GeneratedResourceFactory> enrichFactories(List<GeneratedResourceFactory> resourceFactories,
                                                                      ProcessingEnvironment processingEnv) {
    resourceFactories.forEach(factory -> {
      if (factory instanceof ProcessingEnvironmentAware peaFacotry) {
        peaFacotry.setProcessingEnvironment(processingEnv);
      }
    });

    return resourceFactories;
  }

  @Override
  public List<GeneratedResource> generateFor(ExtensionModel extensionModel) {
    List<GeneratedResource> resources = resourceFactories.stream().map(factory -> factory.generateResource(extensionModel))
        .filter(Optional::isPresent).map(Optional::get).collect(toImmutableList());

    resources.forEach(this::write);
    return resources;
  }
}
