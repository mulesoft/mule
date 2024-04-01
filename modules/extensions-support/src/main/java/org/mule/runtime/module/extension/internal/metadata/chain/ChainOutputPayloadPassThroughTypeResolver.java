/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.chain;

import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Pass Through {@link OutputTypeResolver} implementation for Scopes.
 * <p>
 * Propagates the inner chain's resolved payload type.
 *
 * @since 4.7
 */
public class ChainOutputPayloadPassThroughTypeResolver implements OutputTypeResolver<Void> {

  @Override
  public String getCategoryName() {
    return "SCOPE_PASSTHROUGH_OUTPUT_PAYLOAD";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Void key) throws MetadataResolvingException, ConnectionException {
    return context.getScopeOutputMetadataContext()
        .map(ctx -> ctx.getChainOutputResolver())
        .orElseThrow(() -> new MetadataResolvingException("Chain Output Context not found.", UNKNOWN))
        .get()
        .getPayloadType()
        .orElseThrow(() -> new MetadataResolvingException("Resolved output payload not found.", UNKNOWN));
  }
}
