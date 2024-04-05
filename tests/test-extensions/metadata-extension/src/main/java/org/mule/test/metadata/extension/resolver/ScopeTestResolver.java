/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class ScopeTestResolver implements OutputTypeResolver<String> {

  @Override
  public String getCategoryName() {
    return "scope";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException {
    return context.getScopeOutputMetadataContext().map(ctx -> ctx.getInnerChainOutputMessageType())
        .orElseThrow(() -> new MetadataResolvingException("Invalid Chain output.", INVALID_CONFIGURATION))
        .get()
        .getPayloadType().get();
  }
}
