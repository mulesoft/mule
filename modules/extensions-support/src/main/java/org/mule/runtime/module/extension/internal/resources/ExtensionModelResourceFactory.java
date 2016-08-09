/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MODEL_JSON_FILE_NAME;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.ExtensionProperties;

import java.util.Optional;

/**
 * Generates a resource of name {@link ExtensionProperties#EXTENSION_MODEL_JSON_FILE_NAME} which contains a json representation of
 * the {@link ExtensionModel}
 *
 * @since 4.0
 */
public class ExtensionModelResourceFactory implements GeneratedResourceFactory {

  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    String json = new ExtensionModelJsonSerializer(true).serialize(extensionModel);
    return Optional.of(new GeneratedResource(EXTENSION_MODEL_JSON_FILE_NAME, json.getBytes()));
  }
}
