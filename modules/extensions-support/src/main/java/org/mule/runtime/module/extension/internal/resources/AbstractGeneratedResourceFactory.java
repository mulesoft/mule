/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import java.util.List;
import java.util.Optional;

public abstract class AbstractGeneratedResourceFactory implements GeneratedResourceFactory {

  @Override
  public final Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    List<GeneratedResource> resources = generateResources(extensionModel);
    return resources.isEmpty() ? empty() : of(resources.get(0));
  }
}
