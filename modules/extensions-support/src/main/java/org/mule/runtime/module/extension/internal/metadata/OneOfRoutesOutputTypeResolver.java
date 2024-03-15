/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * {@link OutputTypeResolver} implementation for Routers.
 * <p>
 * This {@link OutputTypeResolver} propagates the metadata the union of the output of every route of the router. This symbolizes
 * that the metadata can be either of the results of any of the routes.
 *
 * @since 4.7
 */
public class OneOfRoutesOutputTypeResolver implements OutputTypeResolver<Void> {

  @Override
  public String getCategoryName() {
    return "OUTPUT_ROUTER_DYNAMIC";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Void key) throws MetadataResolvingException, ConnectionException {
    UnionTypeBuilder builder = context.getTypeBuilder().unionType();
    context.getInnerRoutesOutputType().values().forEach(metadataSupplier -> builder.of(metadataSupplier.get()));
    return builder.build();
  }
}
