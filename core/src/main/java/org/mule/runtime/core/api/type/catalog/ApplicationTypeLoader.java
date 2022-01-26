/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;

import java.util.Optional;

public class ApplicationTypeLoader implements TypeLoader {

  private TypeLoader primitivesTypeLoader = new PrimitiveTypesTypeLoader();

  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    // TODO: implement app type catalog lookup. Should it include extension exported types?
    return primitivesTypeLoader.load(typeIdentifier);
  }
}
