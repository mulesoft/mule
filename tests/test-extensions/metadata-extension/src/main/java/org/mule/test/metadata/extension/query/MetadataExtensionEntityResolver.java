/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.query;

import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.test.metadata.extension.model.shapes.Circle;
import org.mule.test.metadata.extension.model.shapes.Square;

import java.util.Set;
import java.util.stream.Stream;

public class MetadataExtensionEntityResolver implements QueryEntityResolver {

  private static final ClassTypeLoader LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  public static final String CIRCLE = "Circle";
  public static final String SQUARE = "Square";

  @Override
  public Set<MetadataKey> getEntityKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return Stream.of(newKey(SQUARE).build(), newKey(CIRCLE).build()).collect(toSet());
  }

  @Override
  public MetadataType getEntityMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    switch (key) {
      case CIRCLE:
        return LOADER.load(Circle.class);
      case SQUARE:
        return LOADER.load(Square.class);
      default:
        throw new MetadataResolvingException("Invalid metadata key: " + key, INVALID_METADATA_KEY);
    }
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionEntityResolver";
  }
}
