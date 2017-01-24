/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.model.resolver;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.json.JsonTypeLoader;

import java.net.URL;
import java.util.Optional;

/**
 * Represents a single type (commonly used in JSON schemas).
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 *
 * @since 4.0
 */
public class SingleTypeResolver implements TypeResolver {

  private String typeIdentifier;
  private final TypeLoader typeLoader;

  public SingleTypeResolver(String typeIdentifier, URL schemaUrl) {
    Preconditions.checkNotNull(typeIdentifier);
    Preconditions.checkNotNull(schemaUrl);
    typeLoader = new JsonTypeLoader(FileUtils.toFile(schemaUrl));
    this.typeIdentifier = typeIdentifier;
  }

  @Override
  public Optional<MetadataType> resolveType(String typeIdentifier) {
    return this.typeIdentifier.equals(typeIdentifier) ? typeLoader.load(typeIdentifier) : Optional.empty();
  }
}
