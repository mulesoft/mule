/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;

import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Map;

/**
 * {@link OutputTypeResolver} implementation for Routers.
 * <p>
 * This {@link OutputTypeResolver} propagates the metadata the combination of the output of every route in the router. This
 * symbolizes that the metadata has the result of all the routes.
 *
 * @since 1.7
 */
public class AllOfRoutesOutputTypeResolver implements OutputTypeResolver<Void> {

  @Override
  public String getCategoryName() {
    return "OUTPUT_ROUTER_DYNAMIC";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Void key) throws MetadataResolvingException, ConnectionException {
    ObjectTypeBuilder builder = context.getTypeBuilder().objectType();
    Map<String, ScopeOutputMetadataContext> routes = context.getRouterOutputMetadataContext()
        .map(route -> route.getRoutesPropagationContext())
        .orElseThrow(() -> new MetadataResolvingException("Route propagation context not available", UNKNOWN));

    routes.forEach((routeName, route) -> {
      builder.addField().key(routeName).value(route.getChainOutputResolver().get());
    });

    return builder.build();
  }
}
