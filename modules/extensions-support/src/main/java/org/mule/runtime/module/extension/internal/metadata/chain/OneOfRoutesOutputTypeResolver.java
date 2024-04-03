/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.chain;

import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.VOID_TYPE;

import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link OutputTypeResolver} implementation for Routers.
 * <p>
 * This {@link OutputTypeResolver} propagates the metadata the union of every route's output. This symbolizes
 * that the result will be the output of (any) one of the routes.
 *
 * @since 4.7
 */
public class OneOfRoutesOutputTypeResolver implements OutputTypeResolver<Void>, AttributesTypeResolver<Void> {

  public static final OneOfRoutesOutputTypeResolver INSTANCE = new OneOfRoutesOutputTypeResolver();

  private OneOfRoutesOutputTypeResolver() {}

  @Override
  public String getCategoryName() {
    return "OUTPUT_ROUTER_DYNAMIC";
  }

  @Override
  public String getResolverName() {
    return "ONE_OF_ROUTES";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Void key) throws MetadataResolvingException, ConnectionException {
    return union(context, MessageMetadataType::getPayloadType);
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, Void key)
      throws MetadataResolvingException, ConnectionException {
    return union(context, MessageMetadataType::getAttributesType);
  }

  private MetadataType union(MetadataContext context, Function<MessageMetadataType, Optional<MetadataType>> extractor)
      throws MetadataResolvingException {
    UnionTypeBuilder builder = context.getTypeBuilder().unionType();
    Map<String, Supplier<MessageMetadataType>> routes = context.getRouterOutputMetadataContext()
        .map(ctx -> ctx.getRouteOutputMessageTypes())
        .orElseThrow(() -> new MetadataResolvingException("Route propagation context not available", UNKNOWN));

    routes.values().forEach(route -> {
      MetadataType type = extractor.apply(route.get()).orElse(VOID_TYPE);
      builder.of(type);
    });
    return builder.build();
  }
}
