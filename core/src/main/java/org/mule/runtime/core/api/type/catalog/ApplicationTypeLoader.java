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

/**
 * a {@link TypeLoader} for obtaining types available in the context of the current application. It accepts primitive type names
 * (such as string, number, etc.) and can also access types defined in other extensions by using the
 * {@code <extension_namespace>:<type_name>} syntax.
 *
 * @since 4.5.0
 */
public class ApplicationTypeLoader implements TypeLoader {

  private TypeLoader primitivesTypeLoader = new PrimitiveTypesTypeLoader();

  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    // TODO: MULE-20071 - implement app type catalog lookup.
    return primitivesTypeLoader.load(typeIdentifier);
  }
}
