/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;


import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;

/**
 * Implementation of {@link ResourcesGenerator} that writes files using a {@link javax.annotation.processing.Filer} obtained
 * through a annotation {@link javax.annotation.processing.Processor} context
 *
 * @since 3.7.0
 */
public final class AnnotationProcessorResourceGenerator extends AbstractResourcesGenerator {

  private final ProcessingEnvironment processingEnv;

  /**
   * Creates a new instance
   *
   * @param resourceFactories the {@link GeneratedResourceFactory} instances used to generated resources
   * @param processingEnv the current {@link ProcessingEnvironment}
   */
  public AnnotationProcessorResourceGenerator(List<GeneratedResourceFactory> resourceFactories,
                                              ProcessingEnvironment processingEnv) {
    super(resourceFactories);
    this.processingEnv = processingEnv;
  }

  @Override
  protected void write(GeneratedResource resource) {
    FileObject file;
    try {
      file = processingEnv.getFiler().createResource(SOURCE_OUTPUT, EMPTY, resource.getPath());
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
}
