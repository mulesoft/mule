/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Optional.of;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import java.util.Optional;

public class TemporalExtensionModelResourceGenerator implements GeneratedResourceFactory {

  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    final String serializedExtensionModel = new ExtensionModelJsonSerializer(true).serialize(extensionModel);
    return of(new GeneratedResource("temporal-extension-model.json", serializedExtensionModel.getBytes()));
  }
}
