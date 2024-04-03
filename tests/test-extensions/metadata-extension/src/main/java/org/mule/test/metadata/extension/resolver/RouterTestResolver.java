/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Map;
import java.util.function.Supplier;

public class RouterTestResolver implements OutputTypeResolver<String> {

  @Override
  public String getCategoryName() {
    return "router";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException {
    Map<String, Supplier<MessageMetadataType>> routes =
        context.getRouterOutputMetadataContext().get().getRouteOutputMessageTypes();
    if (!routes.containsKey("metaroute")) {
      throw new MetadataResolvingException("Invalid Chain output.", INVALID_CONFIGURATION);
    }
    return routes.get("metaroute").get().getPayloadType().get();
  }
}
