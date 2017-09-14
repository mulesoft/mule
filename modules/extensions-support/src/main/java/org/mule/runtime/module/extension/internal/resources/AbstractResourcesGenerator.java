/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * Base implementation of {@link ResourcesGenerator} that takes care of the basic contract except for actually writing the
 * resources to a persistent store. Implementations are only required to provide that piece of logic by using the
 * {@link #write(GeneratedResource)} template method
 *
 * @since 3.7.0
 */
public abstract class AbstractResourcesGenerator implements ResourcesGenerator {

  private final List<GeneratedResourceFactory> resourceFactories;

  public AbstractResourcesGenerator(Collection<GeneratedResourceFactory> resourceFactories) {
    this.resourceFactories = ImmutableList.copyOf(resourceFactories);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<GeneratedResource> generateFor(ExtensionModel extensionModel) {
    List<GeneratedResource> resources = resourceFactories.stream().map(factory -> factory.generateResource(extensionModel))
        .filter(Optional::isPresent).map(Optional::get).collect(toImmutableList());

    resources.forEach(this::write);
    return resources;
  }

  /**
   * Template method to actually write the given {@code resource} to a persistent store
   *
   * @param resource a non null {@link GeneratedResource}
   */
  protected abstract void write(GeneratedResource resource);
}
